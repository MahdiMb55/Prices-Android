package com.mahdiMb55.prices.data.local.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredConnectionTest {
    @Test
    fun discoveredSnapshotUsesSchemaAndContainsNoAuthenticationState() {
        val connection = StoredConnection(
            apiBaseUrl = "https://example.com/wp-json/prices/v1/",
            siteUrl = "https://example.com/",
            siteName = "Store",
            pluginName = "Prices for WooCommerce",
            pluginVersion = "1.0",
            apiVersion = "prices/v1",
            woocommerceAvailable = true,
            minimumAppVersion = null,
            minimumAppVersionWarning = null,
            currency = "IRR",
            currencyFormat = "%s تومان",
            featureFlags = mapOf("product_list" to true),
            authenticationCapabilities = AuthenticationCapabilitiesSnapshot(true, true, true),
            discoveredAtEpochMillis = 1L
        )

        assertEquals(ConnectionPhase.Discovered, connection.phase)
        assertEquals(StoredConnection.CurrentSchemaVersion, connection.schemaVersion)
        assertTrue(connection.featureFlags.getValue("product_list"))
    }
}
