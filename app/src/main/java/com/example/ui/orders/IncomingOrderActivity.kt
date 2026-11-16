package com.example.ui.orders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.fake.AppContainer
import com.example.data.model.PaymentSummary
import com.example.data.model.isOnlinePaidMethod
import com.example.ui.common.DriverUserMessage
import com.example.ui.components.RouteLine
import com.example.ui.components.RouteStop
import com.example.ui.components.RouteStopState
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate200
import com.example.ui.theme.ItaSlate700
import com.example.ui.theme.ItaStatusCompletedBg
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSuperTheme
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary
import kotlinx.coroutines.launch

/**
 * Chamada de entrega disparada por notificação de alta prioridade.
 * Ela só é criada para pedidos disponíveis quando o motoboy não possui rota ativa.
 */
class IncomingOrderActivity : ComponentActivity() {
    private val orderId: String by lazy { intent.getStringExtra(EXTRA_ORDER_ID).orEmpty() }
    private val storeName: String by lazy { intent.getStringExtra(EXTRA_STORE_NAME).orEmpty() }
    private val neighborhood: String by lazy { intent.getStringExtra(EXTRA_NEIGHBORHOOD).orEmpty() }
    private val shortCode: String by lazy { intent.getStringExtra(EXTRA_SHORT_CODE).orEmpty() }
    private val pickupAddress: String by lazy { intent.getStringExtra(EXTRA_PICKUP_ADDRESS).orEmpty() }
    private val destinationAddress: String by lazy { intent.getStringExtra(EXTRA_DESTINATION_ADDRESS).orEmpty() }
    private val itemCount: Int by lazy { intent.getIntExtra(EXTRA_ITEM_COUNT, 0) }
    private val paymentMethod: String by lazy { intent.getStringExtra(EXTRA_PAYMENT_METHOD).orEmpty() }
    private val totalLabel: String by lazy { intent.getStringExtra(EXTRA_TOTAL_LABEL).orEmpty() }

    private var isSubmitting by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (orderId.isBlank()) {
            finish()
            return
        }

        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ItaSuperTheme {
                IncomingOrderContent(
                    storeName = storeName.ifBlank { "Loja ItaSuper" },
                    neighborhood = neighborhood.ifBlank { "Endereço disponível no pedido" },
                    shortCode = shortCode.ifBlank { "Novo pedido" },
                    pickupAddress = pickupAddress,
                    destinationAddress = destinationAddress,
                    itemCount = itemCount,
                    paymentMethod = paymentMethod,
                    totalLabel = totalLabel,
                    isSubmitting = isSubmitting,
                    errorMessage = errorMessage,
                    onAccept = ::acceptOrder,
                    // "Agora não" equivale a sair, como o Voltar do sistema
                    // faz hoje: fecha a atividade sem aceitar.
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun acceptOrder() {
        if (isSubmitting) return
        lifecycleScope.launch {
            isSubmitting = true
            errorMessage = null
            AppContainer.ordersRepository.acceptOrder(orderId)
                .onSuccess {
                    AppContainer.signalAcceptedOrder(orderId)
                    startActivity(
                        Intent(this@IncomingOrderActivity, MainActivity::class.java)
                            .putExtra(MainActivity.EXTRA_ACCEPTED_ORDER_ID, orderId)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    finish()
                }
                .onFailure { error ->
                    isSubmitting = false
                    errorMessage = DriverUserMessage.orderAcceptance(error)
                }
        }
    }

    companion object {
        private const val EXTRA_ORDER_ID = "incoming_order_id"
        private const val EXTRA_STORE_NAME = "incoming_store_name"
        private const val EXTRA_NEIGHBORHOOD = "incoming_neighborhood"
        private const val EXTRA_SHORT_CODE = "incoming_short_code"
        private const val EXTRA_PICKUP_ADDRESS = "incoming_pickup_address"
        private const val EXTRA_DESTINATION_ADDRESS = "incoming_destination_address"
        private const val EXTRA_ITEM_COUNT = "incoming_item_count"
        private const val EXTRA_PAYMENT_METHOD = "incoming_payment_method"
        private const val EXTRA_TOTAL_LABEL = "incoming_total_label"

        fun intent(
            context: Context,
            orderId: String,
            storeName: String,
            neighborhood: String,
            shortCode: String,
            pickupAddress: String = "",
            destinationAddress: String = "",
            itemCount: Int = 0,
            paymentMethod: String = "",
            totalLabel: String = ""
        ): Intent = Intent(context, IncomingOrderActivity::class.java)
            .putExtra(EXTRA_ORDER_ID, orderId)
            .putExtra(EXTRA_STORE_NAME, storeName)
            .putExtra(EXTRA_NEIGHBORHOOD, neighborhood)
            .putExtra(EXTRA_SHORT_CODE, shortCode)
            .putExtra(EXTRA_PICKUP_ADDRESS, pickupAddress)
            .putExtra(EXTRA_DESTINATION_ADDRESS, destinationAddress)
            .putExtra(EXTRA_ITEM_COUNT, itemCount)
            .putExtra(EXTRA_PAYMENT_METHOD, paymentMethod)
            .putExtra(EXTRA_TOTAL_LABEL, totalLabel)
    }
}

@Composable
private fun IncomingOrderContent(
    storeName: String,
    neighborhood: String,
    shortCode: String,
    pickupAddress: String,
    destinationAddress: String,
    itemCount: Int,
    paymentMethod: String,
    totalLabel: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    // Todas as cores de tinta são explícitas sobre o fundo claro — o título
    // usava onSurface e ficava invisível em dark mode (U-P1-9).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ItaBackground)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(ItaOrange, RoundedCornerShape(50.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NOVA ENTREGA",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = buildAnnotatedString {
                    append(storeName)
                    append(" · ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = ItaTextPrimary
                        )
                    ) {
                        append(shortCode)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = ItaTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // A linha da rota é a heroína: retirada → destino.
            RouteLine(
                stops = listOf(
                    RouteStop(
                        title = pickupAddress.ifBlank { "Retirada na loja parceira" },
                        subtitle = "Retirada · $storeName",
                        state = RouteStopState.DONE
                    ),
                    RouteStop(
                        title = destinationAddress.ifBlank { neighborhood },
                        subtitle = if (destinationAddress.isNotBlank() && neighborhood.isNotBlank()) {
                            "Destino · $neighborhood"
                        } else {
                            "Destino"
                        },
                        state = RouteStopState.CURRENT
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itemCount > 0) {
                        OfferPill(
                            text = "$itemCount ${if (itemCount == 1) "item" else "itens"}",
                            backgroundColor = ItaSlate200,
                            textColor = ItaSlate700
                        )
                    }
                    if (paymentMethod.isNotBlank()) {
                        // Rótulo reutilizado da instrução operacional do
                        // pagamento (mesma regra da rota ativa); o valor
                        // total já vem rotulado nos extras e é exibido
                        // verbatim, sem verbo "Cobrar" inventado.
                        val methodLabel = PaymentSummary(paymentMethod, 0.0)
                            .driverInstruction
                            .substringBefore(" — ")
                        val isPaid = isOnlinePaidMethod(paymentMethod)
                        OfferPill(
                            text = if (isPaid) "Já pago · $methodLabel" else methodLabel,
                            backgroundColor = if (isPaid) ItaStatusCompletedBg else ItaSlate200,
                            textColor = if (isPaid) ItaGreenDark else ItaSlate700
                        )
                    }
                }
                if (totalLabel.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OfferPill(
                            text = totalLabel,
                            backgroundColor = ItaOrange,
                            textColor = Color.White
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = ItaStatusDanger,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAccept,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
            shape = RoundedCornerShape(18.dp)
        ) {
            if (!isSubmitting) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isSubmitting) "Aceitando..." else "ACEITAR ENTREGA",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = "Agora não",
                style = MaterialTheme.typography.labelLarge,
                color = ItaTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Sem relógio correndo: a oferta fica até outro motoboy aceitar ou a loja cancelar.",
            style = MaterialTheme.typography.bodySmall,
            color = ItaTextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OfferPill(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
