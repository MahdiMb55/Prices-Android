package com.mahdiMb55.prices.data.remote

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun discoveryResponseParsesKnownFieldsAndIgnoresUnknownFields() {
        val response = json.decodeFromString<DiscoveryResponseDto>(DISCOVERY_JSON)

        assertEquals("Prices for WooCommerce", response.pluginName)
        assertEquals("پرایسیس", response.siteName)
        assertEquals(null, response.minimumAppVersion)
        assertTrue(response.features.getValue("product_search"))
        assertTrue(response.authentication.deviceTokens)
    }

    @Test(expected = SerializationException::class)
    fun discoveryResponseRequiresApiVersion() {
        json.decodeFromString<DiscoveryResponseDto>(DISCOVERY_JSON.replace("\"api_version\": \"prices/v1\",", ""))
    }

    private companion object {
        const val DISCOVERY_JSON = """
            {
              "plugin_name": "Prices for WooCommerce",
              "plugin_version": "1.2.0",
              "api_version": "prices/v1",
              "woocommerce_available": true,
              "minimum_app_version": null,
              "authentication_methods": ["pairing_code", "device_token"],
              "authentication": {
                "pairing_codes": true,
                "qr_pairing": true,
                "device_tokens": true,
                "application_passwords": false,
                "oauth": false,
                "custom_access_refresh_tokens": false
              },
              "features": { "product_search": true },
              "site_name": "پرایسیس",
              "site_url": "https://example.com",
              "currency": "IRR",
              "currency_format": "%s تومان",
              "unknown_future_field": "ignored"
            }
        """
    }
}
