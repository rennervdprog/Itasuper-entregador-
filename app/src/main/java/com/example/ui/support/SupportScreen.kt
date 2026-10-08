package com.example.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SupportTicket
import com.example.ui.components.itaOutlinedFieldColors
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaStatusPending
import com.example.ui.theme.ItaStatusPendingBg
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary

@Composable
fun SupportScreen(
    viewModel: SupportViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tickets by viewModel.tickets.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(ItaBackground),
            color = ItaBackground
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Suporte ao Entregador",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ItaTextPrimary
                            )
                            Text(
                                text = "Canal direto para dúvidas e ocorrências de loja",
                                fontSize = 13.sp,
                                color = ItaTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { viewModel.openNewTicketDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_new_support_ticket")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Novo Chamado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Help banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ItaSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(ItaOrangeLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HeadsetMic,
                                    contentDescription = null,
                                    tint = ItaOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Atendimento ao motoboy de loja",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ItaTextPrimary
                                )
                                Text(
                                    text = "Atendimento: todos os dias, das 7h às 22h",
                                    fontSize = 12.sp,
                                    color = ItaTextSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Seus chamados (${tickets.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(tickets, key = { it.id }) { ticket ->
                    SupportTicketCard(ticket = ticket)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // New Ticket Dialog
        if (uiState.isNewTicketDialogOpen) {
            NewSupportTicketDialog(
                subject = uiState.subject,
                category = uiState.category,
                description = uiState.description,
                isLoading = uiState.isSubmitting,
                onSubjectChange = { viewModel.onSubjectChange(it) },
                onCategoryChange = { viewModel.onCategoryChange(it) },
                onDescriptionChange = { viewModel.onDescriptionChange(it) },
                onSubmit = { viewModel.submitTicket() },
                onDismiss = { viewModel.closeNewTicketDialog() }
            )
        }
    }
}

@Composable
private fun SupportTicketCard(
    ticket: SupportTicket
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_support_ticket_${ticket.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.category,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ItaOrange
                )

                Box(
                    modifier = Modifier
                        .background(
                            if (ticket.status == "Respondido") ItaGreenLight else ItaStatusPendingBg,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ticket.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ticket.status == "Respondido") ItaGreenDark else ItaStatusPending
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = ticket.subject,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ItaTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = ticket.description,
                fontSize = 13.sp,
                color = ItaTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ItaDivider)
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Criado em ${ticket.createdAt}",
                fontSize = 11.sp,
                color = ItaTextTertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSupportTicketDialog(
    subject: String,
    category: String,
    description: String,
    isLoading: Boolean,
    onSubjectChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf(
        "Dúvida sobre taxa de entrega",
        "Problema de endereço ou cliente",
        "Vínculo com nova loja",
        "Cancelamento ou recusa de pedido",
        "Outros assuntos"
    )
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ItaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_new_support_ticket")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Novo chamado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = ItaTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category selector
                Text(
                    text = "Categoria",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ItaTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = itaOutlinedFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    onCategoryChange(cat)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subject
                Text(
                    text = "Assunto",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ItaTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = onSubjectChange,
                    placeholder = { Text("Ex: Dúvida no pedido #4920") },
                    colors = itaOutlinedFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = "Descrição detalhada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ItaTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    placeholder = { Text("Explique o que aconteceu...") },
                    minLines = 3,
                    maxLines = 5,
                    colors = itaOutlinedFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = ItaTextSecondary)
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Enviar", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
