package com.mahdiMb55.prices.feature.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.mahdiMb55.prices.R
import com.mahdiMb55.prices.core.navigation.PricesTopAppBar
import com.mahdiMb55.prices.core.designsystem.PricesSpacing
import com.mahdiMb55.prices.feature.sample.SampleProduct
import com.mahdiMb55.prices.feature.sample.sampleProducts

@Composable
fun OnboardingScreen(
    onConnectStore: () -> Unit,
    onPreviewAppShell: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PricesSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(PricesSpacing.sm))
        Text(stringResource(R.string.onboarding_description), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PricesSpacing.xl))
        Button(onClick = onConnectStore, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.connect_store))
        }
        Spacer(Modifier.height(PricesSpacing.sm))
        OutlinedButton(onClick = onPreviewAppShell, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.preview_app_shell))
        }
    }
}

@Composable
fun PairingScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            PricesTopAppBar(
                title = stringResource(R.string.pairing_title),
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
            Text(stringResource(R.string.pairing_description), style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.manual_code_coming_soon))
            }
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.qr_scan_coming_soon))
            }
        }
    }
}

@Composable
fun ProductsScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onPriceHistory: () -> Unit,
    onSettings: () -> Unit
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = searchOpen) { searchOpen = false }

    val visibleProducts = sampleProducts.filter { product ->
        query.isBlank() || stringResource(product.titleRes).contains(query, ignoreCase = true) ||
            stringResource(product.skuRes).contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            PricesTopAppBar(
                title = stringResource(R.string.products_title),
                onSearch = { searchOpen = true },
                onHistory = onPriceHistory,
                onSettings = onSettings,
                searchOpen = searchOpen,
                searchQuery = query,
                searchHint = stringResource(R.string.search_products),
                onSearchQueryChange = { query = it },
                onCloseSearch = { searchOpen = false },
                searchContentDescription = stringResource(R.string.search_products),
                historyContentDescription = stringResource(R.string.price_history),
                settingsContentDescription = stringResource(R.string.settings),
                closeSearchContentDescription = stringResource(R.string.close_search)
            )
        }
    ) { padding ->
        if (visibleProducts.isEmpty()) {
            EmptyProductsState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(PricesSpacing.lg)
            ) {
                item {
                    Text(
                        stringResource(R.string.development_sample_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = PricesSpacing.sm)
                    )
                }
                items(visibleProducts, key = { it.id }) { product ->
                    ProductRow(product = product, onClick = { onProductClick(product.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyProductsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PricesSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.products_empty), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ProductRow(product: SampleProduct, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = PricesSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(product.titleRes), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(product.skuRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(product.statusRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(product.priceRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = PricesSpacing.md))
    }
}

@Composable
fun ProductDetailScreen(
    productId: String?,
    onBack: () -> Unit,
    onEditPrice: (String) -> Unit
) {
    Scaffold(
        topBar = {
            PricesTopAppBar(
                title = stringResource(R.string.product_detail_title),
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
            if (productId == null) {
                Text(stringResource(R.string.invalid_product_id), color = MaterialTheme.colorScheme.error)
            } else {
                Text(stringResource(R.string.sample_wireless_keyboard), style = MaterialTheme.typography.headlineSmall)
                Text(productId, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.sample_price_keyboard), style = MaterialTheme.typography.titleMedium)
                Button(onClick = { onEditPrice(productId) }) {
                    Text(stringResource(R.string.edit_price))
                }
            }
        }
    }
}

@Composable
fun PriceEditScreen(productId: String?, onBack: () -> Unit) {
    val initialPrice = stringResource(R.string.sample_price_value)
    var price by rememberSaveable { mutableStateOf(initialPrice) }
    Scaffold(
        topBar = {
            PricesTopAppBar(
                title = stringResource(R.string.price_edit_title),
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
            Text(productId ?: stringResource(R.string.invalid_product_id), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(R.string.price_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.price_edit_preview_notice), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {}, enabled = false) {
                Text(stringResource(R.string.save_price_coming_soon))
            }
        }
    }
}

@Composable
fun PriceHistoryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            PricesTopAppBar(
                title = stringResource(R.string.price_history_title),
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
            Text(stringResource(R.string.price_history_empty), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
