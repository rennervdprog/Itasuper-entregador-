package com.example.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ItaOrange

/** Permissões necessárias para alertas de entrega e navegação do motoboy. */
data class DriverPermissionStatus(
    val notificationsGranted: Boolean,
    val locationGranted: Boolean,
    val overlayGranted: Boolean
) {
    // A sobreposição melhora o alerta visual, mas sua disponibilidade varia por
    // fabricante. Ela não pode impedir o motoboy de entrar e trabalhar.
    val allGranted: Boolean
        get() = notificationsGranted && locationGranted
}

@Composable
fun DriverPermissionsScreen(
    status: DriverPermissionStatus,
    onRequestNotifications: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestOverlay: () -> Unit,
    onTestOverlay: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 22.dp, vertical = 38.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = ItaOrange,
            modifier = Modifier.size(58.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ative seus alertas de entrega",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Versão de teste 2.0.7 — contraste revisado",
            fontSize = 12.sp,
            color = ItaOrange,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Receba novos pedidos e navegue com segurança durante a rota.",
            fontSize = 15.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        PermissionCard(
            icon = Icons.Default.NotificationsActive,
            title = "Notificações de pedidos",
            description = "Avisa quando uma nova entrega estiver disponível.",
            granted = status.notificationsGranted,
            buttonLabel = "Permitir notificações",
            onRequest = onRequestNotifications
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionCard(
            icon = Icons.Default.LocationOn,
            title = "Localização",
            description = "Calcula distância e permite acompanhar a rota ativa.",
            granted = status.locationGranted,
            buttonLabel = "Permitir localização",
            onRequest = onRequestLocation
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionCard(
            icon = Icons.Default.OpenInNew,
            title = "Exibir sobre outros apps",
            description = "Mostra a chamada visual de nova entrega mesmo quando o app estiver fora da tela.",
            granted = status.overlayGranted,
            buttonLabel = "Permitir sobreposição",
            onRequest = onRequestOverlay,
            opensSettings = true
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onTestOverlay,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ItaOrange)
        ) {
            Text("Testar painel visual", color = ItaOrange, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = status.allGranted,
            colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Continuar para entrar",
                modifier = Modifier.padding(vertical = 5.dp),
                fontWeight = FontWeight.Bold
            )
        }
        if (!status.allGranted) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Permita notificações e localização para entrar. A sobreposição pode ser ativada agora ou mais tarde para o alerta visual.",
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    buttonLabel: String,
    onRequest: () -> Unit,
    opensSettings: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) Color(0xFF16A34A) else ItaOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = if (granted) "Ativada" else "Pendente",
                        fontSize = 13.sp,
                        color = if (granted) Color(0xFF15803D) else Color(0xFFB45309)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, color = Color(0xFF64748B), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            if (granted) {
                Text("Permissão concedida", color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            } else {
                OutlinedButton(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ItaOrange)
                ) {
                    Text(buttonLabel, color = ItaOrange, fontWeight = FontWeight.SemiBold)
                    if (opensSettings) {
                        Spacer(modifier = Modifier.size(5.dp))
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp), tint = ItaOrange)
                    }
                }
            }
        }
    }
}
