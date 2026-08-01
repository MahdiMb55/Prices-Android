package com.mahdiMb55.prices.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mahdiMb55.prices.core.di.AppContainer
import com.mahdiMb55.prices.core.di.LocalAppContainer
import com.mahdiMb55.prices.core.navigation.PricesDestination
import com.mahdiMb55.prices.feature.shell.OnboardingScreen
import com.mahdiMb55.prices.feature.shell.PairingScreen
import com.mahdiMb55.prices.feature.shell.PriceEditScreen
import com.mahdiMb55.prices.feature.shell.PriceHistoryScreen
import com.mahdiMb55.prices.feature.shell.ProductDetailScreen
import com.mahdiMb55.prices.feature.shell.ProductsScreen
import com.mahdiMb55.prices.feature.settings.SettingsRoute

@Composable
fun PricesApp(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    CompositionLocalProvider(LocalAppContainer provides appContainer) {
        NavHost(
            navController = navController,
            startDestination = PricesDestination.startDestination,
            enterTransition = { pricesEnterTransition() },
            exitTransition = { pricesExitTransition() },
            popEnterTransition = { pricesEnterTransition() },
            popExitTransition = { pricesExitTransition() }
        ) {
            composable(PricesDestination.Onboarding.route) {
                OnboardingScreen(
                    onConnectStore = { navController.navigate(PricesDestination.Pairing.route) },
                    onPreviewAppShell = { navController.navigate(PricesDestination.Products.route) }
                )
            }
            composable(PricesDestination.Pairing.route) {
                PairingScreen(onBack = { navController.popBackStack() })
            }
            composable(PricesDestination.Products.route) {
                ProductsScreen(
                    onBack = { navController.popBackStack() },
                    onProductClick = { id -> navController.navigate(PricesDestination.productDetail(id)) },
                    onPriceHistory = { navController.navigate(PricesDestination.PriceHistory.route) },
                    onSettings = { navController.navigate(PricesDestination.Settings.route) }
                )
            }
            composable(
                route = PricesDestination.ProductDetail.route,
                arguments = listOf(navArgument(PricesDestination.ProductDetail.argument) {
                    type = NavType.StringType
                })
            ) { entry ->
                ProductDetailScreen(
                    productId = PricesDestination.productIdOrNull(
                        mapOf(PricesDestination.ProductDetail.argument to entry.arguments?.getString(PricesDestination.ProductDetail.argument))
                    ),
                    onBack = { navController.popBackStack() },
                    onEditPrice = { id -> navController.navigate(PricesDestination.priceEdit(id)) }
                )
            }
            composable(
                route = PricesDestination.PriceEdit.route,
                arguments = listOf(navArgument(PricesDestination.PriceEdit.argument) {
                    type = NavType.StringType
                })
            ) { entry ->
                PriceEditScreen(
                    productId = PricesDestination.productIdOrNull(
                        mapOf(PricesDestination.PriceEdit.argument to entry.arguments?.getString(PricesDestination.PriceEdit.argument))
                    ),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(PricesDestination.PriceHistory.route) {
                PriceHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(PricesDestination.Settings.route) {
                SettingsRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun AnimatedContentTransitionScope<*>.pricesEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(200))

private fun AnimatedContentTransitionScope<*>.pricesExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(180))
