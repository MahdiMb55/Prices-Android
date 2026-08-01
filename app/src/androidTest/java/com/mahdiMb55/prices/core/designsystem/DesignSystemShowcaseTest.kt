package com.mahdiMb55.prices.core.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import org.junit.Rule
import org.junit.Test

class DesignSystemShowcaseTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showcaseRendersInLightTheme() {
        composeRule.setContent {
            PricesTheme(darkTheme = false, dynamicColor = false) {
                DesignSystemShowcase()
            }
        }

        composeRule.onNodeWithText("Design system showcase").assertIsDisplayed()
        composeRule.onNodeWithText("Wireless keyboard").assertIsDisplayed()
        composeRule.onNodeWithText("Save price").assertIsDisplayed()
    }

    @Test
    fun showcaseRendersInDarkThemeAndRtl() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PricesTheme(darkTheme = true, dynamicColor = false) {
                    DesignSystemShowcase()
                }
            }
        }

        composeRule.onNodeWithText("Design system showcase").assertIsDisplayed()
        composeRule.onNodeWithText("Search products").assertIsDisplayed()
        composeRule.onNodeWithText("Sync failed").assertIsDisplayed()
    }
}
