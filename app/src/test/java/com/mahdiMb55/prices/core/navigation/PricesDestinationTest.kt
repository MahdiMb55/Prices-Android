package com.mahdiMb55.prices.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PricesDestinationTest {
    @Test
    fun startupDestinationIsOnboarding() {
        assertEquals(PricesDestination.Onboarding.route, PricesDestination.startDestination)
    }

    @Test
    fun productDetailArgumentIsNullWhenMissingOrMalformed() {
        assertNull(PricesDestination.productIdOrNull(emptyMap<String, Any?>()))
        assertNull(PricesDestination.productIdOrNull(mapOf("productId" to " ")))
        assertNull(PricesDestination.productIdOrNull(mapOf("productId" to "bad/id")))
    }

    @Test
    fun productDetailRouteEncodesAValidIdentifier() {
        assertEquals(
            "product_detail/sku-1042",
            PricesDestination.productDetail("sku-1042")
        )
    }
}
