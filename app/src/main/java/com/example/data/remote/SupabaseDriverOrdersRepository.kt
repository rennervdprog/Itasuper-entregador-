package com.example.data.remote

import com.example.data.model.DeliveryItem
import com.example.data.model.DeliveryOrder
import com.example.data.model.DriverContact
import com.example.data.model.OfflineDeliveryConfirmation
import com.example.data.model.OrderDeliveryStatus
import com.example.data.model.OrderDisplayCode
import com.example.data.model.PaymentSummary
import com.example.data.model.isOnlinePaidMethod
import com.example.ItaSuperApplication
import com.example.data.local.OfflineDeliveryQueue
import com.example.data.model.LinkedStore
import com.example.platform.AddressGeocodingCache
import com.example.platform.DriverTrackingService
import com.example.platform.DriverNotificationHelper
import com.example.platform.IncomingOrderOverlay
import com.example.platform.NetworkMonitor
import com.example.data.repository.DriverAvailabilityRepository
import com.example.data.repository.DriverOrdersRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    // Coalescência do Realtime (auditoria M12): uma rajada de mudanças em
    // orders disparava um refreshAll() completo POR EVENTO. Agora os
    // eventos pedem um único refresh compartilhado; o primeiro Insert da
    // rajada guarda a foto (ids disponíveis + rota ativa) p/ o aviso de
    // pedido novo continuar com o mesmo comportamento.
    private var pendingRefreshJob: Job? = null
    private var pendingInsertBaseline: Set<String>? = null
    private var pendingInsertHadDepartedRoute: Boolean = false

    // Fase 2: foto da rota ativa da atualização anterior (pedido →
    // código curto), pra detectar as saídas que merecem aviso:
    // cancelamento e confirmação de recebimento pelo cliente. null até
    // a primeira carga — transição só se anuncia com base conhecida.
    private var previousRouteSnapshot: Map<String, String>? = null

    init {
        // A primeira sincronização não pode derrubar o aplicativo se rede, RLS
        // ou schema estiverem temporariamente indisponíveis.
        scope.launch { runCatching { refreshAll() } }
    }

    override fun getAvailableOrders(): Flow<List<DeliveryOrder>> = availableOrders.asStateFlow()
    override fun getActiveRouteOrders(): Flow<List<DeliveryOrder>> = activeRouteOrders.asStateFlow()
    override fun getTodayCompletedCount(): Flow<Int> = todayCompletedCount.asStateFlow()
    override fun getOfflineConfirmations(): Flow<List<OfflineDeliveryConfirmation>> = offlineQueue.observe()

    /**
     * Quando o processo é iniciado pelo push (app fechado), a sessão
     * salva ainda pode estar carregando — a restauração é assíncrona.
     * Chamar o RPC de aceite nesse instante sai sem usuário e o
     * servidor recusa ("sessão inválida"), embora por dentro do app o
     * mesmo aceite funcione. Esperar a sessão sair do estado inicial
     * antes de aceitar; com o app já aberto, retorna na hora.
     */
    private suspend fun awaitSessionReady() {
        runCatching {
            withTimeoutOrNull(10_000) {
                supabase.auth.sessionStatus.first { status -> status !is SessionStatus.Initializing }
            }
        }
    }

    override suspend fun acceptOrder(orderId: String): Result<Unit> = runCatching {
        awaitSessionReady()
        supabase.postgrest.rpc("driver_accept_order", buildJsonObject {
            put("_order_id", orderId)
        })
        refreshAll()
    }

    override suspend fun acceptAllOrders(orderIds: List<String>): Result<Unit> = runCatching {
        require(orderIds.isNotEmpty()) { "Nenhum pedido disponível para aceitar." }
        awaitSessionReady()
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
            // Sem o código do cliente baixado neste aparelho, não há como
            // validar a confirmação offline: aceitar qualquer PIN seria um
            // carimbo sem prova (auditoria A3). Nesse caso a confirmação
            // exige conexão — o pedido permanece na rota do motoboy.
            if (activeOrder.customerPin.isBlank()) {
                error("Sem o código do cliente neste aparelho, a confirmação precisa de conexão. Conecte-se à internet para confirmar esta entrega.")
            }
            if (activeOrder.customerPin != normalizedPin) {
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

    override suspend fun refreshAfterAppResume() {
        refreshAll(forceRealtimeReconnect = true)
    }

    suspend fun refreshAll(forceRealtimeReconnect: Boolean = false) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: run {
            // Ao voltar do Waze ou após minimizar com gestos, o Android pode
            // entregar o primeiro frame antes de o Auth terminar de reidratar
            // a sessão persistida. Não apagar pedidos nesse intervalo: lançar
            // erro faz o ViewModel repetir a recuperação automaticamente.
            error("Sessão ainda sendo restaurada. Aguarde um instante.")
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
                "assigned_driver_id", "order_number", "metadata", "delivery_confirmed_by_client"
            )) {
                filter {
                    isIn("store_id", storeIds)
                    // Fase 4.3: recorte no servidor — a lista de trabalho
                    // só precisa dos pedidos ABERTOS (disponíveis + rota).
                    // Concluídos e cancelados antigos não descem mais a
                    // cada atualização: o histórico tem repositório
                    // próprio, o contador do dia vem de contagem dedicada
                    // e os avisos da Fase 2 buscam o estado de quem saiu
                    // da rota pontualmente.
                    isIn("status", listOf("pronto_para_entrega", "saiu_entrega", "em_transito"))
                }
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
        val activeMapped = mappedOrders.filter { order ->
            val row = rowsById[order.id] ?: return@filter false
            row.driverId == userId && row.status.lowercase() in activeStatuses
        }
        // Ordem automática da rota (vizinho mais próximo): quem já saiu é
        // sequenciado da posição atual do motoboy; quem ainda está na loja,
        // a partir da loja. Sem posição/loja conhecida, vale a ordem de
        // chegada — o otimizador decide por bloco.
        val driverLoc = AddressGeocodingCache.currentDriverLocation(ItaSuperApplication.appContext)
        val routeStoreLat = activeMapped.firstOrNull()?.store?.latitude
        val routeStoreLng = activeMapped.firstOrNull()?.store?.longitude
        val active = optimizeRouteStops(
            orders = activeMapped,
            departedOriginLat = driverLoc?.latitude ?: routeStoreLat,
            departedOriginLng = driverLoc?.longitude ?: routeStoreLng,
            readyOriginLat = routeStoreLat ?: driverLoc?.latitude,
            readyOriginLng = routeStoreLng ?: driverLoc?.longitude
        )

        availableOrders.value = available
        activeRouteOrders.value = active

        // Fase 2 — avisos de transição da rota: pedido que SAIU da rota
        // ativa desde a última atualização porque foi CANCELADO ou
        // porque o CLIENTE confirmou o recebimento gera notificação.
        // (Finalização pelo próprio motoboy, com PIN, não avisa — ele
        // já sabe.) E a sobreposição de oferta que não existe mais
        // (outro motoboy aceitou, loja cancelou) se fecha sozinha.
        runCatching {
            val appContext = ItaSuperApplication.appContext
            val currentActiveIds = active.map { it.id }.toSet()
            val previous = previousRouteSnapshot
            if (previous != null) {
                // Fase 4.3: quem saiu da rota não está mais na consulta
                // principal (só abertos) — o estado atual desses poucos
                // pedidos vem de uma busca pontual, só com as colunas
                // que o aviso precisa.
                val leftIds = previous.keys.filter { it !in currentActiveIds }
                val transitionById = fetchOrderTransitions(leftIds)
                previous.forEach { (orderId, shortCode) ->
                    if (orderId in currentActiveIds) return@forEach
                    val row = transitionById[orderId] ?: return@forEach
                    when {
                        row.status.equals("cancelado", ignoreCase = true) ->
                            DriverNotificationHelper.showOrderEvent(
                                appContext,
                                orderId,
                                "Pedido cancelado",
                                "$shortCode saiu da sua rota: o pedido foi cancelado."
                            )
                        (row.status.equals("finalizado", ignoreCase = true) ||
                            row.status.equals("entregue", ignoreCase = true)) &&
                            row.deliveryConfirmedByClient == true ->
                            DriverNotificationHelper.showOrderEvent(
                                appContext,
                                orderId,
                                "Cliente confirmou o recebimento",
                                "$shortCode foi concluído pelo cliente — não precisa do PIN."
                            )
                    }
                }
            }
            previousRouteSnapshot = active.associate { it.id to it.shortCode }

            IncomingOrderOverlay.shownOrderId()?.let { overlayOrderId ->
                val stillOffered = available.any { it.id == overlayOrderId }
                val mineNow = overlayOrderId in currentActiveIds
                if (!stillOffered && !mineNow) {
                    IncomingOrderOverlay.dismissIfShowing(overlayOrderId)
                    DriverNotificationHelper.cancelIncomingOrderNotification(appContext, overlayOrderId)
                }
            }
        }
        // Fase 4.3: o contador "Concluídas" agora é o do DIA de verdade
        // — o nome do fluxo sempre prometeu isso, mas ele contava o
        // histórico inteiro que a consulta antiga baixava. Contagem no
        // servidor (nenhuma linha desce): finalizado E entregue contam
        // (a RPC grava um ou outro conforme o vínculo — auditoria A4),
        // pela data de confirmação, no dia do aparelho.
        runCatching {
            todayCompletedCount.value = fetchTodayCompletedCount(userId)
        }
        availabilityRepository.setRouteActive(active.isNotEmpty())
        ensureRealtime(userId, storeIds, forceReconnect = forceRealtimeReconnect)
    }

    /** Estado mínimo de um pedido que saiu da rota (Fases 2 + 4.3). */
    @Serializable
    private data class OrderTransitionRow(
        val id: String,
        val status: String,
        @SerialName("delivery_confirmed_by_client") val deliveryConfirmedByClient: Boolean? = null
    )

    private suspend fun fetchOrderTransitions(ids: List<String>): Map<String, OrderTransitionRow> {
        if (ids.isEmpty()) return emptyMap()
        return runCatching {
            supabase.from("orders")
                .select(columns = Columns.list("id", "status", "delivery_confirmed_by_client")) {
                    filter { isIn("id", ids) }
                }
                .decodeList<OrderTransitionRow>()
                .associateBy { it.id }
        }.getOrElse { emptyMap() }
    }

    /**
     * Concluídas de HOJE pelo servidor (Fase 4.3): contagem exata sem
     * baixar nenhuma linha. O dia é o do aparelho (meia-noite local) e a
     * data é a da confirmação da entrega.
     */
    private suspend fun fetchTodayCompletedCount(userId: String): Int {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        val startIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
            .format(startOfDay)
        val result = supabase.from("orders").select(columns = Columns.list("id")) {
            count(Count.EXACT)
            limit(0)
            filter {
                eq("driver_id", userId)
                isIn("status", listOf("finalizado", "entregue"))
                gte("confirmed_at", startIso)
            }
        }
        return result.countOrNull()?.toInt() ?: 0
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
            .select(columns = Columns.list("id", "name", "address_street", "address_neighborhood", "address_city", "latitude", "longitude")) {
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
            val destinationPrecision = row.metadata
                ?.get("delivery_quote")
                ?.jsonObject
                ?.get("destination_precision")
                ?.jsonPrimitive
                ?.content
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
                    cnpj = "",
                    latitude = storeRow?.latitude,
                    longitude = storeRow?.longitude
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
                destinationPrecision = destinationPrecision,
                deliveryFee = row.deliveryFee ?: 0.0,
                estimatedDistanceKm = distanceKm ?: 0.0,
                estimatedTimeMinutes = 0,
                items = deliveryItems,
                payment = PaymentSummary(
                    method = row.paymentMethod ?: "Não informado",
                    amount = row.totalPrice ?: row.subtotal ?: 0.0,
                    changeFor = row.changeFor,
                    // Só pix/pix_direto são pagos online; pix_machine e
                    // maquininha_pix são cobrados na entrega (auditoria A1).
                    isPaidOnline = isOnlinePaidMethod(row.paymentMethod)
                ),
                customerPin = row.deliveryPin.orEmpty(),
                createdAt = row.createdAt.orEmpty(),
                // Sem acceptedAt/departedAt: o schema não expõe esses marcos
                // e os campos antigos eram fabricados a partir de createdAt
                // (auditoria B4). deliveredAt vem de confirmed_at, que é real.
                deliveredAt = row.confirmedAt
            )
        }
    }

    // haversineKm() e o otimizador de rota vivem em RouteOptimizer.kt
    // (mesmo pacote): a distância em linha reta é compartilhada entre o
    // mapeamento dos pedidos e a ordenação automática das paradas.

    // updateOrderStatus() foi removido: escrevia orders.status direto, fora
    // das RPCs de aceite/partida/finalização, e não tinha nenhum chamador
    // (auditoria B5).

    /**
     * Janela de coalescência de 600ms: eventos que chegam com um refresh já
     * agendado são absorvidos por ele. O aviso de pedido novo usa a foto
     * capturada no primeiro Insert da rajada (antes de qualquer refresh
     * dela) e só dispara sem rota DESPACHADA (Fase 1: pedido aceito ainda
     * na loja não abafa o aviso, pois pode entrar na mesma rota), com
     * 1 refresh por rajada em vez de 1 por evento.
     */
    private fun requestCoalescedRefresh(isInsert: Boolean) {
        if (isInsert && pendingInsertBaseline == null) {
            pendingInsertBaseline = availableOrders.value.map { it.id }.toSet()
            // Além do estado em memória, vale a marca local do rastreio
            // ("driver_tracking"/active_order_id): rota na rua é rota na
            // rua, mesmo se o estado ainda não tiver sido recarregado.
            val trackingActive = runCatching {
                ItaSuperApplication.appContext
                    .getSharedPreferences("driver_tracking", android.content.Context.MODE_PRIVATE)
                    .getString("active_order_id", null) != null
            }.getOrDefault(false)
            pendingInsertHadDepartedRoute = trackingActive || activeRouteOrders.value.any {
                it.status == OrderDeliveryStatus.SAIU_ENTREGA ||
                    it.status == OrderDeliveryStatus.EM_TRANSITO
            }
        }
        if (pendingRefreshJob?.isActive == true) return
        pendingRefreshJob = scope.launch {
            delay(600)
            val baseline = pendingInsertBaseline
            val hadDepartedRoute = pendingInsertHadDepartedRoute
            pendingInsertBaseline = null
            runCatching { refreshAll() }
            if (baseline != null && !hadDepartedRoute) {
                val newAvailableOrder = availableOrders.value.firstOrNull { it.id !in baseline }
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

    private fun ensureRealtime(userId: String, storeIds: List<String>, forceReconnect: Boolean = false) {
        val expected = storeIds.toSet()
        val realtimeIsAlive = realtimeJobs.any { it.isActive }
        if (!forceReconnect && expected == observedStoreIds && channel != null && realtimeIsAlive) return

        realtimeJobs.forEach { it.cancel() }
        realtimeJobs.clear()
        pendingRefreshJob?.cancel()
        pendingRefreshJob = null
        pendingInsertBaseline = null
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
                        requestCoalescedRefresh(isInsert = action is PostgresAction.Insert)
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
