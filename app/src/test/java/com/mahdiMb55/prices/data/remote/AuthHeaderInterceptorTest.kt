package com.mahdiMb55.prices.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthHeaderInterceptorTest {
    @Test
    fun noTokenDoesNotAddAuthorizationHeader() = withServer { server ->
        val client = clientFor(InMemoryAccessTokenProvider(null))

        execute(client, server)

        assertFalse(server.takeRequest().headers.names().contains("Authorization"))
    }

    @Test
    fun blankTokenDoesNotAddAuthorizationHeader() = withServer { server ->
        val client = clientFor(InMemoryAccessTokenProvider("  "))

        execute(client, server)

        assertFalse(server.takeRequest().headers.names().contains("Authorization"))
    }

    @Test
    fun tokenAddsBearerAuthorizationHeader() = withServer { server ->
        val client = clientFor(InMemoryAccessTokenProvider("device-token"))

        execute(client, server)

        assertEquals("Bearer device-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun existingAuthorizationHeaderIsPreserved() = withServer { server ->
        val client = clientFor(InMemoryAccessTokenProvider("replacement-token"))

        execute(client, server, "Bearer caller-token")

        assertEquals("Bearer caller-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun inMemoryProviderUpdatesWithoutExposingTokenInToString() {
        val provider = InMemoryAccessTokenProvider("initial-token")

        provider.update("new-token")

        assertEquals("new-token", provider.currentToken())
        assertFalse(provider.toString().contains("new-token"))
    }

    private fun withServer(block: (MockWebServer) -> Unit) {
        MockWebServer().use { server ->
            server.start()
            block(server)
        }
    }

    private fun clientFor(provider: AccessTokenProvider): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(AuthHeaderInterceptor(provider)).build()

    private fun execute(client: OkHttpClient, server: MockWebServer, authorization: String? = null) {
        server.enqueue(MockResponse().setResponseCode(204))
        val request = Request.Builder().url(server.url("/")).apply {
            if (authorization != null) header("Authorization", authorization)
        }.build()
        client.newCall(request).execute().close()
    }
}
