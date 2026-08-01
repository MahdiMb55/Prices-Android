package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.core.appinfo.AppInfo
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import com.mahdiMb55.prices.data.remote.ApiErrorParser
import com.mahdiMb55.prices.data.remote.NetworkRequestExecutor
import com.mahdiMb55.prices.data.remote.NoTokenAccessTokenProvider
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreDiscoveryRepositoryTest {
    @Test
    fun validDiscoveryPersistsOnlySafeDiscoveredSnapshot() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(discoveryJson()))
            val preferences = FakeConnectionPreferences()
            val result = repository(preferences).discover(server.url("/").toString().removeSuffix("/"))

            assertEquals("/wp-json/prices/v1/discovery", server.takeRequest().path)
            assertEquals("Example Store", (result as StoreDiscoveryResult.Success).connection.siteName)
            assertEquals("Example Store", preferences.value.value?.siteName)
        }
    }

    @Test
    fun unsupportedApiDoesNotOverwriteExistingSnapshot() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(discoveryJson(apiVersion = "prices/v2")))
            val preferences = FakeConnectionPreferences()
            val result = repository(preferences).discover(server.url("/").toString().removeSuffix("/"))

            assertEquals(StoreDiscoveryFailure.UnsupportedApiVersion, (result as StoreDiscoveryResult.Failure).reason)
            assertNull(preferences.value.value)
        }
    }

    private fun repository(preferences: ConnectionPreferences) = DefaultStoreDiscoveryRepository(
        PricesApiFactory(NoTokenAccessTokenProvider), NetworkRequestExecutor(ApiErrorParser()), preferences,
        object : AppInfoProvider { override val appInfo = AppInfo("1.0", 1, "com.mahdiMb55.prices", true) },
        now = { 10L }
    )

    private fun discoveryJson(apiVersion: String = "prices/v1") = """{
        "plugin_name":"Prices","plugin_version":"1.0","api_version":"$apiVersion",
        "woocommerce_available":true,"minimum_app_version":null,"authentication_methods":["pairing_codes"],
        "authentication":{"pairing_codes":true,"qr_pairing":false,"device_tokens":false,
        "application_passwords":false,"oauth":false,"custom_access_refresh_tokens":false},
        "features":{"products":true},"site_name":"Example Store","site_url":"https://example.test/",
        "currency":"USD","currency_format":"%s","unknown_future_field":"ignored"
    }"""

    private class FakeConnectionPreferences : ConnectionPreferences {
        val value = MutableStateFlow<StoredConnection?>(null)
        override val connection: Flow<StoredConnection?> = value
        override suspend fun save(connection: StoredConnection) { value.value = connection }
        override suspend fun clear() { value.value = null }
    }
}
