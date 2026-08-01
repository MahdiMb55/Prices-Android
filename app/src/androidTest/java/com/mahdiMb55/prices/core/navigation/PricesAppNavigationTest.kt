package com.mahdiMb55.prices.core.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import com.mahdiMb55.prices.app.PricesApp
import com.mahdiMb55.prices.core.designsystem.PricesTheme
import com.mahdiMb55.prices.core.appinfo.AppInfo
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.core.di.AppContainer
import com.mahdiMb55.prices.data.remote.NoTokenAccessTokenProvider
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import org.junit.Rule
import org.junit.Test

class PricesAppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startupShowsOnboardingAndConnectStoreOpensPairing() {
        setAppContent()

        composeRule.onNodeWithText("Connect Store").performClick()
        composeRule.onNodeWithText("Connect your store").assertIsDisplayed()
    }

    @Test
    fun developmentEntryReachesProductsAndProductEdit() {
        setAppContent()

        composeRule.onNodeWithText("Preview App Shell").performClick()
        composeRule.onNodeWithText("Products").assertIsDisplayed()
        composeRule.onNodeWithText("Wireless keyboard").performClick()
        composeRule.onNodeWithText("Product detail").assertIsDisplayed()
        composeRule.onNodeWithText("Edit price").performClick()
        composeRule.onNodeWithText("Price edit").assertIsDisplayed()
    }

    @Test
    fun productsOpensHistoryAndSettings() {
        setAppContent()

        composeRule.onNodeWithText("Preview App Shell").performClick()
        composeRule.onNodeWithText("Price history").performClick()
        composeRule.onNodeWithText("Price history").assertIsDisplayed()
    }

    @Test
    fun productsOpensSettings() {
        setAppContent()

        composeRule.onNodeWithText("Preview App Shell").performClick()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("9.8.7").assertIsDisplayed()
        composeRule.onNodeWithText("com.mahdiMb55.prices.test").assertIsDisplayed()
    }

    @Test
    fun detailBackNavigationReturnsToProducts() {
        setAppContent()

        composeRule.onNodeWithText("Preview App Shell").performClick()
        composeRule.onNodeWithText("Wireless keyboard").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Products").assertIsDisplayed()
    }

    @Test
    fun productsSearchOpensAndCloses() {
        setAppContent()

        composeRule.onNodeWithText("Preview App Shell").performClick()
        composeRule.onNodeWithContentDescription("Search products").performClick()
        composeRule.onNodeWithText("Search products").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close search").performClick()
        composeRule.onNodeWithContentDescription("Search products").assertIsDisplayed()
    }

    @Test
    fun rtlShellRendersWithoutCrashing() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PricesTheme(darkTheme = false, dynamicColor = false) {
                    PricesApp(appContainer = testAppContainer)
                }
            }
        }

        composeRule.onNodeWithText("Connect Store").assertIsDisplayed()
    }

    private fun setAppContent() {
        composeRule.setContent {
            PricesTheme(darkTheme = false, dynamicColor = false) {
                PricesApp(appContainer = testAppContainer)
            }
        }
    }

    private val testAppContainer = object : AppContainer {
        override val pricesApiFactory = PricesApiFactory(NoTokenAccessTokenProvider)
        override val appInfoProvider: AppInfoProvider = object : AppInfoProvider {
            override val appInfo = AppInfo(
                versionName = "9.8.7",
                versionCode = 987L,
                packageName = "com.mahdiMb55.prices.test",
                isDebugBuild = true
            )
        }
    }
}
