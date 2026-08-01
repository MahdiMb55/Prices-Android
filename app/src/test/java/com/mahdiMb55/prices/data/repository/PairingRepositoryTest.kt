package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.core.appinfo.AppInfo
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingRepositoryTest {
    @Test fun verifiedPairingPersistsSecureSessionAndEstablishesSuccess() = runTest {
        withServer { server ->
            enqueueSuccessfulPairing(server, unknownField = true)
            val tokenStore = InMemoryAccessTokenProvider()
            val metadata = PairingFakeMetadataPreferences()
            val secure = PairingFakeSecureTokenStorage()
            val connectionPreferences = PairingFakeConnectionPreferences(connection(server))
            val sessionStore = InMemorySessionStore()
            val repository = repository(server, tokenStore, secure, metadata, connectionPreferences, sessionStore)

            val result = repository.exchange("12345678", "test-device")

            assertTrue(result is PairingResult.Success)
            assertEquals("synthetic-device-token", tokenStore.currentToken())
            assertEquals("test-device", metadata.saved?.deviceName)
            assertTrue(sessionStore.state.value is com.mahdiMb55.prices.data.session.SessionState.Authenticated)
            assertEquals(connection(server).apiBaseUrl, connectionPreferences.value.value?.apiBaseUrl)
        }
    }

    @Test fun secureTokenPersistenceFailurePreventsSuccessAndLeavesNoMemoryToken() = runTest {
        withServer { server ->
            enqueueSuccessfulPairing(server)
            val tokenStore = InMemoryAccessTokenProvider()
            val secure = PairingFakeSecureTokenStorage(writeResult = SecureTokenWriteResult.StorageFailure)
            val metadata = PairingFakeMetadataPreferences()
            val repository = repository(server, tokenStore, secure, metadata)

            val result = repository.exchange("12345678", "test-device")

            assertEquals(PairingFailure.SecureSessionSaveFailed, (result as PairingResult.Failure).reason)
            assertNull(tokenStore.currentToken())
            assertNull(metadata.saved)
        }
    }

    @Test fun metadataPersistenceFailureRollsBackSecureTokenAndMemory() = runTest {
        withServer { server ->
            enqueueSuccessfulPairing(server)
            val tokenStore = InMemoryAccessTokenProvider()
            val secure = PairingFakeSecureTokenStorage()
            val metadata = PairingFakeMetadataPreferences(saveResult = MetadataPersistenceResult.Failure)
            val repository = repository(server, tokenStore, secure, metadata)

            assertEquals(PairingFailure.SecureSessionSaveFailed, (repository.exchange("12345678", "test-device") as PairingResult.Failure).reason)
            assertEquals(1, secure.clearCalls)
            assertNull(tokenStore.currentToken())
            assertNull(metadata.saved)
        }
    }

    @Test fun unverifiedTokenIsClearedAndNeverPersisted() = runTest {
        withServer { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(exchangeJson()))
            server.enqueue(MockResponse().setResponseCode(401).setBody("{\"code\":\"AUTHENTICATION_REQUIRED\"}"))
            val tokenStore = InMemoryAccessTokenProvider()
            val secure = PairingFakeSecureTokenStorage()
            val metadata = PairingFakeMetadataPreferences()
            val repository = repository(server, tokenStore, secure, metadata)

            assertEquals(PairingFailure.VerificationFailed, (repository.exchange("12345678", "test-device") as PairingResult.Failure).reason)
            assertNull(tokenStore.currentToken())
            assertEquals(0, secure.writeCalls)
            assertNull(metadata.saved)
        }
    }

    @Test fun retryAfterPersistenceFailureCanSucceed() = runTest {
        withServer { server ->
            enqueueSuccessfulPairing(server)
            enqueueSuccessfulPairing(server)
            val tokenStore = InMemoryAccessTokenProvider()
            val secure = PairingFakeSecureTokenStorage(writeResult = SecureTokenWriteResult.StorageFailure)
            val metadata = PairingFakeMetadataPreferences()
            val repository = repository(server, tokenStore, secure, metadata)

            assertTrue(repository.exchange("12345678", "test-device") is PairingResult.Failure)
            secure.writeResult = SecureTokenWriteResult.Success
            assertTrue(repository.exchange("12345678", "test-device") is PairingResult.Success)
        }
    }

    private fun repository(
        server: MockWebServer,
        tokenStore: InMemoryAccessTokenProvider,
        secure: PairingFakeSecureTokenStorage,
        metadata: PairingFakeMetadataPreferences,
        connectionPreferences: PairingFakeConnectionPreferences = PairingFakeConnectionPreferences(connection(server)),
        sessionStore: InMemorySessionStore = InMemorySessionStore(),
    ): DefaultPairingRepository {
        val secureSession = DefaultSecureSessionRepository(
            secure,
            metadata,
            tokenStore,
            sessionStore,
            connectionPreferences,
        )
        return DefaultPairingRepository(
            connectionPreferences = connectionPreferences,
            pricesApiFactory = PricesApiFactory(tokenStore),
            executor = NetworkRequestExecutor(ApiErrorParser()),
            tokenStore = tokenStore,
            secureSessionRepository = secureSession,
            appInfoProvider = FakeAppInfoProvider(),
            androidVersion = "test",
            now = { 1L },
        )
    }

    private fun enqueueSuccessfulPairing(server: MockWebServer, unknownField: Boolean = false) {
        server.enqueue(MockResponse().setResponseCode(200).setBody(exchangeJson(unknownField)))
        server.enqueue(MockResponse().setResponseCode(200).setBody(verificationJson()))
    }

    private suspend fun withServer(block: suspend (MockWebServer) -> Unit) {
        MockWebServer().use { server ->
            server.start()
            block(server)
        }
    }

    private fun connection(server: MockWebServer) = StoredConnection(
        apiBaseUrl = server.url("/wp-json/prices/v1/").toString(),
        siteUrl = server.url("/").toString(),
        siteName = "Example",
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

    private fun exchangeJson(unknownField: Boolean = false): String {
        val extra = if (unknownField) ",\"future_field\":\"ignored\"" else ""
        return "{\"data\":{\"device_id\":\"dev_0123456789abcdef0123456789abcdef\",\"token_type\":\"Bearer\",\"device_token\":\"synthetic-device-token\",\"authentication_method\":\"device_token\"$extra}}"
    }

    private fun verificationJson() =
        """{"data":{"user_id":42,"device_id":"dev_0123456789abcdef0123456789abcdef","authentication_method":"device_token"}}"""
}

private class FakeAppInfoProvider : AppInfoProvider {
    override val appInfo = AppInfo("1.0", 1, "com.mahdiMb55.prices", true)
}

private class PairingFakeConnectionPreferences(initial: StoredConnection?) : ConnectionPreferences {
    val value = MutableStateFlow(initial)
    override val connection: Flow<StoredConnection?> = value.asStateFlow()
    override suspend fun save(connection: StoredConnection) { value.value = connection }
    override suspend fun clear() { value.value = null }
}

private class PairingFakeSecureTokenStorage(
    var writeResult: SecureTokenWriteResult = SecureTokenWriteResult.Success,
) : SecureTokenStorage {
    var writeCalls = 0
    var clearCalls = 0
    override suspend fun read(): SecureTokenReadResult = SecureTokenReadResult.Missing
    override suspend fun write(token: String): SecureTokenWriteResult {
        writeCalls++
        return writeResult
    }
    override suspend fun clear(): SecureTokenWriteResult {
        clearCalls++
        return SecureTokenWriteResult.Success
    }
}

private class PairingFakeMetadataPreferences(
    private val saveResult: MetadataPersistenceResult = MetadataPersistenceResult.Success,
) : PairedSessionMetadataPreferences {
    var saved: StoredPairedSessionMetadata? = null
    override val metadata: Flow<StoredPairedSessionMetadata?> = MutableStateFlow(null)
    override suspend fun readOnce(): StoredPairedSessionMetadata? = saved
    override suspend fun save(metadata: StoredPairedSessionMetadata): MetadataPersistenceResult {
        if (saveResult is MetadataPersistenceResult.Success) saved = metadata
        return saveResult
    }
    override suspend fun clear(): MetadataPersistenceResult {
        saved = null
        return MetadataPersistenceResult.Success
    }
}
