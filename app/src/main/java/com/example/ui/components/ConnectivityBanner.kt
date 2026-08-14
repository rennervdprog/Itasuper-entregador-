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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
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
    isVisible: Boolean,
    offlineConfirmations: List<OfflineDeliveryConfirmation>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Banner de conectividade discreto
        AnimatedVisibility(visible = isVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("connectivity_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Rede Conectada",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sinal de dados estável · Itaúna / MG",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
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
