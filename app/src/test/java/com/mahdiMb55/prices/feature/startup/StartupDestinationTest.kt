package com.mahdiMb55.prices.feature.startup

import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupDestinationTest {
    @Test
    fun absentSnapshotStartsOnboarding() {
        assertEquals(StartupDestination.Onboarding, StartupDestination.from(null))
    }

    @Test
    fun discoveredSnapshotStartsPairing() {
        assertEquals(StartupDestination.Pairing, StartupDestination.from(discoveredConnection()))
    }

    private fun discoveredConnection() = StoredConnection(
        apiBaseUrl = "https://example.test/wp-json/prices/v1/",
        siteUrl = "https://example.test/",
        siteName = "Example Store",
        pluginName = "Prices",
        pluginVersion = "1.0",
        apiVersion = "prices/v1",
        woocommerceAvailable = true,
        minimumAppVersion = null,
        minimumAppVersionWarning = null,
        currency = "USD",
        currencyFormat = "%s",
        featureFlags = emptyMap(),
        authenticationCapabilities = AuthenticationCapabilitiesSnapshot(true, false, false),
        discoveredAtEpochMillis = 1L
    )
}
