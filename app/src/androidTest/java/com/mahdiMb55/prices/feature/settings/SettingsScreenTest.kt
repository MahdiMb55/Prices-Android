package com.mahdiMb55.prices.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mahdiMb55.prices.core.designsystem.PricesTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsScreenRendersInjectedApplicationInformation() {
        composeRule.setContent {
            PricesTheme(darkTheme = false, dynamicColor = false) {
                SettingsScreen(
                    uiState = SettingsUiState(
                        versionName = "3.2.1",
                        versionCode = 321L,
                        packageName = "com.mahdiMb55.prices",
                        isDebugBuild = true
                    ),
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("3.2.1").assertIsDisplayed()
        composeRule.onNodeWithText("321").assertIsDisplayed()
        composeRule.onNodeWithText("com.mahdiMb55.prices").assertIsDisplayed()
    }
}
