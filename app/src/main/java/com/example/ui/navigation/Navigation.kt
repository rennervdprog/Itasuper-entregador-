package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object OnboardingLink : Screen("onboarding_link")
    object Dashboard : Screen("dashboard")
    object MainDashboard : Screen("main_dashboard")
}

enum class DashboardTab(val title: String, val testTag: String) {
    ORDERS("Pedidos", "tab_orders"),
    SUPPORT("Suporte", "tab_support"),
    PROFILE("Perfil", "tab_profile")
}
