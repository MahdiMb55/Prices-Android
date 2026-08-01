package com.mahdiMb55.prices.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahdiMb55.prices.R
import com.mahdiMb55.prices.core.designsystem.PricesSpacing
import com.mahdiMb55.prices.core.designsystem.PricesTheme
import com.mahdiMb55.prices.core.di.LocalAppContainer
import com.mahdiMb55.prices.core.navigation.PricesTopAppBar

@Composable
fun SettingsRoute(onBack: () -> Unit, onDisconnect: () -> Unit) {
    val appInfoProvider = LocalAppContainer.current.appInfoProvider
    val factory = remember(appInfoProvider) { SettingsViewModelFactory(appInfoProvider) }
    val viewModel: SettingsViewModel = viewModel(factory = factory)

    SettingsScreen(uiState = viewModel.uiState, onBack = onBack, onDisconnect = onDisconnect)
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onDisconnect: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            PricesTopAppBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.navigate_back)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PricesSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PricesSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_description),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.application_information),
                style = MaterialTheme.typography.titleMedium
            )
            AppInfoValue(
                label = stringResource(R.string.app_version),
                value = uiState.versionName
            )
            AppInfoValue(
                label = stringResource(R.string.version_code),
                value = uiState.versionCode.toString()
            )
            AppInfoValue(
                label = stringResource(R.string.package_name),
                value = uiState.packageName
            )
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.disconnect_app_session))
            }
        }
    }
}

@Composable
private fun AppInfoValue(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PricesSpacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PricesTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreen(
            uiState = SettingsUiState(
                versionName = "1.0",
                versionCode = 1L,
                packageName = "com.mahdiMb55.prices",
                isDebugBuild = true
            ),
            onBack = {}
        )
    }
}
