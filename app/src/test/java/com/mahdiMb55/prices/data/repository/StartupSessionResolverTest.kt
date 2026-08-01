package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import com.mahdiMb55.prices.data.local.session.MetadataPersistenceResult
import com.mahdiMb55.prices.data.local.session.PairedSessionMetadataPreferences
import com.mahdiMb55.prices.data.local.session.StoredPairedSessionMetadata
import com.mahdiMb55.prices.data.remote.ApiErrorParser
import com.mahdiMb55.prices.data.remote.InMemoryAccessTokenProvider
import com.mahdiMb55.prices.data.remote.NetworkRequestExecutor
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import com.mahdiMb55.prices.data.security.SecureTokenReadResult
import com.mahdiMb55.prices.data.security.SecureTokenStorage
import com.mahdiMb55.prices.data.security.SecureTokenWriteResult
import com.mahdiMb55.prices.data.session.InMemorySessionStore
import com.mahdiMb55.prices.data.session.PairedSession
import com.mahdiMb55.prices.data.session.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupSessionResolverTest {
    @Test fun noConnectionClearsOrphansAndResolvesOnboarding() = runTest {
        val cleanup = StartupFakeSecureSessionRepository()
        val resolver = resolver(connection = null, cleanup = cleanup)
        assertSame(StartupResolution.Onboarding, resolver.resolve())
        assertEquals(1, cleanup.clearAllCalls)
    }

    @Test fun missingCompleteSessionResolvesPairingAndCleansIncompleteState() = runTest {
        val cleanup = StartupFakeSecureSessionRepository()
        val result = resolver(
            metadata = null,
            token = SecureTokenReadResult.Missing,
            cleanup = cleanup,
        ).resolve()
        assertSame(StartupResolution.Pairing, result)
        assertEquals(1, cleanup.clearLocalCalls)
    }

    @Test fun tokenAndMetadataMustBothExist() = runTest {
        val tokenOnly = resolver(metadata = null, token = SecureTokenReadResult.Available("token"))
        assertSame(StartupResolution.Pairing, tokenOnly.resolve())
        val metadataOnly = resolver(metadata = metadata(), token = SecureTokenReadResult.Missing)
        assertSame(StartupResolution.Pairing, metadataOnly.resolve())
    }

    @Test fun corruptKeyFailuresAndUnsupportedMetadataResolvePairing() = runTest {
        for (token in listOf(SecureTokenReadResult.Corrupted, SecureTokenReadResult.CryptoFailure, SecureTokenReadResult.KeyUnavailable, SecureTokenReadResult.KeyInvalidated)) {
            assertSame(StartupResolution.Pairing, resolver(token = token).resolve())
        }
        assertSame(
            StartupResolution.Pairing,
            resolver(metadata = metadata().copy(schemaVersion = 99), token = SecureTokenReadResult.Available("token")).resolve(),
        )
    }

    @Test fun metadataReferenceAndSupportedValuesAreValidated() = runTest {
        assertSame(StartupResolution.Pairing, resolver(metadata = metadata().copy(connectionApiBaseUrl = "https://other.test/wp-json/prices/v1/"), token = available()).resolve())
        assertSame(StartupResolution.Pairing, resolver(metadata = metadata().copy(tokenType = "Basic"), token = available()).resolve())
        assertSame(StartupResolution.Pairing, resolver(metadata = metadata().copy(authenticationMethod = "password"), token = available()).resolve())
        assertSame(StartupResolution.Pairing, resolver(metadata = metadata().copy(deviceId = ""), token = available()).resolve())
    }

    @Test fun successfulVerificationInstallsTokenAndRebuildsSession() = runTest {
        withServer { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(verificationJson()))
            val tokenStore = InMemoryAccessTokenProvider()
            val sessionStore = InMemorySessionStore()
            val result = resolver(server = server, tokenStore = tokenStore, sessionStore = sessionStore).resolve()
            assertTrue(result is StartupResolution.Products)
            assertEquals("synthetic-token", tokenStore.currentToken())
            val session = (sessionStore.state.value as com.mahdiMb55.prices.data.session.SessionState.Authenticated).session
            assertEquals(42L, session.userId)
            assertFalse(session.toString().contains("synthetic-token"))
        }
    }

    @Test fun authenticationFailureCleansSessionAndPreservesConnection() = runTest {
        withServer { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody(errorJson("AUTHENTICATION_REQUIRED")))
            val tokenStore = InMemoryAccessTokenProvider()
            val cleanup = StartupFakeSecureSessionRepository()
            val result = resolver(server = server, cleanup = cleanup, tokenStore = tokenStore).resolve()
            assertSame(StartupResolution.Pairing, result)
            assertNull(tokenStore.currentToken())
            assertEquals(1, cleanup.clearLocalCalls)
        }
    }

    @Test fun revokedCodeCleansButUnrelated403IsRetryableAndPreservesRecords() = runTest {
        withServer { server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody(errorJson("DEVICE_REVOKED")))
            val cleanup = StartupFakeSecureSessionRepository()
            assertSame(StartupResolution.Pairing, resolver(server = server, cleanup = cleanup).resolve())
            assertEquals(1, cleanup.clearLocalCalls)

            server.enqueue(MockResponse().setResponseCode(403).setBody(errorJson("PERMISSION_DENIED")))
            val preserved = StartupFakeSecureSessionRepository()
            val retry = resolver(server, cleanup = preserved).resolve()
            assertTrue(retry is StartupResolution.RetryableVerificationFailure)
            assertEquals(0, preserved.clearLocalCalls)
        }
    }

    @Test fun connectivityFailureClearsMemoryButPreservesPersistentRecords() = runTest {
        val tokenStore = InMemoryAccessTokenProvider()
        val cleanup = StartupFakeSecureSessionRepository()
        val result = resolver(
            connection = connection("http://127.0.0.1:1/wp-json/prices/v1/"),
            tokenStore = tokenStore,
            cleanup = cleanup,
        ).resolve()
        assertTrue(result is StartupResolution.RetryableVerificationFailure)
        assertNull(tokenStore.currentToken())
        assertEquals(0, cleanup.clearLocalCalls)
    }

    @Test fun storageFailureDoesNotOpenProducts() = runTest {
        val result = resolver(token = SecureTokenReadResult.StorageFailure).resolve()
        assertTrue(result is StartupResolution.StorageFailure)
    }

    @Test fun cancellationClearsTemporaryMemoryAndPreservesPersistentRecords() = runTest {
        val tokenStore = InMemoryAccessTokenProvider()
        val secure = StartupFakeSecureTokenStorage(throwOnRead = true)
        try {
            resolver(tokenStore = tokenStore, secure = secure).resolve()
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            assertNull(tokenStore.currentToken())
        }
    }

    private fun resolver(
        server: MockWebServer? = null,
        connection: StoredConnection? = server?.let { connection(it) } ?: connection(),
        metadata: StoredPairedSessionMetadata? = metadataFor(connection),
        token: SecureTokenReadResult = available(),
        tokenStore: InMemoryAccessTokenProvider = InMemoryAccessTokenProvider(),
        sessionStore: SessionStore = InMemorySessionStore(),
        cleanup: StartupFakeSecureSessionRepository = StartupFakeSecureSessionRepository(),
        secure: StartupFakeSecureTokenStorage = StartupFakeSecureTokenStorage(token),
    ): DefaultStartupSessionResolver = DefaultStartupSessionResolver(
        connectionPreferences = StartupFakeConnectionPreferences(connection),
        metadataPreferences = StartupFakeMetadataPreferences(metadata),
        secureTokenStorage = secure,
        tokenStore = tokenStore,
        sessionStore = sessionStore,
        secureSessionRepository = cleanup,
        pricesApiFactory = PricesApiFactory(tokenStore),
        networkRequestExecutor = NetworkRequestExecutor(ApiErrorParser()),
    )

    private suspend fun withServer(block: suspend (MockWebServer) -> Unit) {
        MockWebServer().use { server ->
            server.start()
            block(server)
        }
    }

    private fun available() = SecureTokenReadResult.Available("synthetic-token")

    private fun metadata() = StoredPairedSessionMetadata(
        deviceId = "dev_0123456789abcdef0123456789abcdef",
        deviceName = "synthetic-device",
        userId = 1,
        authenticationMethod = "device_token",
        tokenType = "Bearer",
        capabilities = AuthenticationCapabilitiesSnapshot(true, false, true),
        pairedAtEpochMillis = 1,
        connectionApiBaseUrl = "http://example.test/wp-json/prices/v1/",
    )

    private fun metadataFor(connection: StoredConnection?) = connection?.let {
        metadata().copy(connectionApiBaseUrl = it.apiBaseUrl)
    } ?: metadata()

    private fun connection(baseUrl: String = "http://example.test/wp-json/prices/v1/") = StoredConnection(
        apiBaseUrl = baseUrl,
        siteUrl = "http://example.test",
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
        authenticationCapabilities = AuthenticationCapabilitiesSnapshot(true, false, true),
        discoveredAtEpochMillis = 1,
    )

    private fun connection(server: MockWebServer) = connection(server.url("/wp-json/prices/v1/").toString())

    private fun verificationJson() = "{\"data\":{\"user_id\":42,\"device_id\":\"dev_0123456789abcdef0123456789abcdef\",\"authentication_method\":\"device_token\"}}"

    private fun errorJson(code: String) = "{\"error\":{\"code\":\"$code\",\"message\":\"synthetic\",\"details\":{},\"request_id\":\"test\"}}"
}

private class StartupFakeConnectionPreferences(initial: StoredConnection?) : ConnectionPreferences {
    override val connection: Flow<StoredConnection?> = MutableStateFlow(initial)
    override suspend fun save(connection: StoredConnection) = Unit
    override suspend fun clear() = Unit
}

private class StartupFakeMetadataPreferences(private val value: StoredPairedSessionMetadata?) : PairedSessionMetadataPreferences {
    override val metadata: Flow<StoredPairedSessionMetadata?> = MutableStateFlow(value).asStateFlow()
    override suspend fun readOnce() = value
    override suspend fun save(metadata: StoredPairedSessionMetadata) = MetadataPersistenceResult.Success
    override suspend fun clear() = MetadataPersistenceResult.Success
}

private class StartupFakeSecureTokenStorage(
    private val result: SecureTokenReadResult = SecureTokenReadResult.Available("synthetic-token"),
    private val throwOnRead: Boolean = false,
) : SecureTokenStorage {
    override suspend fun read(): SecureTokenReadResult {
        if (throwOnRead) throw CancellationException("synthetic cancellation")
        return result
    }
    override suspend fun write(token: String) = SecureTokenWriteResult.Success
    override suspend fun clear() = SecureTokenWriteResult.Success
}

private class StartupFakeSecureSessionRepository : SecureSessionRepository {
    var clearLocalCalls = 0
    var clearAllCalls = 0
    override fun clearInMemorySession() = Unit
    override suspend fun persistVerifiedSession(token: String, metadata: StoredPairedSessionMetadata, session: PairedSession) = PersistSessionResult.Success
    override suspend fun clearLocalSession(): SessionCleanupResult {
        clearLocalCalls++
        return SessionCleanupResult.Success
    }
    override suspend fun clearAllForStoreChange(): SessionCleanupResult {
        clearAllCalls++
        return SessionCleanupResult.Success
    }
}
