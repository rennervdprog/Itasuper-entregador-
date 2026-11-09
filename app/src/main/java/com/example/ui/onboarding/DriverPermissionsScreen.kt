package com.example.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

/** Permissões necessárias para alertas de entrega e navegação do motoboy. */
data class DriverPermissionStatus(
    val notificationsGranted: Boolean,
    val locationGranted: Boolean,
    val overlayGranted: Boolean,
    // Etapa 2 da localização ("Permitir o tempo todo") e a isenção de
    // bateria: são o que mantém o motoboy visível no mapa da loja enquanto
    // ele estiver Online. Recomendadas, mas NÃO travam a entrada — sem elas
    // o app funciona em modo limitado e a Saúde do rastreio avisa.
    val backgroundLocationGranted: Boolean = false,
    val batteryUnrestricted: Boolean = false
) {
    // Fase 4.1: a sobreposição NÃO trava mais a entrada. Sem ela, o alerta
    // de nova entrega chega pela notificação em tela cheia (Fase 2) — modo
    // reduzido, avisado no cartão "Saúde do rastreio" do Perfil até o
    // motoboy conceder. Obrigatórias: notificações + localização.
    val allGranted: Boolean
        get() = notificationsGranted && locationGranted
}

private data class PermissionStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val grantedLabel: String,
    val actionLabel: String,
    val hint: String?,
    val granted: Boolean,
    val mandatory: Boolean,
    val onRequest: () -> Unit
)

/**
 * Fluxo de permissões EM ETAPAS (redesign aprovado pelo Renner em
 * 2026-11-09): uma permissão por tela, com progresso, em vez da lista
 * única de cartões. Nada avança sozinho — cada passo só anda no toque
 * (Continuar/Pular), e conceder no meio do passo só troca o botão para
 * "Continuar". Obrigatórias (notificações, localização) não têm Pular;
 * as recomendadas têm, e o que ficar pulado continua cobrado no cartão
 * "Saúde do rastreio" do Perfil.
 */
@Composable
fun DriverPermissionsScreen(
    status: DriverPermissionStatus,
    onRequestNotifications: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestOverlay: () -> Unit,
    onContinue: () -> Unit,
    onRequestBackgroundLocation: () -> Unit = {},
    onRequestBattery: () -> Unit = {}
) {
    val steps = listOf(
        PermissionStep(
            icon = Icons.Filled.NotificationsActive,
            title = "Receba o aviso de nova entrega",
            description = "Sem as notificações, você não fica sabendo quando cai um pedido — e perde a entrega pra outro motoboy.",
            grantedLabel = "Notificações ativadas",
            actionLabel = "Permitir notificações",
            hint = "O Android vai pedir a confirmação. Esta é obrigatória para entrar no app.",
            granted = status.notificationsGranted,
            mandatory = true,
            onRequest = onRequestNotifications
        ),
        PermissionStep(
            icon = Icons.Filled.LocationOn,
            title = "Sua posição durante o turno",
            description = "A localização calcula as distâncias, monta sua rota e mostra pra loja onde você está enquanto estiver online.",
            grantedLabel = "Localização ativada",
            actionLabel = "Permitir localização",
            hint = "Na tela do Android, escolha “Permitir durante o uso do app”. Esta é obrigatória para entrar no app.",
            granted = status.locationGranted,
            mandatory = true,
            onRequest = onRequestLocation
        ),
        PermissionStep(
            icon = Icons.Filled.Layers,
            title = "Alerta em tela cheia",
            description = "A chamada da nova entrega aparece por cima de qualquer app, com som e vibração. Sem isto, o aviso chega só pela notificação.",
            grantedLabel = "Sobreposição ativada",
            actionLabel = "Permitir sobreposição",
            hint = null,
            granted = status.overlayGranted,
            mandatory = false,
            onRequest = onRequestOverlay
        ),
        PermissionStep(
            icon = Icons.Filled.Schedule,
            title = "Visível mesmo com o app fechado",
            description = "Enquanto você estiver ONLINE, a loja continua vendo sua posição no mapa mesmo com o app em segundo plano. Ficou offline, para na hora.",
            grantedLabel = "Localização o tempo todo ativada",
            actionLabel = "Permitir o tempo todo",
            hint = null,
            granted = status.backgroundLocationGranted,
            mandatory = false,
            onRequest = onRequestBackgroundLocation
        ),
        PermissionStep(
            icon = Icons.Filled.BatteryChargingFull,
            title = "O Android não “dorme” o app",
            description = "Sem a liberação da bateria, o sistema pode congelar o ItaSuper no meio do turno — sua posição some do mapa e o aviso atrasa.",
            grantedLabel = "Bateria liberada",
            actionLabel = "Liberar bateria",
            hint = null,
            granted = status.batteryUnrestricted,
            mandatory = false,
            onRequest = onRequestBattery
        )
    )

    var stepIndex by remember { mutableStateOf(0) }
    val step = steps[stepIndex]
    val isLast = stepIndex == steps.lastIndex
    val advance: () -> Unit = {
        if (isLast) onContinue() else stepIndex++
        Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stepIndex > 0) {
                IconButton(onClick = { stepIndex-- }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = ItaTextSecondary
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "PASSO ${stepIndex + 1} DE ${steps.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i <= stepIndex) ItaOrange else Color(0xFFE2E8F0))
                )
                if (i < steps.lastIndex) Spacer(modifier = Modifier.width(5.dp))
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 42.dp)
                .size(104.dp)
                .clip(CircleShape)
                .background(
                    if (step.granted) ItaGreen.copy(alpha = 0.12f)
                    else ItaOrange.copy(alpha = 0.12f)
                )
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (step.granted) Icons.Filled.Check else step.icon,
                contentDescription = null,
                tint = if (step.granted) ItaGreen else ItaOrange,
                modifier = Modifier.size(50.dp)
            )
        }

        if (!step.mandatory) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(ItaOrange)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "RECOMENDADO",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Text(
            text = step.title,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ItaTextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 27.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )

        if (step.granted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ItaGreen,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = step.grantedLabel,
                    color = ItaGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Text(
            text = step.description,
            fontSize = 15.sp,
            color = ItaTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        if (step.granted) {
            Button(
                onClick = advance,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isLast) "Começar a usar o app" else "Continuar",
                    modifier = Modifier.padding(vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            Button(
                onClick = step.onRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = step.actionLabel,
                    modifier = Modifier.padding(vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            if (step.mandatory) {
                if (step.hint != null) {
                    Text(
                        text = step.hint,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }
            } else {
                TextButton(
                    onClick = advance,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pular por agora",
                        color = ItaTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}
