package com.example.data.remote

import com.example.data.model.DeliveryItem
import com.example.data.model.DeliveryOrder
import com.example.data.model.DriverContact
import com.example.data.model.OfflineDeliveryConfirmation
import com.example.data.model.OrderDeliveryStatus
import com.example.data.model.OrderDisplayCode
import com.example.data.model.PaymentSummary
import com.example.ItaSuperApplication
import com.example.data.local.OfflineDeliveryQueue
import com.example.data.model.LinkedStore
import com.example.platform.AddressGeocodingCache
import com.example.platform.DriverTrackingService
import com.example.platform.DriverNotificationHelper
import com.example.platform.NetworkMonitor
import com.example.data.repository.DriverAvailabilityRepository
import com.example.data.repository.DriverOrdersRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Fonte de pedidos do motoboy de loja. O servidor e as políticas RLS definem
 * quais pedidos podem ser visualizados; o aplicativo só organiza os dados nas
 * listas de disponibilidade e rota ativa.
 */
class SupabaseDriverOrdersRepository(
    private val availabilityRepository: DriverAvailabilityRepository
) : DriverOrdersRepository {
    private val supabase = ItaSuperSupabase.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val availableOrders = MutableStateFlow<List<DeliveryOrder>>(emptyList())
    private val activeRouteOrders = MutableStateFlow<List<DeliveryOrder>>(emptyList())
    private val todayCompletedCount = MutableStateFlow(0)
    private val offlineQueue by lazy { OfflineDeliveryQueue(ItaSuperApplication.appContext) }
    private var channel: RealtimeChannel? = null
    private val realtimeJobs = mutableListOf<Job>()
    private var observedStoreIds: Set<String> = emptySet()

    init {
        // A primeira sincronização não pode derrubar o aplicativo se rede, RLS
        // ou schema estiverem temporariamente indisponíveis.
        scope.launch { runCatching { refreshAll() } }
    }

    override fun getAvailableOrders(): Flow<List<DeliveryOrder>> = availableOrders.asStateFlow()
    override fun getActiveRouteOrders(): Flow<List<DeliveryOrder>> = activeRouteOrders.asStateFlow()
    override fun getTodayCompletedCount(): Flow<Int> = todayCompletedCount.asStateFlow()
    override fun getOfflineConfirmations(): Flow<List<OfflineDeliveryConfirmation>> = offlineQueue.observe()

    override suspend fun acceptOrder(orderId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc("driver_accept_order", buildJsonObject {
            put("_order_id", orderId)
        })
        refreshAll()
    }

    override suspend fun acceptAllOrders(orderIds: List<String>): Result<Unit> = runCatching {
        require(orderIds.isNotEmpty()) { "Nenhum pedido disponível para aceitar." }
        supabase.postgrest.rpc("driver_accept_orders_batch", buildJsonObject {
            put("_order_ids", buildJsonArray {
                orderIds.distinct().forEach { add(JsonPrimitive(it)) }
            })
        })
        refreshAll()
    }

    override suspend fun dispatchOrder(orderId: String) {
        val order = activeRouteOrders.value.firstOrNull { it.id == orderId }
            ?: error("Pedido não encontrado na rota ativa.")
        supabase.postgrest.rpc("driver_depart_route", buildJsonObject {
            put("_order_ids", buildJsonArray { add(JsonPrimitive(orderId)) })
        })
        DriverTrackingService.start(ItaSuperApplication.appContext, order)
        refreshAll()
    }

    override suspend fun dispatchAllReadyOrders() {
        val readyOrders = activeRouteOrders.value
            .filter { it.status == OrderDeliveryStatus.PRONTO_PARA_ENTREGA }
        require(readyOrders.isNotEmpty()) { "Não há pedidos prontos na rota para iniciar." }
        supabase.postgrest.rpc("driver_depart_route", buildJsonObject {
            put("_order_ids", buildJsonArray {
                readyOrders.forEach { add(JsonPrimitive(it.id)) }
            })
        })
        // A primeira parada da rota iniciada é monitorada; as próximas serão configuradas ao avançar a rota.
        DriverTrackingService.start(ItaSuperApplication.appContext, readyOrders.first())
        refreshAll()
    }

    override suspend fun completeDelivery(orderId: String, pin: String): Result<Unit> = runCatching {
        val normalizedPin = pin.trim()
        require(normalizedPin.length == 4) { "Informe o PIN de 4 dígitos fornecido pelo cliente." }
        val activeOrder = activeRouteOrders.value.find { it.id == orderId }
            ?: error("Pedido não encontrado na rota ativa.")

        if (!NetworkMonitor.isOnline(ItaSuperApplication.appContext)) {
            if (activeOrder.customerPin.isNotBlank() && activeOrder.customerPin != normalizedPin) {
                error("PIN incorreto. Verifique o código com o cliente.")
            }
            offlineQueue.enqueue(
                orderId = orderId,
                shortCode = activeOrder.shortCode,
                pin = normalizedPin,
                deviceId = android.provider.Settings.Secure.getString(
                    ItaSuperApplication.appContext.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ).orEmpty()
            )
            activeRouteOrders.value = activeRouteOrders.value.filterNot { it.id == orderId }
            if (activeRouteOrders.value.isEmpty()) {
                DriverTrackingService.stop(ItaSuperApplication.appContext)
            }
            availabilityRepository.setRouteActive(activeRouteOrders.value.isNotEmpty())
            return@runCatching
        }

        supabase.postgrest.rpc("driver_finish_delivery", buildJsonObject {
            put("_order_id", orderId)
            put("_pin", normalizedPin)
        })
        refreshAll()
        val nextInTransitOrder = activeRouteOrders.value.firstOrNull {
            it.status == OrderDeliveryStatus.SAIU_ENTREGA || it.status == OrderDeliveryStatus.EM_TRANSITO
        }
        if (nextInTransitOrder == null) {
            DriverTrackingService.stop(ItaSuperApplication.appContext)
        } else {
            DriverTrackingService.start(ItaSuperApplication.appContext, nextInTransitOrder)
        }
    }

    override suspend fun clearOfflineConfirmations() {
        offlineQueue.clear()
    }

    override suspend fun refreshOrders() {
        refreshAll()
    }

    suspend fun refreshAll() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: run {
            clearState()
            return
        }
        val storeIds = fetchAcceptedStoreIds(userId)
        if (storeIds.isEmpty()) {
            clearState()
            return
        }

        val rows = supabase
            .from("orders")
            .select(columns = Columns.list(
                "id", "client_id", "store_id", "status", "subtotal", "delivery_fee", "total_price",
                "payment_method", "neighborhood", "address_details", "delivery_cep", "delivery_city", "delivery_state",
                "created_at", "driver_id", "delivery_pin", "confirmed_at", "needs_change", "change_for", "client_lat", "client_lng",
                "assigned_driver_id", "order_number"
            )) {
                filter { isIn("store_id", storeIds) }
            }
            .decodeList<OrderRow>()
            .sortedBy { it.createdAt.orEmpty() }

        val activeStatuses = setOf("pronto_para_entrega", "saiu_entrega", "em_transito")
        val rowsToDisplay = rows.filter { row ->
            (row.status.equals("pronto_para_entrega", ignoreCase = true) &&
                row.driverId == null &&
                (row.assignedDriverId == null || row.assignedDriverId == userId)) ||
                (row.driverId == userId && row.status.lowercase() in activeStatuses)
        }
        val mappedOrders = mapOrders(rowsToDisplay)
        val rowsById = rowsToDisplay.associateBy { it.id }
        val available = mappedOrders.filter { order ->
            val row = rowsById[order.id] ?: return@filter false
            row.status.equals("pronto_para_entrega", ignoreCase = true) &&
                row.driverId == null &&
                (row.assignedDriverId == null || row.assignedDriverId == userId)
        }
        val active = mappedOrders.filter { order ->
            val row = rowsById[order.id] ?: return@filter false
            row.driverId == userId && row.status.lowercase() in activeStatuses
        }.mapIndexed { index, order -> order.copy(stopOrder = index + 1) }

        availableOrders.value = available
        activeRouteOrders.value = active
        todayCompletedCount.value = rows.count { it.driverId == userId && it.status == "finalizado" }
        availabilityRepository.setRouteActive(active.isNotEmpty())
        ensureRealtime(userId, storeIds)
    }

    private suspend fun fetchAcceptedStoreIds(userId: String): List<String> = supabase
        .from("store_drivers")
                    .select(columns = Columns.list("id", "store_id", "status")) {

            filter { eq("driver_user_id", userId) }
        }
        .decodeList<StoreDriverLinkRow>()
        .filter { it.status.equals("accepted", ignoreCase = true) }
        .map { it.storeId }

    private suspend fun mapOrders(rows: List<OrderRow>): List<DeliveryOrder> {
        if (rows.isEmpty()) return emptyList()
        val storeIds = rows.map { it.storeId }.distinct()
        val orderIds = rows.map { it.id }
        val driverLocation = AddressGeocodingCache.currentDriverLocation(ItaSuperApplication.appContext)

        val stores = supabase
            .from("stores_driver_view")
            .select(columns = Columns.list("id", "name", "address_street", "address_neighborhood", "address_city", "latitude", "longitude", "driver_pin_autofill")) {
                filter { isIn("id", storeIds) }
            }
            .decodeList<StoreRow>()
            .associateBy { it.id }

        val items = supabase
            .from("order_items")
            .select(columns = Columns.list("id", "order_id", "product_id", "quantity", "unit_price")) {
                filter { isIn("order_id", orderIds) }
            }
            .decodeList<OrderItemRow>()

        val productIds = items.map { it.productId }.distinct()
        val products = if (productIds.isEmpty()) emptyMap() else supabase
            .from("products")
            .select(columns = Columns.list("id", "name")) {
                filter { isIn("id", productIds) }
            }
            .decodeList<ProductRow>()
            .associateBy { it.id }

        // O telefone e o nome são dados pessoais. Em vez de consultar a visão
        // aberta de perfis, usamos a RPC que só devolve contato de entregas já
        // atribuídas ao motoboy autenticado.
        val contacts = runCatching {
            supabase.postgrest.rpc("get_delivery_contacts", buildJsonObject {
                put("_order_ids", buildJsonArray {
                    orderIds.forEach { add(JsonPrimitive(it)) }
                })
            }).decodeList<ProfileContactRow>().associateBy { it.userId }
        }.getOrDefault(emptyMap())

        return rows.map { row ->
            val storeRow = stores[row.storeId]
            val resolvedDestination = if (row.clientLat == null || row.clientLng == null) {
                AddressGeocodingCache.resolve(
                    context = ItaSuperApplication.appContext,
                    address = row.addressDetails.orEmpty(),
                    neighborhood = row.neighborhood.orEmpty(),
                    city = row.deliveryCity ?: storeRow?.addressCity.orEmpty()
                )
            } else {
                null
            }
            val destinationLatitude = row.clientLat ?: resolvedDestination?.latitude
            val destinationLongitude = row.clientLng ?: resolvedDestination?.longitude
            val distanceKm = driverLocation?.let { currentLocation ->
                haversineKm(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    destinationLatitude,
                    destinationLongitude
                )
            }
            val contact = row.clientId?.let(contacts::get)
            val deliveryItems = items.filter { it.orderId == row.id }.map { item ->
                DeliveryItem(
                    id = item.id,
                    name = products[item.productId]?.name ?: "Item do pedido",
                    quantity = item.quantity,
                    unitPrice = item.unitPrice
                )
            }
            val address = row.addressDetails.orEmpty().ifBlank { "Endereço não informado" }
            DeliveryOrder(
                id = row.id,
                shortCode = OrderDisplayCode.fromOrderId(row.id),
                store = LinkedStore(
                    id = row.storeId,
                    name = storeRow?.name ?: "Loja ItaSuper",
                    tradeName = storeRow?.name ?: "Loja ItaSuper",
                    address = storeRow?.addressStreet.orEmpty(),
                    neighborhood = storeRow?.addressNeighborhood.orEmpty(),
                    city = storeRow?.addressCity.orEmpty(),
                    phone = "",
                    cnpj = ""
                ),
                status = row.status.toDeliveryStatus(),
                customerName = contact?.fullName?.takeIf { it.isNotBlank() } ?: "Cliente",
                customerPhone = contact?.whatsappNumber?.takeIf { it.isNotBlank() } ?: contact?.phone.orEmpty(),
                addressStreet = address,
                addressNumber = "",
                addressNeighborhood = row.neighborhood.orEmpty(),
                addressCity = row.deliveryCity ?: storeRow?.addressCity.orEmpty(),
                addressState = row.deliveryState.orEmpty(),
                addressCep = row.deliveryCep.orEmpty(),
                destinationLatitude = destinationLatitude,
                destinationLongitude = destinationLongitude,
                deliveryFee = row.deliveryFee ?: 0.0,
                driverEarnings = row.deliveryFee ?: 0.0,
                estimatedDistanceKm = distanceKm ?: 0.0,
                estimatedTimeMinutes = 0,
                items = deliveryItems,
                payment = PaymentSummary(
                    method = row.paymentMethod ?: "Não informado",
                    amount = row.totalPrice ?: row.subtotal ?: 0.0,
                    changeFor = row.changeFor,
                    isPaidOnline = row.paymentMethod?.contains("pix", ignoreCase = true) == true
                ),
                customerPin = row.deliveryPin.orEmpty(),
                isPinPreFilledAllowed = storeRow?.driverPinAutofill == true,
                notes = null,
                createdAt = row.createdAt.orEmpty(),
                acceptedAt = if (row.driverId != null) row.createdAt else null,
                departedAt = if (row.status == "saiu_entrega" || row.status == "em_transito") row.createdAt else null,
                deliveredAt = row.confirmedAt
            )
        }
    }

    /**
     * Distância em linha reta entre loja e destino, calculada somente quando
     * as coordenadas reais dos dois pontos foram salvas no pedido.
     */
    private fun haversineKm(
        originLat: Double?,
        originLng: Double?,
        clientLat: Double?,
        clientLng: Double?
    ): Double? {
        if (originLat == null || originLng == null || clientLat == null || clientLng == null) {
            return null
        }
        val earthRadiusKm = 6371.0
        val latitudeDelta = Math.toRadians(clientLat - originLat)
        val longitudeDelta = Math.toRadians(clientLng - originLng)
        val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(Math.toRadians(originLat)) * cos(Math.toRadians(clientLat)) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private suspend fun updateOrderStatus(orderId: String, status: String) {
        supabase
            .from("orders")
            .update({ set("status", status) }) {
                filter { eq("id", orderId) }
            }
    }

    private fun ensureRealtime(userId: String, storeIds: List<String>) {
        val expected = storeIds.toSet()
        if (expected == observedStoreIds && channel != null) return

        realtimeJobs.forEach { it.cancel() }
        realtimeJobs.clear()
        val previous = channel
        channel = null
        observedStoreIds = expected
        scope.launch { runCatching { previous?.unsubscribe() } }

        val newChannel = supabase.realtime.channel("store-driver-rt-$userId") { }
        channel = newChannel
        storeIds.forEach { storeId ->
            val changes = newChannel.postgresChangeFlow<PostgresAction>("public") {
                table = "orders"
                filter("store_id", FilterOperator.EQ, storeId)
            }
            realtimeJobs += scope.launch {
                runCatching {
                    changes.collect { action ->
                        val knownAvailableIds = availableOrders.value.map { it.id }.toSet()
                        val hasActiveRoute = activeRouteOrders.value.isNotEmpty()
                        runCatching { refreshAll() }

                        if (action is PostgresAction.Insert && !hasActiveRoute) {
                            val newAvailableOrder = availableOrders.value.firstOrNull {
                                it.id !in knownAvailableIds
                            }
                            newAvailableOrder?.let { order ->
                                runCatching {
                                    DriverNotificationHelper.showIncomingOrder(
                                        context = ItaSuperApplication.appContext,
                                        orderId = order.id,
                                        storeName = order.store.tradeName,
                                        neighborhood = order.addressNeighborhood,
                                        shortCode = order.shortCode
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        scope.launch { runCatching { newChannel.subscribe(blockUntilSubscribed = false) } }
    }

    private fun clearState() {
        availableOrders.value = emptyList()
        activeRouteOrders.value = emptyList()
        todayCompletedCount.value = 0
    }

    private fun String.toDeliveryStatus(): OrderDeliveryStatus = when (lowercase()) {
        "pronto_para_entrega" -> OrderDeliveryStatus.PRONTO_PARA_ENTREGA
        "saiu_entrega" -> OrderDeliveryStatus.SAIU_ENTREGA
        "em_transito" -> OrderDeliveryStatus.EM_TRANSITO
        else -> OrderDeliveryStatus.FINALIZADO
    }
}
