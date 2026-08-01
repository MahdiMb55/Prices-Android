package com.mahdiMb55.prices.feature.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mahdiMb55.prices.R
import com.mahdiMb55.prices.core.designsystem.PricesSpacing

@Composable
fun StartupLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PricesSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(PricesSpacing.md),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.startup_verifying), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun StartupVerificationScreen(
    storeName: String,
    storageFailure: Boolean,
    onRetry: () -> Unit,
    onReturnToPairing: () -> Unit,
    onChangeStore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PricesSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(PricesSpacing.md),
    ) {
        Text(storeName, style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(
                if (storageFailure) R.string.startup_storage_failure
                else R.string.startup_verification_failure,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.retry))
        }
        OutlinedButton(onClick = onReturnToPairing, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.return_to_pairing))
        }
        OutlinedButton(onClick = onChangeStore, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.change_store))
        }
    }
}
