package com.juragan38.pos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.screen.dashboard.DashboardScreen
import com.juragan38.pos.ui.screen.login.LoginScreen
import com.juragan38.pos.ui.screen.pos.PosScreen
import com.juragan38.pos.ui.screen.products.ProductsScreen
import com.juragan38.pos.ui.screen.reports.ReportsScreen
import com.juragan38.pos.ui.screen.splash.SplashScreen
import com.juragan38.pos.ui.screen.transactions.TransactionsScreen
import com.juragan38.pos.ui.screen.users.UsersScreen
import kotlinx.coroutines.delay

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val POS = "pos"
    const val PRODUCTS = "products"
    const val REPORTS = "reports"
    const val TRANSACTIONS = "transactions"
    const val USERS = "users"
}

@Composable
fun JuraganNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val session by viewModel.session.collectAsState()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            val currentSession by rememberUpdatedState(session)
            LaunchedEffect(Unit) {
                delay(1200)
                val target = if (currentSession == null) Routes.LOGIN else Routes.DASHBOARD
                navController.navigate(target) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
            SplashScreen()
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onLoggedIn = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onOpenPos = { navController.navigate(Routes.POS) },
                onOpenProducts = { navController.navigate(Routes.PRODUCTS) },
                onOpenReports = { navController.navigate(Routes.REPORTS) },
                onOpenTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                onOpenUsers = { navController.navigate(Routes.USERS) },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.POS) {
            PosScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.PRODUCTS) {
            ProductsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.REPORTS) {
            ReportsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.TRANSACTIONS) {
            TransactionsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.USERS) {
            UsersScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
