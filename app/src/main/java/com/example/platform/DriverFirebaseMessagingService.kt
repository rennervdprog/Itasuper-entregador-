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
        // Fase 1 (correção do teste ao vivo de 08/11): a PRIMEIRA pergunta
        // é local — o serviço de rastreio grava "active_order_id" nas
        // preferências "driver_tracking" quando a rota sai e apaga quando
        // ela termina (é a mesma marca que o BootReceiver usa). Se ela
        // existe, a rota está na rua: aviso discreto, sem depender de
        // rede, sessão ou consulta ao banco no momento do push (a versão
        // anterior consultava o banco e, sem conseguir confirmar, caía
        // no alerta de tela cheia mesmo com a rota na rua).
        val trackingActive = runCatching {
            applicationContext
                .getSharedPreferences("driver_tracking", android.content.Context.MODE_PRIVATE)
                .getString("active_order_id", null) != null
        }.getOrDefault(false)
        if (trackingActive) {
            val storeName = data["store_name"].orEmpty().ifBlank { "Loja ItaSuper" }
            runCatching {
                DriverNotificationHelper.showRemotePush(
                    applicationContext,
                    "Novo pedido pronto — $storeName",
                    "Ele fica esperando e entra na sua próxima rota, quando você voltar à loja."
                )
            }
            return
        }

        val client = ItaSuperSupabase.client
        val userId = client.auth.currentUserOrNull()?.id
        val activeStatuses = listOf("pronto_para_entrega", "saiu_entrega", "em_transito")
        val myActiveOrders = if (userId == null) emptyList() else runCatching {
            client.from("orders")
                .select(columns = Columns.list("id", "status")) {
                    filter {
                        eq("driver_id", userId)
                        isIn("status", activeStatuses)
                    }
                }
                .decodeList<IncomingOrderState>()
        }.getOrDefault(emptyList())
        // Fase 1: pedido aceito ainda na loja NÃO abafa o alerta (ele pode
        // entrar na mesma rota). Só a rota já despachada (saiu_entrega /
        // em_transito) troca o alerta de tela cheia por um aviso discreto.
        val routeDeparted = myActiveOrders.any {
            it.status == "saiu_entrega" || it.status == "em_transito"
        }
        if (routeDeparted) {
            val storeName = data["store_name"].orEmpty().ifBlank { "Loja ItaSuper" }
            runCatching {
                DriverNotificationHelper.showRemotePush(
                    applicationContext,
                    "Novo pedido pronto — $storeName",
                    "Ele fica esperando e entra na sua próxima rota, quando você voltar à loja."
                )
            }
            return
        }

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
        // Ordem importa (2.5.9): a notificação posta (canal silencioso)
        // e toca o aviso único pelo alertAttention; a sobreposição abre
        // logo depois e o alertAttention dela é suprimido pela guarda de
        // som — 1× por pedido, sem dobrar.
        withContext(Dispatchers.Main.immediate) {
            DriverNotificationHelper.showIncomingOrder(
                context = applicationContext,
                orderId = alert.orderId,
                storeName = alert.storeName,
                neighborhood = alert.neighborhood,
                shortCode = alert.shortCode,
                alert = alert
            )
            IncomingOrderOverlay.show(
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

        // O nome que o servidor manda no push é o oficial (lido da loja com
        // acesso total); a vista de lojas é só enriquecimento — ela depende
        // da sessão/vínculo e pode voltar vazia com o app frio.
        val storeName = data["store_name"].orEmpty()
            .ifBlank { store?.name.orEmpty() }
            .ifBlank { "Loja ItaSuper" }
        val pickupAddress = listOfNotNull(
            store?.addressStreet?.takeIf { it.isNotBlank() },
            store?.addressNeighborhood?.takeIf { it.isNotBlank() },
            store?.addressCity?.takeIf { it.isNotBlank() }
        ).joinToString(" · ").ifBlank {
            // A vista de lojas só mostra lojas com vínculo aceito; fora dela,
            // o endereço de retirada vem nos dados do próprio push.
            data["pickup_address"].orEmpty().ifBlank { "Retirada na loja parceira" }
        }
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
            // Fase 2: o valor aparece ROTULADO no alerta ("Total do
            // pedido: R$ X") — antes era o número solto, sem dizer o que é.
            totalLabel = order.totalPrice?.let { "Total do pedido: ${formatCurrency(it)}" }.orEmpty()
        )
    }

    private fun fallbackAlert(orderId: String, data: Map<String, String>) = IncomingOrderAlert(
        orderId = orderId,
        shortCode = OrderDisplayCode.fromOrderId(orderId),
        storeName = data["store_name"].orEmpty().ifBlank { "Loja ItaSuper" },
        pickupAddress = data["pickup_address"].orEmpty().ifBlank { "Retirada na loja parceira" },
        neighborhood = data["neighborhood"].orEmpty(),
        destinationAddress = data["neighborhood"].orEmpty().ifBlank { "Destino disponível" }
    )

    private fun formatCurrency(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}
