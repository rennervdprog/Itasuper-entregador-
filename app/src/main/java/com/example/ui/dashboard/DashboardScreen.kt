package com.example.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.history.HistoryViewModel
import com.example.ui.orders.OrdersScreen
import com.example.ui.orders.OrdersViewModel
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel
import com.example.ui.support.SupportScreen
import com.example.ui.support.SupportViewModel
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangePill
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

enum class DashboardTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ORDERS("Pedidos", Icons.Filled.DeliveryDining, Icons.Outlined.DeliveryDining, "nav_tab_orders"),
    SUPPORT("Suporte", Icons.Filled.HeadsetMic, Icons.Outlined.HeadsetMic, "nav_tab_support"),
    PROFILE("Perfil", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
}

@Composable
fun DashboardScreen(
    ordersViewModel: OrdersViewModel,
    historyViewModel: HistoryViewModel,
    supportViewModel: SupportViewModel,
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.ORDERS) }
    val activeOrders by ordersViewModel.activeRouteOrders.collectAsState()
    val availableOrders by ordersViewModel.availableOrders.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ItaBackground,
        bottomBar = {
            NavigationBar(
                containerColor = ItaSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .drawBehind {
                        drawLine(
                            color = ItaDivider,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .testTag("main_navigation_bar")
            ) {
                DashboardTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == DashboardTab.ORDERS && (activeOrders.isNotEmpty() || availableOrders.isNotEmpty())) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = if (activeOrders.isNotEmpty()) ItaOrange else ItaSlate500
                                        ) {
                                            Text(
                                                text = if (activeOrders.isNotEmpty()) activeOrders.size.toString() else availableOrders.size.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ItaOrange,
                            selectedTextColor = ItaOrange,
                            unselectedIconColor = ItaSlate900.copy(alpha = 0.45f),
                            unselectedTextColor = ItaSlate900.copy(alpha = 0.45f),
                            indicatorColor = ItaOrangePill
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                DashboardTab.ORDERS -> OrdersScreen(
                    viewModel = ordersViewModel,
                    historyViewModel = historyViewModel
                )
                DashboardTab.SUPPORT -> SupportScreen(
                    viewModel = supportViewModel
                )
                DashboardTab.PROFILE -> ProfileScreen(
                    viewModel = profileViewModel,
                    onLogout = onLogout
                )
            }
        }
    }
}
