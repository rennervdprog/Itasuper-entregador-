package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.auth.AuthViewModel
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
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.OnboardingLink.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    val hasLink = authViewModel.hasAcceptedLink.value
                    if (hasLink) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.OnboardingLink.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.OnboardingLink.route) { inclusive = true }
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
