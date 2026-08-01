package com.mahdiMb55.prices.feature.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahdiMb55.prices.R
import com.mahdiMb55.prices.core.designsystem.PricesSpacing
import com.mahdiMb55.prices.core.navigation.PricesTopAppBar
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import com.mahdiMb55.prices.data.repository.StoreDiscoveryFailure
import com.mahdiMb55.prices.data.repository.PairingCodeFormat
import com.mahdiMb55.prices.data.repository.PairingFailure
import com.mahdiMb55.prices.feature.onboarding.OnboardingViewModel
import com.mahdiMb55.prices.feature.onboarding.OnboardingViewModelFactory
import com.mahdiMb55.prices.feature.pairing.PairingViewModel
import com.mahdiMb55.prices.feature.pairing.PairingViewModelFactory
import com.mahdiMb55.prices.feature.pairing.PairingUiState

@Composable
fun OnboardingRoute(
    factory: OnboardingViewModelFactory,
    onDiscovered: () -> Unit
) {
    val viewModel: OnboardingViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.discoveryCompleted) {
        if (state.discoveryCompleted) onDiscovered()
    }
    OnboardingScreen(
        state = state,
        onStoreUrlChanged = viewModel::updateStoreUrl,
        onDiscover = viewModel::discover
    )
}

@Composable
private fun OnboardingScreen(
    state: com.mahdiMb55.prices.feature.onboarding.OnboardingUiState,
    onStoreUrlChanged: (String) -> Unit,
    onDiscover: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PricesSpacing.xl),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(PricesSpacing.sm))
        Text(stringResource(R.string.store_discovery_description), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PricesSpacing.xl))
        OutlinedTextField(
            value = state.storeUrl,
            onValueChange = onStoreUrlChanged,
            label = { Text(stringResource(R.string.store_url)) },
            singleLine = true,
            isError = state.error != null,
            supportingText = state.error?.let { { Text(discoveryErrorText(it)) } },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr)
        )
        Spacer(Modifier.height(PricesSpacing.md))
        Button(
            onClick = onDiscover,
            enabled = !state.isDiscovering,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isDiscovering) CircularProgressIndicator(modifier = Modifier.height(18.dp))
            else Text(stringResource(R.string.discover_store))
        }
    }
}

@Composable
fun PairingRoute(
    factory: PairingViewModelFactory,
    onChangeStore: () -> Unit,
    onSuccess: () -> Unit
) {
    val viewModel: PairingViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.success) { if (state.success) { viewModel.consumeSuccess(); onSuccess() } }
    PairingScreen(state, viewModel::updateCode, viewModel::updateDeviceName, viewModel::submit, onChangeStore)
}

@Composable
private fun PairingScreen(
    state: PairingUiState,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onChangeStore: () -> Unit
) {
    Scaffold(topBar = { PricesTopAppBar(title = stringResource(R.string.manual_pairing_title)) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(PricesSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PricesSpacing.md)
        ) {
            Text(stringResource(R.string.pairing_code_help), style = MaterialTheme.typography.bodyLarge)
            state.connection?.let { connection ->
                Text(connection.siteName, style = MaterialTheme.typography.titleLarge)
                Text(connection.siteUrl, style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr))
            }
            Text(stringResource(R.string.discovery_status_discovered), style = MaterialTheme.typography.labelLarge)
            if (state.connection?.minimumAppVersionWarning != null) {
                Text(stringResource(R.string.minimum_version_warning), color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(value = PairingCodeFormat.display(state.pairingCode), onValueChange = onCodeChange,
                label = { Text(stringResource(R.string.pairing_code)) }, singleLine = true,
                isError = state.failure == PairingFailure.InvalidCodeFormat || state.failure == PairingFailure.InvalidOrExpiredCode,
                textStyle = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.deviceName, onValueChange = onNameChange, label = { Text(stringResource(R.string.device_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            state.failure?.let { Text(pairingErrorText(it), color = MaterialTheme.colorScheme.error) }
            Button(onClick = onSubmit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
                if (state.isSubmitting) CircularProgressIndicator(modifier = Modifier.height(18.dp)) else Text(stringResource(R.string.connect_device))
            }
            OutlinedButton(onClick = onChangeStore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.change_store))
            }
        }
    }
}

@Composable
private fun pairingErrorText(failure: PairingFailure): String = stringResource(when (failure) {
    PairingFailure.InvalidCodeFormat -> R.string.pairing_error_format
    PairingFailure.InvalidOrExpiredCode -> R.string.pairing_error_invalid_or_expired
    PairingFailure.TooManyAttempts -> R.string.pairing_error_too_many_attempts
    PairingFailure.PairingDisabled -> R.string.pairing_error_disabled
    PairingFailure.DeviceLimitReached -> R.string.pairing_error_device_limit
    PairingFailure.PermissionDenied -> R.string.pairing_error_permission
    PairingFailure.NetworkUnavailable -> R.string.pairing_error_network
    PairingFailure.Timeout -> R.string.pairing_error_timeout
    PairingFailure.TlsFailure -> R.string.pairing_error_tls
    PairingFailure.ServerError -> R.string.pairing_error_server
    PairingFailure.InvalidResponse -> R.string.pairing_error_response
    PairingFailure.VerificationFailed -> R.string.pairing_error_verification
    PairingFailure.SecureSessionSaveFailed -> R.string.pairing_error_secure_session_save
    else -> R.string.pairing_error_generic
})

@Composable
private fun discoveryErrorText(failure: StoreDiscoveryFailure): String = stringResource(
    when (failure) {
        StoreDiscoveryFailure.InvalidUrl -> R.string.discovery_error_invalid_url
        StoreDiscoveryFailure.WooCommerceUnavailable -> R.string.discovery_error_woocommerce
        StoreDiscoveryFailure.UnsupportedApiVersion -> R.string.discovery_error_api_version
        StoreDiscoveryFailure.AppUpdateRequired -> R.string.discovery_error_update_required
        StoreDiscoveryFailure.PairingUnavailable -> R.string.discovery_error_pairing_unavailable
        else -> R.string.discovery_error_generic
    }
)
