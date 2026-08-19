package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.platform.DriverBiometricAccess
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.BiometricEnrollmentScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.onboarding.DriverLinkViewModel
import com.example.ui.onboarding.OnboardingLinkScreen
import com.example.ui.orders.OrdersViewModel
import com.example.ui.profile.ProfileViewModel
import com.example.ui.splash.SplashScreen
import com.example.ui.support.SupportViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    fun navigateAfterAuthentication(hasAcceptedLink: Boolean, popUpRoute: String) {
        val destination = if (hasAcceptedLink) Screen.Dashboard.route else Screen.OnboardingLink.route
        navController.navigate(destination) {
            popUpTo(popUpRoute) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = { biometricAvailable ->
                    navController.navigate(Screen.Login.createRoute(biometricAvailable)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Login.route,
            arguments = listOf(
                navArgument("biometric") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            val authViewModel: AuthViewModel = viewModel()
            val biometricAvailable = entry.arguments?.getBoolean("biometric") ?: false
            LoginScreen(
                viewModel = authViewModel,
                biometricAvailable = biometricAvailable,
                onBiometricSuccess = { _, hasLink ->
                    navigateAfterAuthentication(hasLink, Screen.Login.route)
                },
                onLoginSuccess = { profile, hasLink ->
                    if (DriverBiometricAccess.isEnabledFor(profile.id)) {
                        navigateAfterAuthentication(hasLink, Screen.Login.route)
                    } else {
                        navController.navigate(Screen.BiometricEnrollment.createRoute(profile.id, hasLink)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.BiometricEnrollment.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("hasAcceptedLink") { type = NavType.BoolType }
            )
        ) { entry ->
            val userId = entry.arguments?.getString("userId").orEmpty()
            val hasAcceptedLink = entry.arguments?.getBoolean("hasAcceptedLink") ?: false
            BiometricEnrollmentScreen(
                userId = userId,
                onEnabled = {
                    navigateAfterAuthentication(hasAcceptedLink, Screen.BiometricEnrollment.route)
                },
                onSkip = {
                    navigateAfterAuthentication(hasAcceptedLink, Screen.BiometricEnrollment.route)
                }
            )
        }

        composable(Screen.OnboardingLink.route) {
            val linkViewModel: DriverLinkViewModel = viewModel()
            OnboardingLinkScreen(
                viewModel = linkViewModel,
                onLinkAccepted = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.OnboardingLink.route) { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.createRoute()) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val ordersViewModel: OrdersViewModel = viewModel()
            val historyViewModel: HistoryViewModel = viewModel()
            val supportViewModel: SupportViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()

            DashboardScreen(
                ordersViewModel = ordersViewModel,
                historyViewModel = historyViewModel,
                supportViewModel = supportViewModel,
                profileViewModel = profileViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.createRoute()) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
