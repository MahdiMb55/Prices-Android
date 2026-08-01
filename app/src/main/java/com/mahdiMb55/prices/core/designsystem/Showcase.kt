package com.mahdiMb55.prices.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private data class ShowcaseProduct(
    val title: String,
    val sku: String,
    val price: String,
    val status: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DesignSystemShowcase(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val products = remember {
        listOf(
            ShowcaseProduct("Wireless keyboard", "SKU-1042", "$49.00", "In stock"),
            ShowcaseProduct("USB-C hub", "SKU-2088", "$29.00", "Low stock")
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Design system showcase") }) }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(PricesSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PricesSpacing.md)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search products") }
                )
            }
            items(products, key = { it.sku }) { product ->
                ShowcaseProductRow(product)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PricesSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current price", style = PricesTypography.labelSmall)
                    Text("$49.00", style = PricesTypography.titleMedium, fontWeight = FontWeight.Bold)
                    AssistChip(onClick = {}, label = { Text("Ready") })
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PricesSpacing.sm)) {
                    Button(onClick = {}) { Text("Save price") }
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            item {
                Text(
                    text = "Sync failed",
                    color = MaterialTheme.colorScheme.error,
                    style = PricesTypography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, PricesShapes.small)
                        .padding(PricesSpacing.md)
                )
            }
        }
    }
}

@Composable
private fun ShowcaseProductRow(product: ShowcaseProduct) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.title, style = PricesTypography.titleMedium)
                Spacer(Modifier.height(PricesSpacing.xs))
                Text("${product.sku} · ${product.status}", style = PricesTypography.bodyMedium)
            }
            Text(product.price, style = PricesTypography.titleMedium, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(modifier = Modifier.padding(top = PricesSpacing.md))
    }
}

@Preview(showBackground = true, name = "Prices light LTR")
@Composable
private fun LightShowcasePreview() {
    PricesTheme(darkTheme = false, dynamicColor = false) {
        DesignSystemShowcase()
    }
}

@Preview(showBackground = true, name = "Prices dark RTL", locale = "fa")
@Composable
private fun DarkShowcasePreview() {
    PricesTheme(darkTheme = true, dynamicColor = false) {
        DesignSystemShowcase()
    }
}
