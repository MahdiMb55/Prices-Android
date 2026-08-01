package com.mahdiMb55.prices.core.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PricesDestination {
    const val startDestination = "onboarding"

    object Onboarding {
        const val route = "onboarding"
    }

    object Pairing {
        const val route = "pairing"
    }

    object Products {
        const val route = "products"
    }

    object ProductDetail {
        const val route = "product_detail/{productId}"
        const val argument = "productId"
    }

    object PriceEdit {
        const val route = "price_edit/{productId}"
        const val argument = "productId"
    }

    object PriceHistory {
        const val route = "price_history"
    }

    object Settings {
        const val route = "settings"
    }

    fun productDetail(productId: String): String =
        "product_detail/${encodeSegment(productId)}"

    fun priceEdit(productId: String): String =
        "price_edit/${encodeSegment(productId)}"

    fun productIdOrNull(arguments: Map<String, *>): String? {
        val productId = arguments[ProductDetail.argument]?.toString()?.trim()
        return productId?.takeUnless { it.isBlank() || '/' in it }
    }

    private fun encodeSegment(value: String): String =
        URLEncoder.encode(value.trim(), StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
}
