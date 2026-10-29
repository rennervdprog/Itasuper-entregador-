package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OfflineDeliveryConfirmation
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaStatusPending
import com.example.ui.theme.ItaStatusPendingBg

@Composable
fun ConnectivityBanner(
    offlineConfirmations: List<OfflineDeliveryConfirmation>,
    modifier: Modifier = Modifier
) {
    // A faixa "Sinal de dados estável" foi removida (auditoria B13): não há
    // medição de conectividade no app e o repositório emitia false fixo —
    // era código morto dos dois lados. Ficam as faixas reais da fila.
    Column(modifier = modifier.fillMaxWidth()) {
        // Faixa de erro: confirmações que o servidor RECUSOU (auditoria C2).
        // O erro fica persistido na fila e aparece aqui até a sincronização
        // passar — nada some em silêncio.
        val failedConfirmations = offlineConfirmations.filter { !it.lastError.isNullOrBlank() }
        AnimatedVisibility(visible = failedConfirmations.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("offline_error_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Erro de sincronização",
                    tint = Color(0xFFB91C1C),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${failedConfirmations.size} entrega(s) não confirmada(s) no servidor — " +
                        offlineSyncErrorText(failedConfirmations.first().lastError) +
                        ". Abra o pedido e confirme a entrega novamente.",
                    fontSize = 12.sp,
                    color = Color(0xFF991B1B),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Faixa de confirmações offline pendentes
        AnimatedVisibility(visible = offlineConfirmations.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(ItaStatusPendingBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("offline_pending_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sincronização pendente",
                        tint = ItaStatusPending,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${offlineConfirmations.size} entrega(s) aguardando sincronização de PIN",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Traduz o código de erro persistido da fila offline p/ o entregador. */
private fun offlineSyncErrorText(error: String?): String = when (error) {
    "invalid_pin" -> "PIN incorreto: confira o código com o cliente"
    "order_not_in_delivery_route" -> "o pedido não está mais em rota de entrega no servidor"
    "network_error" -> "sem conexão com o servidor"
    else -> "o servidor recusou a confirmação"
}
