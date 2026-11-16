package com.example.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.model.DriverLinkStatus
import com.example.data.model.OrderDeliveryStatus
import com.example.data.model.StoreDriverLink
import com.example.ui.components.ConnectivityBanner
import com.example.ui.components.MetricsRow
import com.example.ui.components.OnlineHeroToggle
import com.example.ui.history.HistoryScreen
import com.example.platform.DriverExternalActions
import com.example.ui.history.HistoryViewModel
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaGreenDark
import com.example.ui.theme.ItaGreenLight
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate100
import com.example.ui.theme.ItaSlate400
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate600
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaStatusPending
import com.example.ui.theme.ItaStatusPendingBg
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    historyViewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val availability by viewModel.availability.collectAsState()
    val linkedStores by viewModel.linkedStores.collectAsState()
    val availableOrders by viewModel.availableOrders.collectAsState()
    val updateInfo by com.example.platform.AppUpdateChecker.available.collectAsState()
    val activeRouteOrders by viewModel.activeRouteOrders.collectAsState()
    val todayCompletedCount by viewModel.todayCompletedCount.collectAsState()
    val navPreference by viewModel.navPreference.collectAsState()
    val offlineConfirmations by viewModel.offlineConfirmations.collectAsState()

    val acceptedStores = remember(linkedStores) {
        linkedStores.filter { it.status == DriverLinkStatus.ACCEPTED }
    }
    val filteredAvailableOrders = remember(availableOrders, uiState.selectedStoreFilterId) {
        availableOrders.filter {
            uiState.selectedStoreFilterId == null || it.store.id == uiState.selectedStoreFilterId
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshAfterAppResume()
            while (true) {
                kotlinx.coroutines.delay(60_000)
                viewModel.refreshOrders(silent = true)
            }
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
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
                    onRefresh = { viewModel.refreshOrders() }
                )

                // Segmented Switch: Entregas vs Histórico
                // Sombra sutil: a barra fixa se separa do conteúdo rolante.
                Surface(
                    shadowElevation = 2.dp,
                    color = ItaSurface
                ) {
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
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (uiState.activeTabSegment == 0) FontWeight.Bold else FontWeight.Medium
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
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (uiState.activeTabSegment == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("tab_segment_history")
                        )
                    }
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
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 0. Aviso de versão nova (Fase 4.2)
                        val currentUpdateInfo = updateInfo
                        if (currentUpdateInfo != null) {
                            UpdateAvailableBanner(
                                info = currentUpdateInfo,
                                onDownload = {
                                    com.example.platform.AppUpdateChecker.openDownload(context, currentUpdateInfo)
                                },
                                onDismiss = {
                                    com.example.platform.AppUpdateChecker.dismiss(context)
                                }
                            )
                        }

                        // 1. Local Connectivity Banner & Offline Sync Faixa
                        ConnectivityBanner(
                            offlineConfirmations = offlineConfirmations
                        )

                        // 2. Hero Online/Offline full-width control
                        OnlineHeroToggle(
                            isOnline = availability.isOnline,
                            isRouteActive = activeRouteOrders.isNotEmpty(),
                            visibleStoresCount = acceptedStores.size,
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

                        // O cartão "rota otimizada" foi removido (auditoria
                        // M3): era um toggle de preferência sem ordenação
                        // real e exibia "~0 min" permanente, porque
                        // estimatedTimeMinutes é sempre 0 no mapeamento.

                        // 6. Active Route Section
                        if (activeRouteOrders.isNotEmpty()) {
                            ActiveRouteSection(
                                orders = activeRouteOrders,
                                navPreference = navPreference,
                                onNavPrefChange = { viewModel.setNavPreference(it) },
                                onDispatchOrder = { viewModel.dispatchOrder(it) },
                                onDispatchAll = { viewModel.dispatchAllReady() },
                                onOpenPinConfirm = { viewModel.openPinConfirmDialog(it) },
                                onOpenNavigation = { preference, order ->
                                    DriverExternalActions.openNavigation(
                                        context = context,
                                        preference = preference,
                                        destination = order.fullAddress,
                                        latitude = order.destinationLatitude,
                                        longitude = order.destinationLongitude,
                                        destinationPrecision = order.destinationPrecision
                                    ).onFailure { viewModel.reportExternalActionFailure(it.message) }
                                },
                                onNavigateRoute = {
                                    // Só as paradas já despachadas entram na
                                    // navegação: pedido ainda na loja não é destino.
                                    val departed = activeRouteOrders.filter {
                                        it.status == OrderDeliveryStatus.SAIU_ENTREGA ||
                                            it.status == OrderDeliveryStatus.EM_TRANSITO
                                    }
                                    DriverExternalActions.openFullRoute(context, departed)
                                        .onFailure { viewModel.reportExternalActionFailure(it.message) }
                                },
                                onOpenContact = { type, num ->
                                    val action = if (type == "WhatsApp") {
                                        DriverExternalActions.openWhatsApp(context, num)
                                    } else {
                                        DriverExternalActions.openDialer(context, num)
                                    }
                                    action.onFailure { viewModel.reportExternalActionFailure(it.message) }
                                }
                            )
                        }

                        // 7. Available Orders Section (Visible only when Online)
                        if (availability.isOnline) {
                            if (filteredAvailableOrders.isNotEmpty()) {
                                AvailableOrdersSection(
                                    orders = filteredAvailableOrders,
                                    // Fase 1: a trava de aceite só vale com a rota
                                    // já despachada. Pedido aceito ainda na loja
                                    // pode receber companhia na mesma rota.
                                    hasActiveRoute = activeRouteOrders.any {
                                        it.status == OrderDeliveryStatus.SAIU_ENTREGA ||
                                            it.status == OrderDeliveryStatus.EM_TRANSITO
                                    },
                                    onAcceptOrder = { viewModel.acceptOrder(it) },
                                    onAcceptAll = { viewModel.acceptAllAvailable() }
                                )
                            } else if (activeRouteOrders.isEmpty()) {
                                // Online and empty
                                OnlineEmptyState(
                                    onRefresh = { viewModel.refreshOrders() },
                                    pendingSyncCount = offlineConfirmations.size,
                                    storesCount = acceptedStores.size
                                )
                            }
                        } else if (activeRouteOrders.isEmpty()) {
                            // Offline: o herói acima já carrega o botão
                            // gigante; aqui ficam as lojas vinculadas e o
                            // compromisso do rastreio.
                            OfflineEmptyState(stores = acceptedStores)
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
    onRefresh: () -> Unit
) {
    val initials = driverName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "MS" }

    Surface(
        color = ItaSurface,
        border = BorderStroke(1.dp, ItaDivider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ItaSuper Entregador",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = ItaSlate500
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Olá, $driverName",
                    style = MaterialTheme.typography.titleMedium,
                    color = ItaSlate900
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_refresh_orders")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Atualizar pedidos",
                        tint = ItaSlate400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun StoresChipsFilter(
    stores: List<StoreDriverLink>,
    availableOrders: List<com.example.data.model.DeliveryOrder>,
    selectedStoreId: String?,
    onSelectStore: (String?) -> Unit
) {
    val orderCountByStoreId = remember(availableOrders) {
        availableOrders.groupingBy { it.store.id }.eachCount()
    }

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
                        style = MaterialTheme.typography.labelMedium,
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
            val storeOrdersCount = orderCountByStoreId[link.store.id] ?: 0
            val isSelected = selectedStoreId == link.store.id

            FilterChip(
                selected = isSelected,
                onClick = { onSelectStore(link.store.id) },
                label = {
                    Text(
                        text = "${link.store.tradeName.replace("ItaSuper - ", "")} ($storeOrdersCount)",
                        style = MaterialTheme.typography.labelMedium,
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

/**
 * Offline, sem rota: as lojas vinculadas + o compromisso do rastreio. O
 * botão de ficar online vive no herói acima — sem CTA duplicado.
 */
@Composable
private fun OfflineEmptyState(
    stores: List<StoreDriverLink>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Suas lojas",
            style = MaterialTheme.typography.titleMedium,
            color = ItaTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (stores.isEmpty()) {
            Text(
                text = "Nenhuma loja vinculada ainda.",
                style = MaterialTheme.typography.bodyMedium,
                color = ItaTextSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stores.forEach { link ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ItaSurface),
                        border = BorderStroke(1.dp, ItaBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = ItaOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = link.store.tradeName.replace("ItaSuper - ", ""),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ItaTextPrimary
                                )
                                Text(
                                    text = if (link.store.neighborhood.isNotBlank()) {
                                        "${link.store.neighborhood} · vinculada"
                                    } else {
                                        "vinculada"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ItaTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Vinculada",
                                tint = ItaGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "O rastreio só liga quando você aceita e sai para uma entrega. " +
                "Offline, sua posição não é compartilhada.",
            style = MaterialTheme.typography.bodySmall,
            color = ItaTextSecondary
        )
    }
}

/**
 * Online vazio: "De olho na pista" + selos de saúde com o estado REAL já
 * consumido pela tela (fila de sincronização e lojas vinculadas). Não há
 * selo de GPS nem de avisos: nenhum estado de GPS/permissão chega a esta
 * tela pelo ViewModel, e um selo "GPS ok" sem medição seria a mentira que
 * a auditoria N-P1-3 denunciou.
 */
@Composable
private fun OnlineEmptyState(
    onRefresh: () -> Unit,
    pendingSyncCount: Int,
    storesCount: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "De olho na pista.",
            style = MaterialTheme.typography.headlineMedium,
            color = ItaTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Quando um pedido cair, ele toma a tela com som — você decide em segundos.",
            style = MaterialTheme.typography.bodyMedium,
            color = ItaTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ItaSurface),
            border = BorderStroke(1.dp, ItaBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(ItaOrangeLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = ItaOrange,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhum pedido agora",
                    style = MaterialTheme.typography.titleMedium,
                    color = ItaTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Suas lojas ainda não liberaram nada. O app avisa na hora que chegar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ItaTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (pendingSyncCount > 0) {
                        HealthPill(
                            icon = Icons.Filled.Sync,
                            text = "Sincronização pendente ($pendingSyncCount)",
                            backgroundColor = ItaStatusPendingBg,
                            contentColor = ItaStatusPending
                        )
                    } else {
                        HealthPill(
                            icon = Icons.Filled.CloudDone,
                            text = "Sincronização em dia",
                            backgroundColor = ItaGreenLight,
                            contentColor = ItaGreenDark
                        )
                    }
                    if (storesCount > 0) {
                        HealthPill(
                            icon = Icons.Filled.Storefront,
                            text = if (storesCount == 1) "1 loja vinculada" else "$storesCount lojas vinculadas",
                            backgroundColor = ItaSlate100,
                            contentColor = ItaSlate600
                        )
                    } else {
                        HealthPill(
                            icon = Icons.Filled.Storefront,
                            text = "Nenhuma loja vinculada",
                            backgroundColor = ItaStatusPendingBg,
                            contentColor = ItaStatusPending
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onRefresh,
            modifier = Modifier.testTag("btn_refresh_empty_orders")
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = ItaOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Atualizar pedidos",
                style = MaterialTheme.typography.labelLarge,
                color = ItaOrange
            )
        }
    }
}

@Composable
private fun HealthPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

/** Banner de versão nova (Fase 4.2) — some quando o motoboy baixa ou dispensa. */
@Composable
private fun UpdateAvailableBanner(
    info: com.example.platform.DriverAppVersionInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ItaOrange)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = ItaOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nova versão disponível: ${info.version}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ItaSlate900
                )
                Text(
                    text = "Atualize para receber as melhorias e correções.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaSlate500
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Baixar agora",
                    style = MaterialTheme.typography.labelLarge,
                    color = ItaOrange,
                    modifier = Modifier.clickable { onDownload() }
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dispensar aviso",
                    tint = ItaSlate500
                )
            }
        }
    }
}
