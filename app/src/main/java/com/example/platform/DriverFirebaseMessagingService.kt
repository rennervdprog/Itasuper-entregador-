package com.example.platform

import android.os.Build
import com.example.ItaSuperApplication
import com.example.data.model.IncomingOrderAlert
import com.example.data.model.OrderDisplayCode
import com.example.data.remote.ItaSuperSupabase
import com.example.data.remote.OrderItemRow
import com.example.data.remote.OrderRow
import com.example.data.remote.StoreRow
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
private data class PushDeviceRegistration(
    val fcm_token: String,
    val device_info: String
)

@Serializable
private data class IncomingOrderState(
    val id: String,
    val status: String,
    @kotlinx.serialization.SerialName("driver_id") val driverId: String? = null
)

/** Registra e reatribui o token FCM quando a conta de motoboy muda no dispositivo. */
object DriverPushRegistration {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun reclaimForAuthenticatedDriver() {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                scope.launch { register(token) }
            }
        }
    }

    fun registerFromService(token: String) {
        if (token.isBlank()) return
        scope.launch { runCatching { register(token) } }
    }

    private suspend fun register(token: String) {
        if (token.isBlank() || ItaSuperSupabase.client.auth.currentUserOrNull() == null) return
        runCatching {
            ItaSuperSupabase.client.functions.invoke(
                function = "register-push-device",
                body = PushDeviceRegistration(
                    fcm_token = token,
                    device_info = "android:${Build.MANUFACTURER} ${Build.MODEL}; api:${Build.VERSION.SDK_INT}"
                ),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
    }
}

class DriverFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        runCatching { DriverPushRegistration.registerFromService(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data["event"] == "driver_delivery_available") {
            val orderId = message.data["order_id"].orEmpty()
            if (orderId.isNotBlank()) scope.launch { showDeliveryAlertIfEligible(orderId, message.data) }
            return
        }

        val title = message.notification?.title ?: message.data["title"] ?: "ItaSuper Entregador"
        val body = message.notification?.body ?: message.data["body"] ?: "Há uma atualização para sua entrega."
        runCatching { DriverNotificationHelper.showRemotePush(applicationContext, title, body) }
    }

    private suspend fun showDeliveryAlertIfEligible(orderId: String, data: Map<String, String>) {
        val client = ItaSuperSupabase.client
        val userId = client.auth.currentUserOrNull()?.id
        val activeStatuses = listOf("pronto_para_entrega", "saiu_entrega", "em_transito")
        val hasActiveRoute = if (userId == null) false else runCatching {
            client.from("orders")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("driver_id", userId)
                        isIn("status", activeStatuses)
                    }
                }
                .decodeList<IncomingOrderState>()
                .isNotEmpty()
        }.getOrDefault(false)
        if (hasActiveRoute) return

        val order = runCatching {
            client.from("orders")
                .select(columns = Columns.list(
                    "id", "store_id", "status", "driver_id", "neighborhood", "address_details",
                    "delivery_city", "delivery_state", "payment_method", "total_price"
                )) { filter { eq("id", orderId) } }
                .decodeList<OrderRow>()
                .firstOrNull()
        }.getOrNull()

        if (order != null && (order.status != "pronto_para_entrega" || order.driverId != null)) return
        val alert = order?.let { buildIncomingAlert(it, data) } ?: fallbackAlert(orderId, data)

        // FCM e as consultas rodam em IO; a janela Android deve ser criada na Main.
        withContext(Dispatchers.Main.immediate) {
            IncomingOrderOverlay.show(
                context = applicationContext,
                orderId = alert.orderId,
                storeName = alert.storeName,
                neighborhood = alert.neighborhood,
                shortCode = alert.shortCode,
                alert = alert
            )
            DriverNotificationHelper.showIncomingOrder(
                context = applicationContext,
                orderId = alert.orderId,
                storeName = alert.storeName,
                neighborhood = alert.neighborhood,
                shortCode = alert.shortCode,
                alert = alert
            )
        }
    }

    private suspend fun buildIncomingAlert(order: OrderRow, data: Map<String, String>): IncomingOrderAlert {
        val client = ItaSuperSupabase.client
        val store = runCatching {
            client.from("stores_driver_view")
                .select(columns = Columns.list("id", "name", "address_street", "address_neighborhood", "address_city")) {
                    filter { eq("id", order.storeId) }
                }
                .decodeList<StoreRow>()
                .firstOrNull()
        }.getOrNull()
        val itemCount = runCatching {
            client.from("order_items")
                .select(columns = Columns.list("id", "order_id", "product_id", "quantity", "unit_price")) {
                    filter { eq("order_id", order.id) }
                }
                .decodeList<OrderItemRow>()
                .sumOf { it.quantity }
        }.getOrDefault(0)

        val storeName = store?.name?.takeIf { it.isNotBlank() }
            ?: data["store_name"].orEmpty().ifBlank { "Loja ItaSuper" }
        val pickupAddress = listOfNotNull(
            store?.addressStreet?.takeIf { it.isNotBlank() },
            store?.addressNeighborhood?.takeIf { it.isNotBlank() },
            store?.addressCity?.takeIf { it.isNotBlank() }
        ).joinToString(" · ").ifBlank { "Retirada na loja parceira" }
        val destination = order.addressDetails.orEmpty().ifBlank {
            listOfNotNull(
                order.neighborhood?.takeIf { it.isNotBlank() },
                order.deliveryCity?.takeIf { it.isNotBlank() },
                order.deliveryState?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
        }.ifBlank { data["neighborhood"].orEmpty().ifBlank { "Destino disponível" } }

        return IncomingOrderAlert(
            orderId = order.id,
            shortCode = OrderDisplayCode.fromOrderId(order.id),
            storeName = storeName,
            pickupAddress = pickupAddress,
            destinationAddress = destination,
            neighborhood = order.neighborhood.orEmpty(),
            itemCount = itemCount,
            paymentMethod = order.paymentMethod.orEmpty(),
            totalLabel = order.totalPrice?.let(::formatCurrency).orEmpty()
        )
    }

    private fun fallbackAlert(orderId: String, data: Map<String, String>) = IncomingOrderAlert(
        orderId = orderId,
        shortCode = OrderDisplayCode.fromOrderId(orderId),
        storeName = data["store_name"].orEmpty().ifBlank { "Loja ItaSuper" },
        neighborhood = data["neighborhood"].orEmpty(),
        destinationAddress = data["neighborhood"].orEmpty().ifBlank { "Destino disponível" }
    )

    private fun formatCurrency(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}
