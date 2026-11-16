package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate600
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

/**
 * Herói online/offline do Modo Pilotagem — tudo em modo claro, sem o canvas
 * escuro antigo. Offline, a decisão do turno é um BOTÃO
 * GIGANTE laranja de 72dp, não um Switch. A semântica é a mesma de antes:
 * em rota, desligar fica bloqueado até concluir a rota.
 */
@Composable
fun OnlineHeroToggle(
    isOnline: Boolean,
    isRouteActive: Boolean = false,
    visibleStoresCount: Int = 0,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulso do ponto verde online — o mesmo padrão da RouteLine.
    val pulse by rememberInfiniteTransition(label = "heroOnlinePulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "heroOnlinePulseAlpha"
    )

    if (!isOnline) {
        OfflineHero(
            onGoOnline = { onToggle(true) },
            modifier = modifier
        )
    } else if (isRouteActive) {
        RouteActiveHero(modifier = modifier)
    } else {
        OnlineHero(
            pulseAlpha = pulse,
            visibleStoresCount = visibleStoresCount,
            onGoOffline = { onToggle(false) },
            modifier = modifier
        )
    }
}

@Composable
private fun OfflineHero(
    onGoOnline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_online_toggle_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = BorderStroke(1.dp, ItaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Selo de estado
            Row(
                modifier = Modifier
                    .background(ItaSlate100, RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = null,
                    tint = ItaSlate600,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline",
                    style = MaterialTheme.typography.labelMedium,
                    color = ItaSlate600
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Você está fora do jogo.",
                style = MaterialTheme.typography.headlineMedium,
                color = ItaTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nenhum pedido chega enquanto você está offline. " +
                    "Quando estiver pronto para rodar, é um toque.",
                style = MaterialTheme.typography.bodyMedium,
                color = ItaTextSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGoOnline,
                colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .testTag("btn_go_online_hero")
            ) {
                Icon(
                    imageVector = Icons.Filled.ElectricBike,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FICAR ONLINE",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun OnlineHero(
    pulseAlpha: Float,
    visibleStoresCount: Int,
    onGoOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_online_toggle_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = BorderStroke(1.dp, ItaGreenLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(pulseAlpha)
                        .background(ItaGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Online · recebendo pedidos",
                        style = MaterialTheme.typography.titleSmall,
                        color = ItaGreenDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (visibleStoresCount > 0) {
                            "Recebendo pedidos de $visibleStoresCount " +
                                if (visibleStoresCount == 1) "loja vinculada" else "lojas vinculadas"
                        } else {
                            "Recebendo pedidos das suas lojas"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ItaTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = onGoOffline,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, ItaBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ItaTextSecondary),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Ficar offline",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun RouteActiveHero(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_online_toggle_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ItaOrangeLight),
        border = BorderStroke(1.dp, ItaOrange),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(ItaOrange, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Você está em rota",
                    style = MaterialTheme.typography.titleSmall,
                    color = ItaTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Disponibilidade para novos pedidos pausada até concluir a rota",
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaSlate500
                )
            }
        }
    }
}
