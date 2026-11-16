package com.example.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate200
import com.example.ui.theme.ItaSlate400
import com.example.ui.theme.ItaStatusPending
import com.example.ui.theme.ItaSurface
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
 *
 * Ao fim dos 5 passos, um resumo final mostra o estado REAL de cada item
 * (o `status` que a MainActivity recalcula no sistema a cada retorno) —
 * incluindo notificações e sobreposição, que o `PresenceHealth.allOk` não
 * cobre. O que foi pulado aparece como pulado, com o caminho para ativar
 * depois, no perfil.
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
    var skippedSteps by remember { mutableStateOf(setOf<Int>()) }
    val showSummary = stepIndex == steps.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ItaBackground)
            .padding(horizontal = 24.dp, vertical = 16.dp)
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
                text = if (showSummary) "RESUMO" else "PASSO ${stepIndex + 1} DE ${steps.size}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = ItaSlate400
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i <= stepIndex) ItaOrange else ItaSlate200)
                )
                if (i < steps.lastIndex) Spacer(modifier = Modifier.width(4.dp))
            }
        }

        if (showSummary) {
            PermissionSummaryContent(
                steps = steps,
                skippedSteps = skippedSteps,
                onRevisitStep = { index ->
                    skippedSteps = skippedSteps - index
                    stepIndex = index
                },
                onContinue = onContinue,
                modifier = Modifier.weight(1f)
            )
        } else {
            val step = steps[stepIndex]
            val isLast = stepIndex == steps.lastIndex
            val advance: () -> Unit = { stepIndex++ }

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .padding(top = 32.dp)
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
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "RECOMENDADO",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp)
                        )
                    }
                }

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ItaTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step.grantedLabel,
                            color = ItaGreen,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ItaTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (step.granted) {
                    Button(
                        onClick = advance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = if (isLast) "Ver resumo" else "Continuar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Button(
                        onClick = step.onRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = step.actionLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (step.mandatory) {
                        if (step.hint != null) {
                            Text(
                                text = step.hint,
                                color = ItaSlate400,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                skippedSteps = skippedSteps + stepIndex
                                advance()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pular por agora",
                                color = ItaTextSecondary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PermissionSummaryContent(
    steps: List<PermissionStep>,
    skippedSteps: Set<Int>,
    onRevisitStep: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(ItaGreen.copy(alpha = 0.12f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ItaGreen,
                    modifier = Modifier.size(46.dp)
                )
            }

            Text(
                text = "Tudo pronto para rodar.",
                style = MaterialTheme.typography.headlineSmall,
                color = ItaTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
            Text(
                text = "Este é o estado real das 5 permissões — incluindo notificações e alerta em tela cheia.",
                style = MaterialTheme.typography.bodyMedium,
                color = ItaTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = BorderStroke(1.dp, ItaBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    steps.forEachIndexed { index, step ->
                        val skipped = index in skippedSteps
                        PermissionSummaryRow(
                            step = step,
                            skipped = skipped,
                            onClick = if (step.granted) null else ({ onRevisitStep(index) })
                        )
                        if (index < steps.lastIndex) {
                            HorizontalDivider(color = ItaBorder)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = "Começar a usar o app",
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionSummaryRow(
    step: PermissionStep,
    skipped: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(onClick?.let { handler -> Modifier.clickable { handler() } } ?: Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (step.granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (step.granted) ItaGreen else ItaSlate400,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.grantedLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ItaTextPrimary
            )
            if (!step.granted) {
                Text(
                    text = if (skipped) {
                        "Pulado — você pode ativar depois, no seu perfil."
                    } else {
                        "Não ativado — você pode ativar depois, no seu perfil."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaTextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when {
                step.granted -> "Ativada"
                skipped -> "Pulada"
                else -> "Pendente"
            },
            style = MaterialTheme.typography.labelMedium,
            color = when {
                step.granted -> ItaGreenDark
                skipped -> ItaSlate400
                else -> ItaStatusPending
            }
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Voltar a este passo",
                tint = ItaSlate400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
