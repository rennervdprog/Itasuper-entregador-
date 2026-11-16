package com.example.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.platform.PresenceHealthChecker
import com.example.platform.PresenceHealthState
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

/**
 * Cartão "Saúde do rastreio" no Perfil: mostra, item a item, o que falta
 * configurar neste aparelho para a loja enxergar o motoboy no mapa durante
 * o turno inteiro. Cada item pendente é tocável e abre a tela certa.
 */
@Composable
fun TrackingHealthCard(health: PresenceHealthState) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = BorderStroke(1.dp, ItaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Saúde do rastreio",
                style = MaterialTheme.typography.titleSmall,
                color = ItaTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (health.allOk) {
                    "Tudo certo: enquanto você estiver Online, suas lojas veem sua posição no mapa o turno inteiro."
                } else {
                    "Com os itens abaixo pendentes, sua posição pode sumir do mapa da loja no meio do turno. Toque num item para corrigir."
                },
                style = MaterialTheme.typography.bodySmall,
                color = ItaTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            HealthRow(
                ok = health.locationForeground,
                label = "Localização ativada",
                onFix = { context.startActivity(PresenceHealthChecker.appDetailsIntent(context)) }
            )
            HealthRow(
                ok = health.locationBackground,
                label = "Localização \"o tempo todo\"",
                onFix = { context.startActivity(PresenceHealthChecker.appDetailsIntent(context)) }
            )
            HealthRow(
                ok = health.batteryUnrestricted,
                label = "Bateria sem restrição",
                onFix = { context.startActivity(PresenceHealthChecker.batteryIntent(context)) }
            )
            HealthRow(
                ok = health.notificationsGranted,
                label = "Notificações ativadas",
                onFix = { context.startActivity(PresenceHealthChecker.appDetailsIntent(context)) }
            )
            HealthRow(
                ok = health.overlayGranted,
                label = "Alertas em tela cheia — sem isto, alertas reduzidos",
                onFix = { context.startActivity(PresenceHealthChecker.overlayIntent(context)) }
            )

            val hint = health.manufacturerHint
            if (hint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaOrange,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            health.manufacturerIntent?.let { context.startActivity(it) }
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthRow(ok: Boolean, label: String, onFix: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (ok) Modifier else Modifier.clickable { onFix() })
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (ok) ItaGreen else ItaStatusDanger,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ItaTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (ok) "OK" else "Corrigir",
            style = MaterialTheme.typography.labelMedium,
            color = if (ok) ItaGreen else ItaStatusDanger
        )
    }
}
