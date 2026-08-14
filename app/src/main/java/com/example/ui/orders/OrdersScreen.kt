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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverLinkStatus
import com.example.ui.components.ConnectivityBanner
import com.example.ui.components.MetricsRow
import com.example.ui.components.OnlineHeroToggle
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate400
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    historyViewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val availability by viewModel.availability.collectAsState()
    val linkedStores by viewModel.linkedStores.collectAsState()
    val availableOrders by viewModel.availableOrders.collectAsState()
    val activeRouteOrders by viewModel.activeRouteOrders.collectAsState()
    val todayCompletedCount by viewModel.todayCompletedCount.collectAsState()
    val isOptimizedRoute by viewModel.isOptimizedRoute.collectAsState()
    val navPreference by viewModel.navPreference.collectAsState()
    val isConnectivityBannerVisible by viewModel.isConnectivityBannerVisible.collectAsState()
    val offlineConfirmations by viewModel.offlineConfirmations.collectAsState()

    val acceptedStores = linkedStores.filter { it.status == DriverLinkStatus.ACCEPTED }
    val filteredAvailableOrders = availableOrders.filter {
        uiState.selectedStoreFilterId == null || it.store.id == uiState.selectedStoreFilterId
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage, uiState.simulatedNavToastMessage) {
        val message = uiState.feedbackMessage ?: uiState.simulatedNavToastMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Compact Header with store indicator
                TopHeader(
                    driverName = profile.name,
                    storesCount = acceptedStores.size,
                    onResetDemo = { viewModel.resetDemoData() }
                )

                // Segmented Switch: Entregas vs Histórico
                TabRow(
                    selectedTabIndex = uiState.activeTabSegment,
                    containerColor = ItaSurface,
                    contentColor = ItaOrange,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.activeTabSegment]),
                            color = ItaOrange,
                            height = 3.dp
                        )
                    },
                    divider = { HorizontalDivider(color = ItaDivider) }
                ) {
                    Tab(
                        selected = uiState.activeTabSegment == 0,
                        onClick = { viewModel.setTabSegment(0) },
                        text = {
                            Text(
                                text = "Entregas",
                                fontWeight = if (uiState.activeTabSegment == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_segment_deliveries")
                    )
                    Tab(
                        selected = uiState.activeTabSegment == 1,
                        onClick = { viewModel.setTabSegment(1) },
                        text = {
                            Text(
                                text = "Histórico",
                                fontWeight = if (uiState.activeTabSegment == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_segment_history")
                    )
                }

                if (uiState.activeTabSegment == 1) {
                    // Histórico View
                    HistoryScreen(viewModel = historyViewModel)
                } else {
                    // Entregas View
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Local Connectivity Banner & Offline Sync Faixa
                        ConnectivityBanner(
                            isVisible = isConnectivityBannerVisible,
                            offlineConfirmations = offlineConfirmations
                        )

                        // 2. Hero Online/Offline full-width control
                        OnlineHeroToggle(
                            isOnline = availability.isOnline,
                            onToggle = { viewModel.toggleAvailability(it) }
                        )

                        // 3. Metrics Summary Row (Na rota, Disponíveis, Concluídas)
                        MetricsRow(
                            activeCount = activeRouteOrders.size,
                            availableCount = availableOrders.size,
                            completedCount = todayCompletedCount
                        )

                        // 4. Linked Stores Horizontal Chips (when linked to stores)
                        if (acceptedStores.isNotEmpty()) {
                            StoresChipsFilter(
                                stores = acceptedStores,
                                availableOrders = availableOrders,
                                selectedStoreId = uiState.selectedStoreFilterId,
                                onSelectStore = { viewModel.setStoreFilter(it) }
                            )
                        }

                        // 5. Rota Otimizada Toggle
                        OptimizedRouteToggleCard(
                            isEnabled = isOptimizedRoute,
                            onToggle = { viewModel.toggleOptimizedRoute(it) },
                            activeCount = activeRouteOrders.size,
                            totalKm = activeRouteOrders.sumOf { it.estimatedDistanceKm },
                            totalMinutes = activeRouteOrders.sumOf { it.estimatedTimeMinutes }
                        )

                        // 6. Active Route Section
                        if (activeRouteOrders.isNotEmpty()) {
                            ActiveRouteSection(
                                orders = activeRouteOrders,
                                navPreference = navPreference,
                                onNavPrefChange = { viewModel.setNavPreference(it) },
                                onDispatchOrder = { viewModel.dispatchOrder(it) },
                                onDispatchAll = { viewModel.dispatchAllReady() },
                                onOpenPinConfirm = { viewModel.openPinConfirmDialog(it) },
                                onSimulateNav = { app, addr -> viewModel.simulateExternalNavigation(app, addr) },
                                onSimulateContact = { type, num -> viewModel.simulateContactAction(type, num) }
                            )
                        }

                        // 7. Available Orders Section (Visible only when Online)
                        if (availability.isOnline) {
                            if (filteredAvailableOrders.isNotEmpty()) {
                                AvailableOrdersSection(
                                    orders = filteredAvailableOrders,
                                    hasActiveRoute = activeRouteOrders.isNotEmpty(),
                                    onAcceptOrder = { viewModel.acceptOrder(it) },
                                    onAcceptAll = { viewModel.acceptAllAvailable() },
                                    onRejectOrder = { viewModel.rejectOrder(it) }
                                )
                            } else if (activeRouteOrders.isEmpty()) {
                                // Online and empty
                                OnlineEmptyState(onRefresh = { viewModel.resetDemoData() })
                            }
                        } else if (activeRouteOrders.isEmpty()) {
                            // Offline empty state
                            OfflineEmptyState(onGoOnline = { viewModel.toggleAvailability(true) })
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Snackbar host for notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // PIN Confirmation Dialog
        uiState.orderToConfirmPin?.let { order ->
            PinConfirmDialog(
                order = order,
                enteredPin = uiState.enteredPin,
                errorMessage = uiState.pinErrorMessage,
                isLoading = uiState.isCompletingDelivery,
                onPinChange = { viewModel.onPinChange(it) },
                onConfirm = { viewModel.confirmDeliveryWithPin() },
                onDismiss = { viewModel.closePinConfirmDialog() }
            )
        }
    }
}

@Composable
private fun TopHeader(
    driverName: String,
    storesCount: Int,
    onResetDemo: () -> Unit
) {
    val initials = driverName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "MS" }

    Surface(
        color = ItaSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaDivider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ITASUPER ENTREGADOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = ItaSlate500
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Olá, $driverName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaSlate900
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onResetDemo,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_reset_demo_data")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restaurar pedidos demo",
                        tint = ItaSlate400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ItaOrange, CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StoresChipsFilter(
    stores: List<com.example.data.model.StoreDriverLink>,
    availableOrders: List<com.example.data.model.DeliveryOrder>,
    selectedStoreId: String?,
    onSelectStore: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("row_stores_filter"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Todas" chip
        item {
            FilterChip(
                selected = selectedStoreId == null,
                onClick = { onSelectStore(null) },
                label = {
                    Text(
                        text = "Todas (${availableOrders.size})",
                        fontSize = 12.sp,
                        fontWeight = if (selectedStoreId == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ItaOrangeLight,
                    selectedLabelColor = ItaOrange,
                    containerColor = ItaSurface,
                    labelColor = ItaTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedStoreId == null,
                    borderColor = if (selectedStoreId == null) ItaOrange else ItaBorder
                )
            )
        }

        // Store specific chips
        items(stores, key = { it.id }) { link ->
            val storeOrdersCount = availableOrders.count { it.store.id == link.store.id }
            val isSelected = selectedStoreId == link.store.id

            FilterChip(
                selected = isSelected,
                onClick = { onSelectStore(link.store.id) },
                label = {
                    Text(
                        text = "${link.store.tradeName.replace("ItaSuper - ", "")} ($storeOrdersCount)",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ItaOrangeLight,
                    selectedLabelColor = ItaOrange,
                    containerColor = ItaSurface,
                    labelColor = ItaTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) ItaOrange else ItaBorder
                )
            )
        }
    }
}

@Composable
private fun OptimizedRouteToggleCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    activeCount: Int,
    totalKm: Double,
    totalMinutes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ItaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AltRoute,
                    contentDescription = null,
                    tint = if (isEnabled) ItaOrange else ItaTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Rota otimizada de entrega",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ItaTextPrimary
                    )
                    Text(
                        text = if (activeCount > 0)
                            "Estimativa demo: ${String.format("%.1f", totalKm)} km · ~$totalMinutes min"
                        else
                            "Ordenação sequencial inteligente por bairros",
                        fontSize = 11.sp,
                        color = ItaTextSecondary
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ItaOrange,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.testTag("switch_optimized_route")
            )
        }
    }
}

@Composable
private fun OfflineEmptyState(
    onGoOnline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFFE2E8F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerOff,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Você está offline",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ItaTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Fique online para começar a receber pedidos das suas lojas vinculadas.",
            fontSize = 13.sp,
            color = ItaTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onGoOnline,
            colors = ButtonDefaults.buttonColors(containerColor = ItaGreenDark),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("btn_go_online_empty")
        ) {
            Text("Ficar Online Agora", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnlineEmptyState(
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(ItaGreenLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = ItaGreenDark,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Aguardando novos pedidos",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ItaTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Assim que as lojas parceiras expedirem pedidos, eles surgirão automaticamente aqui.",
            fontSize = 13.sp,
            color = ItaTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onRefresh,
            modifier = Modifier.testTag("btn_reload_demo_orders")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = ItaOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Carregar novos pedidos demo", color = ItaOrange, fontWeight = FontWeight.SemiBold)
        }
    }
}
