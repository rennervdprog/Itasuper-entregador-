package com.example.ui.orders

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.DeliveryOrder
import com.example.data.model.PaymentSummary
import com.example.data.model.formatBrl
import com.example.data.model.isMachinePixMethod
import com.example.data.model.isOnlinePaidMethod
import com.example.ui.components.RouteLine
import com.example.ui.components.RouteStop
import com.example.ui.components.RouteStopState
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDisabledAction
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeDark
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate300
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate600
import com.example.ui.theme.ItaSlate700
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaStatusPending
import com.example.ui.theme.ItaStatusPendingBg
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextSecondary
import java.util.Locale

@Composable
fun AvailableOrdersSection(
    orders: List<DeliveryOrder>,
    hasActiveRoute: Boolean,
    onAcceptOrder: (String) -> Unit,
    onAcceptAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dispensa LOCAL: só esconde o pedido desta lista. Nenhuma chamada de
    // backend — o registro da recusa na loja é trabalho futuro. O conjunto
    // é limpo quando a lista muda (nova atualização do servidor).
    var dismissedIds by remember(orders) { mutableStateOf(emptySet<String>()) }
    val visibleOrders = remember(orders, dismissedIds) {
        orders.filter { it.id !in dismissedIds }
    }
    // Prévia da rota: "Aceitar rota (N)" mostra a sequência de paradas antes
    // do aceite. Também reinicia quando a lista muda.
    var showRoutePreview by remember(orders) { mutableStateOf(false) }

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
                    style = MaterialTheme.typography.titleMedium,
                    color = ItaSlate900
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(if (hasActiveRoute) ItaSlate100 else ItaOrangeLight, RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = visibleOrders.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasActiveRoute) ItaSlate600 else ItaOrangeDark
                    )
                }
            }

            if (!hasActiveRoute && visibleOrders.size >= 2 && !showRoutePreview) {
                Button(
                    onClick = { showRoutePreview = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_accept_all_available")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aceitar rota (${visibleOrders.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        if (hasActiveRoute) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ItaStatusPendingBg, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = ItaStatusPending,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Finalize a rota atual antes de aceitar novas entregas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaStatusPending,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showRoutePreview && !hasActiveRoute && visibleOrders.size >= 2) {
            Spacer(modifier = Modifier.height(12.dp))
            RoutePreviewCard(
                orders = visibleOrders,
                hasDismissedOrders = dismissedIds.isNotEmpty(),
                onConfirm = onAcceptAll,
                onBack = { showRoutePreview = false }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (visibleOrders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = BorderStroke(1.dp, ItaBorder)
            ) {
                Text(
                    text = "Você dispensou os pedidos desta lista. Eles voltam na próxima atualização.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ItaTextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleOrders.forEach { order ->
                    AvailableOrderCard(
                        order = order,
                        isDisabled = hasActiveRoute,
                        onAccept = { onAcceptOrder(order.id) },
                        onDismiss = { dismissedIds = dismissedIds + order.id }
                    )
                }
            }
        }
    }
}

/**
 * Sequência de paradas antes do aceite em lote, montada só com os dados de
 * rota já presentes no modelo (`stopOrder`, `routeLegKm`, loja e endereço).
 */
@Composable
private fun RoutePreviewCard(
    orders: List<DeliveryOrder>,
    hasDismissedOrders: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val stops = remember(orders) {
        orders.sortedBy { it.stopOrder }.mapIndexed { index, order ->
            RouteStop(
                title = order.shortAddress,
                subtitle = buildString {
                    append(cleanStoreName(order.store.tradeName))
                    if (order.addressNeighborhood.isNotBlank()) {
                        append(" · ").append(order.addressNeighborhood)
                    }
                    order.routeLegKm?.let { append(" · ").append(formatKm(it)) }
                },
                state = if (index == 0) RouteStopState.CURRENT else RouteStopState.NEXT
            )
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = BorderStroke(1.dp, ItaBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ordem da rota",
                style = MaterialTheme.typography.titleSmall,
                color = ItaSlate900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Confira as paradas antes de aceitar.",
                style = MaterialTheme.typography.bodySmall,
                color = ItaTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            RouteLine(stops = stops)
            if (hasDismissedOrders) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Atenção: aceitar a rota inclui também os pedidos dispensados desta lista.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ItaStatusPending
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_confirm_accept_route")
                ) {
                    Text(
                        text = "Aceitar rota (${orders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ItaBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ItaTextSecondary),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(
                        text = "Voltar",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
fun AvailableOrderCard(
    order: DeliveryOrder,
    isDisabled: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_available_order_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled) ItaSlate100 else ItaSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isDisabled) ItaSlate300 else ItaBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Loja + código curto do pedido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cleanStoreName(order.store.tradeName),
                    style = MaterialTheme.typography.titleSmall,
                    color = ItaSlate900,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.shortCode,
                    style = MaterialTheme.typography.labelMedium,
                    color = ItaSlate500
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Endereço gigante (23sp, exigido pelo preview — o papel
            // titleLarge cobre 22sp; o 1sp extra vem do preview).
            Text(
                text = order.shortAddress + (order.addressComplement?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 23.sp,
                color = ItaSlate900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = orderMetaLine(order),
                style = MaterialTheme.typography.bodyMedium,
                color = ItaSlate500
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pílulas de pagamento: o que cobrar (ou o já pago) + troco/método.
            PaymentPills(payment = order.payment)

            Spacer(modifier = Modifier.height(12.dp))

            // Resumo rápido dos produtos, sem exigir a expansão do pedido.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    tint = ItaSlate600,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Itens: ${order.driverItemsSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaSlate700,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Decisão: aceitar (mesmo callback de hoje) ou dispensar (local).
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = !isDisabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ItaOrange,
                        disabledContainerColor = ItaDisabledAction,
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_accept_order_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDisabled) "Finalize a rota atual" else "Aceitar",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ItaBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ItaTextSecondary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("btn_dismiss_order_${order.id}")
                ) {
                    Text(
                        text = "Dispensar",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentPills(payment: PaymentSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (payment.isPaidOnline) {
            Pill(
                text = if (isOnlinePaidMethod(payment.method) || payment.method.contains("pix", ignoreCase = true)) {
                    "Pix · já pago"
                } else {
                    "Já pago"
                },
                backgroundColor = ItaGreenLight,
                textColor = ItaGreenDark
            )
            Pill(
                text = "Sem troco",
                backgroundColor = ItaSlate100,
                textColor = ItaSlate600
            )
        } else {
            Pill(
                text = "Cobrar ${formatBrl(payment.amount)}",
                backgroundColor = ItaOrangeLight,
                textColor = ItaOrangeDark
            )
            val changeFor = payment.changeFor
            if (changeFor != null && changeFor > payment.amount) {
                Pill(
                    text = "Troco p/ ${formatBrl(changeFor)}",
                    backgroundColor = ItaSlate100,
                    textColor = ItaSlate600
                )
            } else {
                Pill(
                    text = paymentMethodLabel(payment.method),
                    backgroundColor = ItaSlate100,
                    textColor = ItaSlate600
                )
            }
        }
    }
}

@Composable
private fun Pill(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

/** Linha em segundo plano: bairro · distância · itens. */
private fun orderMetaLine(order: DeliveryOrder): String {
    val parts = mutableListOf<String>()
    if (order.addressNeighborhood.isNotBlank()) {
        parts.add(order.addressNeighborhood)
    }
    if (order.estimatedDistanceKm > 0.0) {
        parts.add(formatKm(order.estimatedDistanceKm))
    } else {
        parts.add("distância não informada")
    }
    val itemCount = order.items.sumOf { it.quantity }
    parts.add(if (itemCount == 1) "1 item" else "$itemCount itens")
    return parts.joinToString(" · ")
}

private fun formatKm(km: Double): String =
    String.format(Locale("pt", "BR"), "~%.1f km", km)

/** Mesmo corte de prefixo usado no filtro de lojas da tela de pedidos. */
private fun cleanStoreName(tradeName: String): String =
    tradeName.replace("ItaSuper - ", "")

/**
 * Rótulo curto do método de cobrança, espelhando os ramos de
 * `PaymentSummary.driverInstruction` sem o sufixo operacional.
 */
private fun paymentMethodLabel(method: String): String {
    val normalized = method.trim().lowercase()
    return when {
        isMachinePixMethod(normalized) -> "Pix na maquininha"
        normalized.contains("cart") || normalized.contains("crédito") ||
            normalized.contains("credito") || normalized.contains("débito") ||
            normalized.contains("debito") -> "Cartão · maquininha"
        normalized.contains("dinheiro") || normalized.contains("cash") ||
            normalized.contains("money") -> "Dinheiro"
        else -> method.replace('_', ' ').ifBlank { "Pagamento não informado" }
    }
}
