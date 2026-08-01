package com.mahdiMb55.prices.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryCompatibilityTest {
    @Test
    fun hostOnlyInputReceivesHttpsWithoutChangingStrictNormalizerPolicy() {
        assertEquals("https://example.com", StoreUrlInput.prepare("example.com"))
        assertEquals("https://example.com/shop", StoreUrlInput.prepare("https://example.com/shop"))
    }

    @Test
    fun apiPolicyAcceptsOnlyConfirmedPricesV1Format() {
        assertEquals(ApiVersionCompatibility.Supported, ApiVersionPolicy.check("prices/v1"))
        assertEquals(ApiVersionCompatibility.Unsupported, ApiVersionPolicy.check("prices/v2"))
        assertEquals(ApiVersionCompatibility.Malformed, ApiVersionPolicy.check("1.0"))
    }

    @Test
    fun minimumAppVersionIsNumericAndMalformedValueRemainsObservable() {
        assertEquals(MinimumAppVersionCheck.Allowed, MinimumAppVersionPolicy.check(null, "1.0"))
        assertEquals(MinimumAppVersionCheck.UpdateRequired, MinimumAppVersionPolicy.check("1.1", "1.0"))
        assertEquals(MinimumAppVersionCheck.Allowed, MinimumAppVersionPolicy.check("1.0.0", "1.0"))
        assertEquals(MinimumAppVersionCheck.Allowed, MinimumAppVersionPolicy.check("1.0", "1.1"))
        assertEquals(
            MinimumAppVersionCheck.Malformed("next-release"),
            MinimumAppVersionPolicy.check("next-release", "1.0")
        )
    }
}
