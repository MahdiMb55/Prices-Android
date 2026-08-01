package com.mahdiMb55.prices.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import retrofit2.Response
import org.junit.Test

class PricesApiFactoryTest {
    @Test
    fun validatedRuntimeBaseUrlCreatesDiscoveryClient() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody(DISCOVERY_JSON).setHeader("Content-Type", "application/json"))
            val baseUrl = (StoreUrlNormalizer.normalize(server.url("shop/").toString()) as StoreUrlNormalizationResult.Valid).baseUrl

            val response = PricesApiFactory(NoTokenAccessTokenProvider).create(baseUrl).discovery()

            assertEquals("prices/v1", response.body()?.apiVersion)
            assertEquals("/shop/wp-json/prices/v1/discovery", server.takeRequest().path)
            assertNull(server.takeRequest(1, java.util.concurrent.TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun serverErrorPassesThroughRetrofitForNetworkExecutor() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"error":{"code":"AUTHENTICATION_REQUIRED","message":"Authentication is required.","details":{},"request_id":"request-5"}}""")
            )
            val baseUrl = (StoreUrlNormalizer.normalize(server.url("/").toString()) as StoreUrlNormalizationResult.Valid).baseUrl
            val api = PricesApiFactory(NoTokenAccessTokenProvider).create(baseUrl)

            val result = NetworkRequestExecutor(ApiErrorParser()).execute { api.discovery() }

            assertEquals(NetworkErrorCategory.Authentication, (result as NetworkResult.Failure).error.category)
        }
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
              "features": {},
              "site_name": "Store",
              "site_url": "https://example.com",
              "currency": "IRR",
              "currency_format": "%s تومان"
            }
        """
    }
}
