package com.example.ui.support

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                style = MaterialTheme.typography.titleLarge,
                                color = ItaTextPrimary
                            )
                            Text(
                                text = "Canal direto para dúvidas e ocorrências de loja",
                                style = MaterialTheme.typography.bodySmall,
                                color = ItaTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { viewModel.openNewTicketDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_new_support_ticket")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Novo Chamado",
                                style = MaterialTheme.typography.labelLarge,
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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ItaSurface),
                        border = BorderStroke(1.dp, ItaBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(ItaOrangeLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HeadsetMic,
                                    contentDescription = null,
                                    tint = ItaOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Atendimento ao motoboy de loja",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ItaTextPrimary
                                )
                                Text(
                                    text = "Atendimento: todos os dias, das 7h às 22h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ItaTextSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Seus chamados (${tickets.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = ItaTextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (tickets.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ItaSurface),
                            border = BorderStroke(1.dp, ItaBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(ItaOrangeLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MarkEmailRead,
                                        contentDescription = null,
                                        tint = ItaOrange,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nenhum chamado ainda",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ItaTextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Quando você abrir um chamado, ele aparece aqui.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ItaTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.openNewTicketDialog() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "Abrir um chamado",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(tickets, key = { it.id }) { ticket ->
                        SupportTicketCard(ticket = ticket)
                    }
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
    // Sem bloco de resposta: SupportTicket não tem campo de resposta e o
    // select de support_tickets não traz coluna de resposta — mostrar a
    // resposta do chamado "Respondido" é trabalho futuro de backend + UI.
    // Nada de texto inventado aqui: só a pílula de status, com os tokens
    // do tema (pendente = laranja suave, nunca âmbar).
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_support_ticket_${ticket.id}"),
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
                    text = ticket.category,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ItaOrange,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            if (ticket.status == "Respondido") ItaGreenLight else ItaStatusPendingBg,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = ticket.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ticket.status == "Respondido") ItaGreenDark else ItaStatusPending
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.titleSmall,
                color = ItaTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ItaTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ItaDivider)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Criado em ${formatTicketDateTime(ticket.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = ItaTextTertiary
            )
        }
    }
}

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ItaSurface,
            border = BorderStroke(1.dp, ItaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_new_support_ticket")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Novo chamado",
                        style = MaterialTheme.typography.titleMedium,
                        color = ItaTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Fechar",
                            tint = ItaTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category selector: as 5 categorias reais como chips
                // selecionáveis, em vez do dropdown sem scroll.
                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    color = ItaTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                categories.forEachIndexed { index, cat ->
                    SupportCategoryChip(
                        label = cat,
                        selected = category == cat,
                        index = index,
                        onClick = { onCategoryChange(cat) }
                    )
                    if (index < categories.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subject
                Text(
                    text = "Assunto",
                    style = MaterialTheme.typography.labelMedium,
                    color = ItaTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = onSubjectChange,
                    placeholder = { Text("Ex: Dúvida no pedido #4920") },
                    colors = itaOutlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = "Descrição detalhada",
                    style = MaterialTheme.typography.labelMedium,
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancelar", color = ItaTextSecondary)
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                text = "Enviar",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportCategoryChip(
    label: String,
    selected: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) ItaOrangeLight else ItaSurface,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) ItaOrange else ItaBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chip_support_category_$index"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) ItaOrange else ItaTextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = ItaOrange,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Data do chamado no padrão relativo do Histórico: "Hoje, 15:30",
 * "Ontem, 18:20" ou "29/10, 13:45" (com ano reduzido em outro ano).
 * O repositório já entrega "dd/MM/yyyy HH:mm"; aceita também o ISO cru
 * como fallback. Sem java.time (minSdk 24): SimpleDateFormat + Calendar,
 * e qualquer falha devolve o valor original em vez de esconder a data.
 */
private fun formatTicketDateTime(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return raw
    if (trimmed.startsWith("Hoje") || trimmed.startsWith("Ontem")) return trimmed
    return try {
        val parsed = try {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                .apply { isLenient = false }
                .parse(trimmed)
        } catch (_: Throwable) {
            null
        } ?: try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .apply { isLenient = false }
                .parse(trimmed)
        } catch (_: Throwable) {
            null
        } ?: return raw
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val date = java.util.Calendar.getInstance().apply {
            time = parsed
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffDays = ((today.timeInMillis - date.timeInMillis) / 86_400_000L).toInt()
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale("pt", "BR")).format(parsed)
        val dayLabel = java.text.SimpleDateFormat("dd/MM", java.util.Locale("pt", "BR")).format(parsed)
        val dayLabelWithYear =
            java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale("pt", "BR")).format(parsed)
        val label = when {
            diffDays <= 0 -> "Hoje"
            diffDays == 1 -> "Ontem"
            date.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) -> dayLabel
            else -> dayLabelWithYear
        }
        "$label, $time"
    } catch (_: Throwable) {
        raw
    }
}
