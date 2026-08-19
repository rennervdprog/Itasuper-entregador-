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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.fake.AppContainer
import com.example.ui.components.DriverHelmetMark
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSuperTheme
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
                    onAccept = ::acceptOrder
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
                    startActivity(
                        Intent(this@IncomingOrderActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    finish()
                }
                .onFailure { error ->
                    isSubmitting = false
                    errorMessage = error.message ?: "Não foi possível aceitar esta entrega."
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

@androidx.compose.runtime.Composable
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
    onAccept: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DriverHelmetMark(
            contentDescription = "ItaSuper Entregador",
            modifier = Modifier.size(68.dp),
            tint = ItaOrange
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Nova entrega disponível",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Você não possui rota ativa.",
            fontSize = 15.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = storeName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shortCode,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = ItaOrange,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = destinationAddress.ifBlank { neighborhood },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Retirada",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = pickupAddress.ifBlank { "Na loja parceira" },
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                val orderDetails = listOfNotNull(
                    itemCount.takeIf { it > 0 }?.let { "$it ${if (it == 1) "item" else "itens"}" },
                    paymentMethod.takeIf { it.isNotBlank() },
                    totalLabel.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (orderDetails.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = orderDetails,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ItaOrange
                    )
                }
            }
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFB91C1C),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onAccept,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isSubmitting) "Aceitando..." else "Aceitar entrega",
                modifier = Modifier.padding(vertical = 6.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
