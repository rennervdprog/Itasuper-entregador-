package com.example.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverLinkStatus
import com.example.data.model.StoreDriverLink
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate200
import com.example.ui.theme.ItaSlate400
import com.example.ui.theme.ItaSlate50
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate600
import com.example.ui.theme.ItaSlate700
import com.example.ui.theme.ItaSlate800
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

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

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(ItaBackground),
        color = ItaBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Olá, ${profile.name.split(" ").firstOrNull() ?: "Entregador"}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaSlate900
                        )
                        Text(
                            text = profile.email,
                            fontSize = 12.sp,
                            color = ItaSlate500
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.logout(onLogout) },
                    modifier = Modifier.testTag("btn_logout_onboarding")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
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
                    text = "Convites de Lojas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaSlate900
                )
                Text(
                    text = "Você foi convidado para ser motoboy das seguintes lojas:",
                    fontSize = 13.sp,
                    color = ItaSlate500,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(pendingInvites, key = { it.id }) { link ->
                        StoreInviteCard(
                            link = link,
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

            // Quick State Tester Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.simulateResetToAguardando() },
                        modifier = Modifier.testTag("btn_simulate_waiting")
                    ) {
                        Text("Modo: Sem Vínculo", fontSize = 11.sp, color = ItaSlate500)
                    }
                    TextButton(
                        onClick = { viewModel.simulateAddInvite() },
                        modifier = Modifier.testTag("btn_simulate_invite")
                    ) {
                        Text("Modo: Receber Convite", fontSize = 11.sp, color = ItaOrange)
                    }
                }
            }
        }
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
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = ItaOrange,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aguardando Vínculo com Loja",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ItaSlate900
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Peça ao dono da loja para adicionar seu e-mail como motoboy no painel dele.",
            fontSize = 13.sp,
            color = ItaSlate500,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3 Visual Steps
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ItaSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StepItem(
                    stepNumber = "1",
                    title = "Envie seu e-mail de cadastro",
                    description = userEmail,
                    icon = Icons.Default.MarkEmailRead
                )
                Spacer(modifier = Modifier.height(14.dp))
                StepItem(
                    stepNumber = "2",
                    title = "O lojista adiciona você no painel",
                    description = "Ele vinculará sua conta à loja parceira ItaSuper",
                    icon = Icons.Default.SupervisedUserCircle
                )
                Spacer(modifier = Modifier.height(14.dp))
                StepItem(
                    stepNumber = "3",
                    title = "Receba e aceite o convite",
                    description = "Após o envio pelo lojista, você poderá aceitar",
                    icon = Icons.Default.Storefront
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onCheckInvites,
            enabled = !isChecking,
            colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verificar convites",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(ItaOrangeLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = ItaOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ItaSlate900
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = ItaSlate500
            )
        }
    }
}

@Composable
private fun StoreInviteCard(
    link: StoreDriverLink,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_store_invite_${link.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = link.store.tradeName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaSlate900
                )
                Box(
                    modifier = Modifier
                        .background(ItaOrangeLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CONVITE DE LOJA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = ItaOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${link.store.address} - ${link.store.neighborhood}",
                fontSize = 12.sp,
                color = ItaSlate500
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Indicators / Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                link.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(ItaSlate100, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = ItaSlate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = ItaDivider)
            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Recusar & Aceitar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ItaStatusDanger),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_reject_invite")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recusar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaGreenDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_accept_invite")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aceitar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
