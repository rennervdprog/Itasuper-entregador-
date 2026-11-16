package com.example.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryOrder
import com.example.data.model.formatBrl
import java.util.Locale
import kotlin.math.roundToInt
import com.example.ui.components.DriverHelmetMark
import com.example.ui.components.RouteLine
import com.example.ui.components.RouteStop
import com.example.ui.components.RouteStopState
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
    onNavigateRoute: () -> Unit,
    onOpenContact: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val readyToDispatchCount = remember(orders) {
        orders.count { it.status == OrderDeliveryStatus.PRONTO_PARA_ENTREGA }
    }
    // O destaque de próxima parada só existe depois da saída explícita para entrega.
    val nextStopOrder = remember(orders) {
        orders.firstOrNull {
            it.status == OrderDeliveryStatus.SAIU_ENTREGA || it.status == OrderDeliveryStatus.EM_TRANSITO
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // "Próxima parada" só é exibida para rota que já saiu para entrega.
        if (nextStopOrder != null) {
            NextStopHeroCard(
                order = nextStopOrder,
                orders = orders,
                currentStopNumber = realStopNumber(orders, nextStopOrder),
                totalStops = orders.size,
                navPreference = navPreference,
                onNavPrefChange = onNavPrefChange,
                onStartNav = { onOpenNavigation(navPreference, nextStopOrder) },
                onOpenPinConfirm = { onOpenPinConfirm(nextStopOrder) },
                onCallCustomer = { onOpenContact("Telefone", nextStopOrder.customerPhone) },
                onWhatsApp = { onOpenContact("WhatsApp", nextStopOrder.customerPhone) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // "Sua rota" Summary Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = null,
                    tint = ItaOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_dispatch_all_ready")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
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

        Spacer(modifier = Modifier.height(12.dp))

        // Rota otimizada: total estimado + navegação da rota inteira.
        // Fluxo correto: primeiro "Sair para entrega". O botão Navegar
        // rota só aparece com a rota na rua (2+ paradas já despachadas)
        // e navega somente essas paradas — pedido ainda na loja não entra.
        val departedStops = orders.filter {
            it.status == OrderDeliveryStatus.SAIU_ENTREGA || it.status == OrderDeliveryStatus.EM_TRANSITO
        }
        if (orders.size >= 2) {
            val totalSource = if (departedStops.isNotEmpty()) departedStops else orders
            val routeTotalKm = totalSource.mapNotNull { it.routeLegKm }.sum()
            if (routeTotalKm > 0.0) {
                Text(
                    text = "Ordem otimizada pela menor distância · ± ${formatRouteKm(routeTotalKm)} no total",
                    fontSize = 12.sp,
                    color = ItaTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (departedStops.size >= 2) {
                Button(
                    onClick = onNavigateRoute,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_navigate_full_route")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Navegar rota (${departedStops.size} paradas)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (navPreference == NavigationPreference.WAZE) {
                    Text(
                        text = "A rota completa abre no Google Maps — o Waze não aceita várias paradas.",
                        fontSize = 11.sp,
                        color = ItaTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                Text(
                    text = "Depois de sair para entrega, o botão Navegar rota aparece aqui.",
                    fontSize = 11.sp,
                    color = ItaTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Active delivery cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            orders.forEachIndexed { index, order ->
                ActiveDeliveryCard(
                    order = order,
                    stopNumber = index + 1,
                    onDispatch = { onDispatchOrder(order.id) },
                    onConfirmPin = { onOpenPinConfirm(order) },
                    onNavigate = { onOpenNavigation(navPreference, order) },
                    onCallCustomer = { onOpenContact("Telefone", order.customerPhone) },
                    onWhatsApp = { onOpenContact("WhatsApp", order.customerPhone) },
                    isHeroCovered = nextStopOrder?.id == order.id
                )
            }
        }
    }
}

/**
 * Número real da parada atual na sequência otimizada da rota.
 *
 * Fonte: `DeliveryOrder.stopOrder`, preenchido por `optimizeRouteStops`
 * (RouteOptimizer) e aplicado à lista ativa no repositório — 1..n na
 * ordem de entrega (quem já saiu, a partir do motoboy; quem ainda está
 * na loja, a partir da loja). Se a lista não trouxer uma permutação
 * válida de 1..n (ex.: dados sem otimização, todos com o padrão 1), cai
 * para a posição do pedido na lista exibida — a mesma numeração dos
 * cartões de parada. Só leitura do modelo existente, nenhum campo novo.
 */
private fun realStopNumber(orders: List<DeliveryOrder>, order: DeliveryOrder): Int {
    val stopNumbers = orders.map { it.stopOrder }
    val isValidSequence = stopNumbers.size == stopNumbers.toSet().size &&
        stopNumbers.all { it in 1..orders.size }
    return if (isValidSequence) {
        order.stopOrder
    } else {
        orders.indexOf(order).takeIf { it >= 0 }?.plus(1) ?: 1
    }
}

@Composable
private fun NextStopHeroCard(
    order: DeliveryOrder,
    orders: List<DeliveryOrder>,
    currentStopNumber: Int,
    totalStops: Int,
    navPreference: NavigationPreference,
    onNavPrefChange: (NavigationPreference) -> Unit,
    onStartNav: () -> Unit,
    onOpenPinConfirm: () -> Unit,
    onCallCustomer: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val distanceText = order.routeLegKm?.let { formatRouteKm(it) }
        ?: order.estimatedDistanceKm.takeIf { it > 0.0 }?.let { "~ ${formatRouteKm(it)}" }
    val infoLine = listOfNotNull(
        order.addressNeighborhood.takeIf { it.isNotBlank() },
        distanceText,
        "Cliente: ${order.customerName}"
    ).joinToString(" · ")
    val methodLabel = order.payment.driverInstruction.substringBefore(" — ")
    val isPaid = order.payment.isPaidOnline

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(ItaOrange, RoundedCornerShape(50.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.TwoWheeler,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EM ROTA",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${order.shortCode} · ${order.store.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaTextTertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Parada atual com o número real da sequência otimizada.
            Text(
                text = "PARADA $currentStopNumber DE $totalStops · PRÓXIMA PARADA",
                style = MaterialTheme.typography.labelMedium,
                color = ItaOrangeDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = order.shortAddress + (order.addressComplement?.let { " - $it" } ?: ""),
                style = MaterialTheme.typography.displaySmall,
                color = ItaTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = infoLine,
                style = MaterialTheme.typography.bodyMedium,
                color = ItaTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pagamento em bloco heroico: cobrar (laranja sólido, sem
            // gradiente) ou já pago (verde suave). Valor e troco vêm dos
            // mesmos campos de hoje; nada de ganho do motoboy aqui.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isPaid) ItaGreenLight else ItaOrange,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = if (isPaid) {
                            "PAGAMENTO · $methodLabel"
                        } else {
                            "COBRAR NA ENTREGA · $methodLabel"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaid) ItaGreenDark else Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPaid) "Já pago" else formatBrl(order.payment.amount),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (isPaid) ItaGreenDark else Color.White
                    )
                    if (isPaid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total do pedido: ${formatBrl(order.payment.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ItaGreenDark
                        )
                    }
                    order.payment.displayChangeText?.let { changeText ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = changeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isPaid) ItaGreenDark else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Seletor do app de navegação (Maps/Waze), como hoje: a
            // preferência decide para onde o botão Mapa do dock navega.
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
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = if (navPreference == NavigationPreference.GOOGLE_MAPS) ItaOrange else ItaSlate600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Maps",
                            style = MaterialTheme.typography.labelLarge,
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
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = null,
                            tint = if (navPreference == NavigationPreference.WAZE) ItaOrange else ItaSlate600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Waze",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (navPreference == NavigationPreference.WAZE) ItaOrange else ItaSlate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dock de 3 colunas com os MESMOS intents de hoje: Mapa abre
            // a navegação externa; Ligar e WhatsApp, o contato do cliente.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartNav,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .testTag("btn_start_navigation_hero")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mapa",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }

                OutlinedButton(
                    onClick = onCallCustomer,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = null,
                            tint = ItaTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ligar",
                            style = MaterialTheme.typography.labelLarge,
                            color = ItaTextPrimary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onWhatsApp,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = null,
                            tint = ItaGreenDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "WhatsApp",
                            style = MaterialTheme.typography.labelLarge,
                            color = ItaTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mapa abre no Google Maps ou Waze, como hoje.",
                style = MaterialTheme.typography.bodySmall,
                color = ItaTextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SUA ROTA",
                style = MaterialTheme.typography.labelSmall,
                color = ItaTextTertiary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Linha da rota completa: retirada feita, parada atual e
            // próximas, na sequência otimizada já exibida pelos cartões.
            RouteLine(
                stops = buildList {
                    add(
                        RouteStop(
                            title = "Retirada · ${order.store.name}",
                            subtitle = order.store.address.takeIf { it.isNotBlank() },
                            state = RouteStopState.DONE
                        )
                    )
                    val heroIndex = orders.indexOf(order)
                    orders.forEachIndexed { index, routeOrder ->
                        val isDeparted = routeOrder.status == OrderDeliveryStatus.SAIU_ENTREGA ||
                            routeOrder.status == OrderDeliveryStatus.EM_TRANSITO
                        add(
                            RouteStop(
                                title = routeOrder.shortAddress +
                                    (routeOrder.addressComplement?.let { " - $it" } ?: ""),
                                subtitle = listOfNotNull(
                                    routeOrder.addressNeighborhood.takeIf { it.isNotBlank() },
                                    "Cliente: ${routeOrder.customerName}"
                                ).joinToString(" · "),
                                state = when {
                                    index == heroIndex -> RouteStopState.CURRENT
                                    index < heroIndex && isDeparted -> RouteStopState.DONE
                                    else -> RouteStopState.NEXT
                                }
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chegada abre a confirmação com PIN — o mesmo callback do
            // botão "Confirmar entrega com PIN" do cartão da parada.
            Button(
                onClick = onOpenPinConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ItaGreenDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("btn_arrived_destination")
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CHEGUEI AO DESTINO",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}


/** Formata distância de trecho/total em pt-BR: "850 m" ou "1,2 km". */
private fun formatRouteKm(km: Double): String =
    if (km < 1.0) {
        "${(km * 1000).roundToInt()} m"
    } else {
        String.format(Locale("pt", "BR"), "%.1f km", km)
    }

@Composable
fun ActiveDeliveryCard(
    order: DeliveryOrder,
    stopNumber: Int,
    onDispatch: () -> Unit,
    onConfirmPin: () -> Unit,
    onNavigate: () -> Unit,
    onCallCustomer: () -> Unit,
    onWhatsApp: () -> Unit,
    // O cartão do pedido coberto pelo herói ("próxima parada") é só resumo:
    // as ações (Mapa/Ligar/WhatsApp + PIN) vivem no herói, acima dele.
    // Cartões de outros pedidos mantêm seus botões — o herói não os cobre.
    isHeroCovered: Boolean = false
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
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isReady) "PRONTO PARA SAIR" else "EM ENTREGA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        color = if (isReady) ItaStatusPending else Color(0xFF1D4ED8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destination Neighborhood + Address
            Text(
                text = order.addressNeighborhood,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ItaSlate900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = order.fullAddress,
                fontSize = 13.sp,
                color = ItaSlate500
            )

            if (order.routeLegKm != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Route,
                        contentDescription = null,
                        tint = ItaOrange,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Distância do trecho: ${formatRouteKm(order.routeLegKm)}",
                        fontSize = 12.sp,
                        color = ItaTextSecondary
                    )
                }
            }

            // Customer Name & Contact
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cliente: ${order.customerName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ItaSlate700
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ItaDivider)
            Spacer(modifier = Modifier.height(12.dp))

            // In-transit actions: Navigation, WhatsApp, Call — só quando o
            // herói não cobre este pedido (o herói já traz o mesmo dock).
            if (isInTransit && !isHeroCovered) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigate,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_nav_delivery_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
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
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_whatsapp_delivery_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
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
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("btn_call_delivery_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = null,
                            tint = ItaSlate500,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Itens: ${order.driverItemsSummary}",
                        fontSize = 12.sp,
                        color = ItaSlate700,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = order.payment.driverInstruction,
                        fontSize = 11.sp,
                        color = if (order.payment.isPaidOnline) ItaGreenDark else ItaSlate600,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
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
                        .padding(12.dp)
                ) {
                    // Sem repetir a instrução de pagamento aqui: ela já
                    // está na linha de resumo acima da caixa.
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
                                text = formatBrl(item.quantity * item.unitPrice),
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
                            color = ItaStatusPending
                        )
                    }
                }
            }

            if (!isHeroCovered) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main Action Button depending on status — o cartão coberto
            // pelo herói não mostra botão: a ação vive no herói.
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
                    DriverHelmetMark(
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
            } else if (!isHeroCovered) {
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
                        imageVector = Icons.Filled.Key,
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
