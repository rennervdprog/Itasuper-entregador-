package com.example.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverLinkStatus
import com.example.data.model.StoreDriverLink
import com.example.ui.components.RouteLine
import com.example.ui.components.RouteStop
import com.example.ui.components.RouteStopState
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate700
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface

@Composable
fun OnboardingLinkScreen(
    viewModel: DriverLinkViewModel,
    onLinkAccepted: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val links by viewModel.links.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val pendingInvites = links.filter { it.status == DriverLinkStatus.PENDING }

    // Feedback visível de aceite/recusa/erro: o ViewModel já grava
    // feedbackMessage, mas nenhuma tela o exibia (aceitar/recusar era
    // silencioso). Mesmo padrão de Snackbar do SupportScreen/OrdersScreen.
    // O ViewModel não tem clearFeedback, então a tela guarda a última
    // mensagem exibida e rearma quando o valor volta a null (toda ação
    // zera a mensagem antes de gravar a próxima).
    val snackbarHostState = remember { SnackbarHostState() }
    var lastShownFeedback by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        if (message == null) {
            lastShownFeedback = null
        } else if (message != lastShownFeedback) {
            lastShownFeedback = message
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(ItaBackground),
            color = ItaBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Top Bar: Driver greeting + Avatar + Logout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ItaOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.name.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Olá, ${profile.name.split(" ").firstOrNull() ?: "Entregador"}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ItaSlate900
                            )
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = ItaSlate500
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.logout(onLogout) },
                        modifier = Modifier.testTag("btn_logout_onboarding")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Sair da conta",
                            tint = ItaSlate500
                        )
                    }
                }

                HorizontalDivider(color = ItaDivider)
                Spacer(modifier = Modifier.height(16.dp))

                if (pendingInvites.isNotEmpty()) {
                    // Estado "Convite recebido"
                    Text(
                        text = "Convites de lojas",
                        style = MaterialTheme.typography.titleMedium,
                        color = ItaSlate900
                    )
                    Text(
                        text = "Você foi convidado para ser motoboy das seguintes lojas:",
                        style = MaterialTheme.typography.bodySmall,
                        color = ItaSlate500,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(pendingInvites, key = { it.id }) { link ->
                            StoreInviteCard(
                                link = link,
                                isProcessing = uiState.processingInviteActions.containsKey(link.id),
                                isAccepting = uiState.processingInviteActions[link.id] == true,
                                onAccept = { viewModel.acceptInvite(link.id, onLinkAccepted) },
                                onReject = { viewModel.rejectInvite(link.id) }
                            )
                        }
                    }
                } else {
                    // Estado "Aguardando vínculo"
                    WaitingForLinkContent(
                        userEmail = profile.email,
                        isChecking = uiState.isCheckingInvites,
                        onCheckInvites = { viewModel.checkNewInvites() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun WaitingForLinkContent(
    userEmail: String,
    isChecking: Boolean,
    onCheckInvites: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(ItaOrangeLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.HourglassEmpty,
                contentDescription = null,
                tint = ItaOrange,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aguardando vínculo com loja",
            style = MaterialTheme.typography.titleMedium,
            color = ItaSlate900
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Peça ao lojista para adicionar seu e-mail como motoboy no painel da loja.",
            style = MaterialTheme.typography.bodySmall,
            color = ItaSlate500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Os 3 passos reais, na linha da rota (marca visual do app).
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ItaSurface),
            border = BorderStroke(1.dp, ItaBorder)
        ) {
            RouteLine(
                stops = listOf(
                    RouteStop(
                        title = "Envie seu e-mail de cadastro",
                        subtitle = userEmail,
                        state = RouteStopState.CURRENT
                    ),
                    RouteStop(
                        title = "O lojista adiciona você no painel",
                        subtitle = "Ele vinculará sua conta à loja parceira ItaSuper",
                        state = RouteStopState.NEXT
                    ),
                    RouteStop(
                        title = "Receba e aceite o convite",
                        subtitle = "Após o envio pelo lojista, você poderá aceitar",
                        state = RouteStopState.NEXT
                    )
                ),
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCheckInvites,
            enabled = !isChecking,
            colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("btn_check_invites")
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verificar convites",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreInviteCard(
    link: StoreDriverLink,
    isProcessing: Boolean,
    isAccepting: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_store_invite_${link.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = BorderStroke(1.dp, ItaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = link.store.tradeName,
                    style = MaterialTheme.typography.titleSmall,
                    color = ItaSlate900,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(ItaOrangeLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Convite de loja",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = ItaOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${link.store.address} - ${link.store.neighborhood}",
                style = MaterialTheme.typography.bodySmall,
                color = ItaSlate500
            )

            if (link.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    link.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(ItaSlate100, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = ItaSlate700
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ItaDivider)
            Spacer(modifier = Modifier.height(12.dp))

            // A decisão: Aceitar verde de 56 dp, Recusar fantasma.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.textButtonColors(contentColor = ItaStatusDanger),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_reject_invite")
                ) {
                    if (isProcessing && !isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ItaStatusDanger,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recusar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaGreenDark),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_accept_invite")
                ) {
                    if (isProcessing && isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Aceitar",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
