package com.example.ui.orders

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DeliveryOrder
import com.example.ui.components.PinInputBoxes
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDisabledAction
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaStatusPending
import com.example.ui.theme.ItaStatusPendingBg
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary

@Composable
fun PinConfirmDialog(
    order: DeliveryOrder,
    enteredPin: String,
    errorMessage: String?,
    isLoading: Boolean,
    onPinChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPinHelp by remember { mutableStateOf(false) }
    // Contador de tentativas: visível SOMENTE quando a mensagem de erro
    // informar o número — mesma interpretação do ViewModel
    // (friendlyPinError extrai "N tentativa(s)" da mensagem do servidor).
    // Sem número na mensagem, nenhum contador é inventado.
    val remainingAttempts = remember(errorMessage) {
        errorMessage?.let {
            Regex("(\\d+)\\s+tentativa", RegexOption.IGNORE_CASE)
                .find(it)
                ?.groupValues
                ?.getOrNull(1)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ItaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_pin_confirm")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(ItaGreen, RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "VOCÊ CHEGOU · ${order.shortCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_pin_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Fechar",
                            tint = ItaSlate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Código do cliente",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ItaTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Peça os 4 números para ")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = ItaTextPrimary
                            )
                        ) {
                            append(order.customerName)
                        }
                        append(".\nConfirma sozinho quando completar.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ItaTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = order.fullAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaTextTertiary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Caixas gigantes + teclado próprio; o 4º dígito confirma
                // sozinho pelo mesmo callback de confirmação.
                PinInputBoxes(
                    pin = enteredPin,
                    onPinChange = onPinChange,
                    onConfirm = onConfirm
                )

                if (remainingAttempts != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(ItaStatusPendingBg, RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (remainingAttempts == "1") {
                                "Resta 1 tentativa"
                            } else {
                                "Restam $remainingAttempts tentativas"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = ItaStatusPending
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = ItaStatusDanger,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("text_pin_error")
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Digite no teclado abaixo — sem toque extra.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ItaTextTertiary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            style = MaterialTheme.typography.labelLarge,
                            color = ItaTextSecondary
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = enteredPin.length == 4 && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ItaOrange,
                            disabledContainerColor = ItaDisabledAction,
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("btn_submit_pin")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Validar PIN",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { showPinHelp = true }) {
                    Text(
                        text = "Cliente não sabe o código?",
                        style = MaterialTheme.typography.labelLarge,
                        color = ItaOrange
                    )
                }
            }
        }
    }

    // Ajuda neutra e factual apenas: nenhuma contingência ou regra nova é
    // inventada aqui (Fase 5, decisão de produto pendente).
    if (showPinHelp) {
        AlertDialog(
            onDismissRequest = { showPinHelp = false },
            title = {
                Text(
                    text = "Código do cliente",
                    style = MaterialTheme.typography.titleMedium,
                    color = ItaTextPrimary
                )
            },
            text = {
                Text(
                    text = "O código de 4 dígitos está com o cliente, no pedido dele.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ItaTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showPinHelp = false }) {
                    Text(
                        text = "Entendi",
                        style = MaterialTheme.typography.labelLarge,
                        color = ItaOrange
                    )
                }
            }
        )
    }
}
