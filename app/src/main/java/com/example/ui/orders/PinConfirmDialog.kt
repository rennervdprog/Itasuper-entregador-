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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DeliveryOrder
import com.example.ui.components.PinInputBoxes
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate200
import com.example.ui.theme.ItaSlate300
import com.example.ui.theme.ItaSlate400
import com.example.ui.theme.ItaSlate50
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate600
import com.example.ui.theme.ItaSlate700
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

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
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ItaOrangeLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = ItaOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Confirmar Entrega",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ItaSlate900
                            )
                            Text(
                                text = "Pedido ${order.shortCode}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = ItaSlate500
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_pin_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = ItaSlate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ItaDivider)
                Spacer(modifier = Modifier.height(14.dp))

                // Customer info card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ItaSlate50, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = order.customerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaSlate900
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = order.fullAddress,
                        fontSize = 12.sp,
                        color = ItaSlate500
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Peça o código PIN de 4 dígitos ao cliente para validar e finalizar a entrega.",
                    fontSize = 13.sp,
                    color = ItaSlate600,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // PIN Boxes
                PinInputBoxes(
                    pin = enteredPin,
                    onPinChange = onPinChange,
                    expectedPin = order.customerPin,
                    onConfirm = onConfirm
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = ItaStatusDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("text_pin_error")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Cancelar", color = ItaSlate600, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = enteredPin.length == 4 && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ItaOrange,
                            disabledContainerColor = ItaSlate300
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
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
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
