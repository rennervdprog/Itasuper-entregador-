package com.example.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryOrder
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDisabledAction
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate200
import com.example.ui.theme.ItaSlate300
import com.example.ui.theme.ItaSlate400
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate600
import com.example.ui.theme.ItaSlate700
import com.example.ui.theme.ItaSlate800
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary

@Composable
fun AvailableOrdersSection(
    orders: List<DeliveryOrder>,
    hasActiveRoute: Boolean,
    onAcceptOrder: (String) -> Unit,
    onAcceptAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with Accept All if applicable
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pedidos disponíveis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(if (hasActiveRoute) Color(0xFFE2E8F0) else ItaOrangeLight, RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = orders.size.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasActiveRoute) Color(0xFF64748B) else ItaOrange
                    )
                }
            }

            if (!hasActiveRoute && orders.size >= 2) {
                Button(
                    onClick = onAcceptAll,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_accept_all_available")
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Aceitar rota (${orders.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (hasActiveRoute) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Finalize a rota atual antes de aceitar novas entregas.",
                    fontSize = 12.sp,
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            orders.forEach { order ->
                AvailableOrderCard(
                    order = order,
                    isDisabled = hasActiveRoute,
                    onAccept = { onAcceptOrder(order.id) }
                )
            }
        }
    }
}

@Composable
fun AvailableOrderCard(
    order: DeliveryOrder,
    isDisabled: Boolean,
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDisabled) 0.6f else 1.0f)
            .testTag("card_available_order_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Store Name + Short Code + Earnings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.store.tradeName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = ItaSlate500
                    )
                    Text(
                        text = order.shortCode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaSlate900
                    )
                }

            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ItaDivider)
            Spacer(modifier = Modifier.height(10.dp))

            // Neighborhood (Primary prominent destination) + Short address
            Text(
                text = order.addressNeighborhood,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ItaSlate900
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = order.shortAddress + (order.addressComplement?.let { " ($it)" } ?: ""),
                fontSize = 13.sp,
                color = ItaSlate500
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details pill row: Items count, distance, payment method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Items
                Row(
                    modifier = Modifier
                        .background(ItaSlate100, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = ItaSlate600,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${order.items.sumOf { it.quantity }} itens",
                        fontSize = 11.sp,
                        color = ItaSlate700,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Payment info
                Row(
                    modifier = Modifier
                        .background(
                            if (order.payment.isPaidOnline) ItaGreenLight else Color(0xFFFEF3C7),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            order.payment.method.contains("PIX", true) -> Icons.Default.QrCode2
                            order.payment.method.contains("Dinheiro", true) -> Icons.Default.Payments
                            else -> Icons.Default.CreditCard
                        },
                        contentDescription = null,
                        tint = if (order.payment.isPaidOnline) ItaGreenDark else Color(0xFFB45309),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = order.payment.method,
                        fontSize = 11.sp,
                        color = if (order.payment.isPaidOnline) ItaGreenDark else Color(0xFF92400E),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Distância calculada somente com coordenadas reais salvas no pedido.
                Text(
                    text = if (order.estimatedDistanceKm > 0.0) {
                        "• ~${String.format("%.1f", order.estimatedDistanceKm)} km"
                    } else {
                        "• Localização não informada"
                    },
                    fontSize = 11.sp,
                    color = ItaSlate400
                )
            }

            // Troco note if payment is cash
            order.payment.displayChangeText?.let { changeText ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Troco: $changeText",
                    fontSize = 11.sp,
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // O pedido permanece disponível até ser aceito por este motoboy ou por outro elegível da mesma loja.
            Button(
                onClick = onAccept,
                enabled = !isDisabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ItaOrange,
                    disabledContainerColor = ItaDisabledAction,
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("btn_accept_order_${order.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDisabled) "Finalize a rota atual" else "Aceitar entrega",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
