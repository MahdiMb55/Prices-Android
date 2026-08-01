package com.mahdiMb55.prices.feature.sample

import androidx.annotation.StringRes
import com.mahdiMb55.prices.R

data class SampleProduct(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:StringRes val skuRes: Int,
    @get:StringRes val priceRes: Int,
    @get:StringRes val statusRes: Int
)

val sampleProducts = listOf(
    SampleProduct("sku-1042", R.string.sample_wireless_keyboard, R.string.sample_sku_keyboard, R.string.sample_price_keyboard, R.string.sample_in_stock),
    SampleProduct("sku-2088", R.string.sample_usb_c_hub, R.string.sample_sku_hub, R.string.sample_price_hub, R.string.sample_low_stock)
)
