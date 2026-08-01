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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mahdiMb55.prices.core.di.AppContainer
import com.mahdiMb55.prices.core.di.LocalAppContainer
import com.mahdiMb55.prices.core.navigation.PricesDestination
import com.mahdiMb55.prices.feature.connection.PairingRoute
import com.mahdiMb55.prices.feature.connection.OnboardingRoute
import com.mahdiMb55.prices.feature.onboarding.OnboardingViewModelFactory
import com.mahdiMb55.prices.feature.startup.StartupDestination
import com.mahdiMb55.prices.feature.startup.StartupUiState
import com.mahdiMb55.prices.feature.startup.StartupViewModel
import com.mahdiMb55.prices.feature.startup.StartupViewModelFactory
import com.mahdiMb55.prices.feature.pairing.PairingViewModelFactory
import com.mahdiMb55.prices.data.session.SessionState
import com.mahdiMb55.prices.feature.shell.PriceEditScreen
import com.mahdiMb55.prices.feature.shell.PriceHistoryScreen
import com.mahdiMb55.prices.feature.shell.ProductDetailScreen
import com.mahdiMb55.prices.feature.shell.ProductsScreen
import com.mahdiMb55.prices.feature.settings.SettingsRoute
import kotlinx.coroutines.launch

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
            composable(PricesDestination.Startup.route) {
                val startupViewModel: StartupViewModel = viewModel(
                    factory = StartupViewModelFactory(appContainer.connectionPreferences)
                )
                val startupState by startupViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(startupState) {
                    when (val state = startupState) {
                        StartupUiState.Loading -> Unit
                        is StartupUiState.Ready -> when (state.destination) {
                            StartupDestination.Onboarding -> navController.navigate(PricesDestination.Onboarding.route) {
                                popUpTo(PricesDestination.Startup.route) { inclusive = true }
                            }
                            StartupDestination.Pairing -> navController.navigate(PricesDestination.Pairing.route) {
                                popUpTo(PricesDestination.Startup.route) { inclusive = true }
                            }
                        }
                    }
                }
            }
            composable(PricesDestination.Onboarding.route) {
                OnboardingRoute(
                    factory = OnboardingViewModelFactory(appContainer.storeDiscoveryRepository),
                    onDiscovered = {
                        navController.navigate(PricesDestination.Pairing.route) {
                            popUpTo(PricesDestination.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(PricesDestination.Pairing.route) {
                val connection by appContainer.connectionPreferences.connection.collectAsStateWithLifecycle(initialValue = null)
                val scope = rememberCoroutineScope()
                connection?.let { discovered ->
                    PairingRoute(
                        factory = PairingViewModelFactory(appContainer.pairingRepository, discovered),
                        onChangeStore = {
                            scope.launch {
                                appContainer.pairingRepository.clearForStoreChange()
                                navController.navigate(PricesDestination.Onboarding.route) {
                                    popUpTo(PricesDestination.Pairing.route) { inclusive = true }
                                }
                            }
                        },
                        onSuccess = { navController.navigate(PricesDestination.Products.route) { popUpTo(PricesDestination.Pairing.route) { inclusive = true } } }
                    )
                }
            }
            composable(PricesDestination.Products.route) {
                val session by appContainer.sessionStore.state.collectAsStateWithLifecycle()
                if (session !is SessionState.Authenticated) {
                    LaunchedEffect(session) { navController.navigate(PricesDestination.Pairing.route) { popUpTo(PricesDestination.Products.route) { inclusive = true } } }
                } else ProductsScreen(
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
                val scope = rememberCoroutineScope()
                SettingsRoute(
                    onBack = { navController.popBackStack() },
                    onDisconnect = {
                        scope.launch {
                            appContainer.pairingRepository.clearSession()
                            navController.navigate(PricesDestination.Pairing.route) {
                                popUpTo(PricesDestination.Products.route) { inclusive = true }
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun AnimatedContentTransitionScope<*>.pricesEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(200))

private fun AnimatedContentTransitionScope<*>.pricesExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(180))
