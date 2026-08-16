package com.example.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryOrder
import com.example.data.model.NavigationPreference
import com.example.data.model.OrderDeliveryStatus
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeDark
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaStatusInTransit
import com.example.ui.theme.ItaStatusInTransitBg
import com.example.ui.theme.ItaStatusPending
import com.example.ui.theme.ItaStatusPendingBg
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate200
import com.example.ui.theme.ItaSlate300
import com.example.ui.theme.ItaSlate400
import com.example.ui.theme.ItaSlate50
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
fun ActiveRouteSection(
    orders: List<DeliveryOrder>,
    navPreference: NavigationPreference,
    onNavPrefChange: (NavigationPreference) -> Unit,
    onDispatchOrder: (String) -> Unit,
    onDispatchAll: () -> Unit,
    onOpenPinConfirm: (DeliveryOrder) -> Unit,
    onOpenNavigation: (NavigationPreference, DeliveryOrder) -> Unit,
    onOpenContact: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val readyToDispatchCount = orders.count { it.status == OrderDeliveryStatus.PRONTO_PARA_ENTREGA }
    // O destaque de próxima parada só existe depois da saída explícita para entrega.
    val nextStopOrder = orders.firstOrNull {
        it.status == OrderDeliveryStatus.SAIU_ENTREGA || it.status == OrderDeliveryStatus.EM_TRANSITO
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // "Próxima parada" só é exibida para rota que já saiu para entrega.
        if (nextStopOrder != null) {
            NextStopHeroCard(
                order = nextStopOrder,
                totalStops = orders.size,
                navPreference = navPreference,
                onNavPrefChange = onNavPrefChange,
                onStartNav = { onOpenNavigation(navPreference, nextStopOrder) }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // "Sua rota" Summary Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = ItaOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sua rota ativa (${orders.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaTextPrimary
                )
            }

            if (readyToDispatchCount > 1) {
                Button(
                    onClick = onDispatchAll,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_dispatch_all_ready")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sair para entrega ($readyToDispatchCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active delivery cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            orders.forEachIndexed { index, order ->
                ActiveDeliveryCard(
                    order = order,
                    stopNumber = index + 1,
                    onDispatch = { onDispatchOrder(order.id) },
                    onConfirmPin = { onOpenPinConfirm(order) },
                    onNavigate = { onOpenNavigation(navPreference, order) },
                    onCallCustomer = { onOpenContact("Telefone", order.customerPhone) },
                    onWhatsApp = { onOpenContact("WhatsApp", order.customerPhone) }
                )
            }
        }
    }
}

@Composable
private fun NextStopHeroCard(
    order: DeliveryOrder,
    totalStops: Int = 1,
    navPreference: NavigationPreference,
    onNavPrefChange: (NavigationPreference) -> Unit,
    onStartNav: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_next_stop_hero"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: EM ENTREGA ATIVA + Próxima Parada + PARADA 1 DE X badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "EM ENTREGA ATIVA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = ItaOrange
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Próxima parada",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaSlate900
                    )
                }

                Box(
                    modifier = Modifier
                        .background(ItaSlate100, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PARADA 1 DE $totalStops",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaSlate600
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inner slate container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ItaSlate50, RoundedCornerShape(16.dp))
                    .border(1.dp, ItaSlate200, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                // Destination + Pedido #code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bairro: ${order.addressNeighborhood}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaSlate900
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = order.shortAddress + (order.addressComplement?.let { " - $it" } ?: ""),
                            fontSize = 12.sp,
                            color = ItaSlate500
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PEDIDO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaSlate400
                        )
                        Text(
                            text = order.shortCode,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaSlate900
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation App Selector Buttons (Maps & Waze)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (navPreference == NavigationPreference.GOOGLE_MAPS) ItaOrangeLight else ItaSurface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (navPreference == NavigationPreference.GOOGLE_MAPS) ItaOrange else ItaSlate200,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onNavPrefChange(NavigationPreference.GOOGLE_MAPS) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (navPreference == NavigationPreference.GOOGLE_MAPS) ItaOrange else ItaSlate600,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Maps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (navPreference == NavigationPreference.GOOGLE_MAPS) ItaOrange else ItaSlate700
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (navPreference == NavigationPreference.WAZE) ItaOrangeLight else ItaSurface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (navPreference == NavigationPreference.WAZE) ItaOrange else ItaSlate200,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onNavPrefChange(NavigationPreference.WAZE) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = if (navPreference == NavigationPreference.WAZE) ItaOrange else ItaSlate600,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Waze",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (navPreference == NavigationPreference.WAZE) ItaOrange else ItaSlate700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Info & Troco Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pagamento: ${order.payment.method}",
                        fontSize = 12.sp,
                        color = ItaSlate600,
                        fontWeight = FontWeight.Medium
                    )

                    order.payment.displayChangeText?.let { changeText ->
                        Text(
                            text = changeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Start Navigation Action Button
                Button(
                    onClick = onStartNav,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_start_navigation_hero")
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Navegar com ${navPreference.displayName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveDeliveryCard(
    order: DeliveryOrder,
    stopNumber: Int,
    onDispatch: () -> Unit,
    onConfirmPin: () -> Unit,
    onNavigate: () -> Unit,
    onCallCustomer: () -> Unit,
    onWhatsApp: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val isReady = order.status == OrderDeliveryStatus.PRONTO_PARA_ENTREGA
    val isInTransit = order.status == OrderDeliveryStatus.SAIU_ENTREGA || order.status == OrderDeliveryStatus.EM_TRANSITO

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_active_order_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isInTransit) ItaOrange.copy(alpha = 0.6f) else ItaBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Stop order badge + status banner + short code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (isInTransit) ItaOrange else ItaSlate600, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stopNumber.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.shortCode,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaSlate900
                    )
                }

                // Status Pill
                Box(
                    modifier = Modifier
                        .background(
                            if (isReady) ItaStatusPendingBg else ItaStatusInTransitBg,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isReady) "PRONTO PARA SAIR" else "EM ENTREGA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        color = if (isReady) Color(0xFFB45309) else Color(0xFF1D4ED8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Destination Neighborhood + Address
            Text(
                text = order.addressNeighborhood,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ItaSlate900
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = order.fullAddress,
                fontSize = 13.sp,
                color = ItaSlate500
            )

            // Customer Name & Contact
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cliente: ${order.customerName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ItaSlate700
            )

            order.notes?.let { note ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Observação: $note",
                    fontSize = 11.sp,
                    color = ItaSlate500
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ItaDivider)
            Spacer(modifier = Modifier.height(12.dp))

            // In-transit actions: Navigation, WhatsApp, Call
            if (isInTransit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigate,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_nav_delivery_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = ItaOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mapa", fontSize = 12.sp, color = ItaOrange, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onWhatsApp,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_whatsapp_delivery_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, color = ItaSlate800, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onCallCustomer,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("btn_call_delivery_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = ItaSlate500,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Expandable Items and Payment Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.items.size} itens no pedido • ${order.payment.method}",
                    fontSize = 12.sp,
                    color = ItaSlate500
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ItaSlate400,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ItaSlate50, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.name}",
                                fontSize = 12.sp,
                                color = ItaSlate900
                            )
                            Text(
                                text = "R$ ${String.format("%.2f", item.quantity * item.unitPrice)}",
                                fontSize = 12.sp,
                                color = ItaSlate500
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    order.payment.displayChangeText?.let { changeText ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Troco: $changeText",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Action Button depending on status
            if (isReady) {
                Button(
                    onClick = onDispatch,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_dispatch_order_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeliveryDining,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sair para entrega",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = onConfirmPin,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaGreenDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_confirm_delivery_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirmar entrega com PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
