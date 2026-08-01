package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.core.appinfo.AppInfo
import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import com.mahdiMb55.prices.data.local.session.MetadataPersistenceResult
import com.mahdiMb55.prices.data.local.session.PairedSessionMetadataPreferences
import com.mahdiMb55.prices.data.local.session.StoredPairedSessionMetadata
import com.mahdiMb55.prices.data.remote.MutableAccessTokenStore
import com.mahdiMb55.prices.data.security.SecureTokenReadResult
import com.mahdiMb55.prices.data.security.SecureTokenStorage
import com.mahdiMb55.prices.data.security.SecureTokenWriteResult
import com.mahdiMb55.prices.data.session.InMemorySessionStore
import com.mahdiMb55.prices.data.session.PairedSession
import com.mahdiMb55.prices.data.session.SessionState
import com.mahdiMb55.prices.data.session.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureSessionRepositoryTest {
    @Test fun successfulPersistenceWritesTokenMetadataThenSession() = runTest {
        val events = mutableListOf<String>()
        val secure = FakeSecureTokenStorage(events)
        val metadataPreferences = FakeMetadataPreferences(events)
        val sessionStore = RecordingSessionStore(events)
        val repository = repository(secure, metadataPreferences, sessionStore)

        val result = repository.persistVerifiedSession("synthetic-token", metadata(), session())

        assertSame(PersistSessionResult.Success, result)
        assertEquals(listOf("token-write", "metadata-write", "session-authenticate"), events)
        assertEquals("synthetic-device", metadataPreferences.saved?.deviceName)
        assertFalse(metadataPreferences.saved.toString().contains("synthetic-token"))
        assertTrue(sessionStore.state.value is SessionState.Authenticated)
    }

    @Test fun secureTokenFailurePreventsMetadataAndClearsMemory() = runTest {
        val events = mutableListOf<String>()
        val secure = FakeSecureTokenStorage(events, writeResult = SecureTokenWriteResult.StorageFailure)
        val metadataPreferences = FakeMetadataPreferences(events)
        val tokenStore = RecordingTokenStore(events, "temporary")
        val repository = repository(secure, metadataPreferences, tokenStore = tokenStore)

        assertSame(PersistSessionResult.SecureTokenWriteFailed, repository.persistVerifiedSession("token", metadata(), session()))
        assertEquals(null, metadataPreferences.saved)
        assertEquals(null, tokenStore.currentToken())
    }

    @Test fun metadataFailureRollsBackTokenMetadataAndSession() = runTest {
        val events = mutableListOf<String>()
        val secure = FakeSecureTokenStorage(events)
        val metadataPreferences = FakeMetadataPreferences(events, saveResult = MetadataPersistenceResult.Failure)
        val sessionStore = RecordingSessionStore(events)
        val repository = repository(secure, metadataPreferences, sessionStore)

        assertSame(PersistSessionResult.MetadataWriteFailed, repository.persistVerifiedSession("token", metadata(), session()))
        assertEquals(1, secure.clearCalls)
        assertEquals(0, sessionStore.authenticateCalls)
        assertTrue(events.indexOf("token-clear") < events.indexOf("memory-clear"))
    }

    @Test fun rollbackFailureIsDistinguishable() = runTest {
        val secure = FakeSecureTokenStorage(
            mutableListOf(),
            clearResult = SecureTokenWriteResult.StorageFailure,
        )
        val metadataPreferences = FakeMetadataPreferences(saveResult = MetadataPersistenceResult.Failure)
        val result = repository(secure, metadataPreferences).persistVerifiedSession("token", metadata(), session())
        assertSame(PersistSessionResult.RollbackFailed, result)
    }

    @Test fun sessionEstablishmentFailureRollsBackPersistentState() = runTest {
        val secure = FakeSecureTokenStorage(mutableListOf())
        val metadataPreferences = FakeMetadataPreferences()
        val sessionStore = RecordingSessionStore(throwOnAuthenticate = true)
        val result = repository(secure, metadataPreferences, sessionStore)
            .persistVerifiedSession("token", metadata(), session())
        assertSame(PersistSessionResult.Unknown, result)
        assertEquals(1, secure.clearCalls)
        assertEquals(1, metadataPreferences.clearCalls)
    }

    @Test fun localDisconnectClearsMemoryFirstAndPreservesConnection() = runTest {
        val events = mutableListOf<String>()
        val connection = FakeConnectionPreferences(events)
        val result = repository(
            secure = FakeSecureTokenStorage(events),
            metadataPreferences = FakeMetadataPreferences(events),
            tokenStore = RecordingTokenStore(events),
            sessionStore = RecordingSessionStore(events),
            connection = connection,
        ).clearLocalSession()
        assertSame(SessionCleanupResult.Success, result)
        assertEquals(listOf("memory-token-clear", "memory-clear", "token-clear", "metadata-clear"), events)
        assertFalse(connection.cleared)
    }

    @Test fun changeStoreClearsEverythingIncludingConnection() = runTest {
        val events = mutableListOf<String>()
        val connection = FakeConnectionPreferences(events)
        val result = repository(
            secure = FakeSecureTokenStorage(events),
            metadataPreferences = FakeMetadataPreferences(events),
            tokenStore = RecordingTokenStore(events),
            sessionStore = RecordingSessionStore(events),
            connection = connection,
        ).clearAllForStoreChange()
        assertSame(SessionCleanupResult.Success, result)
        assertTrue(connection.cleared)
        assertEquals("memory-token-clear", events.first())
    }

    @Test fun cleanupFailureIsSurfacedAndCancellationPropagates() = runTest {
        val failed = repository(
            secure = FakeSecureTokenStorage(mutableListOf(), clearResult = SecureTokenWriteResult.StorageFailure),
        ).clearLocalSession()
        assertSame(SessionCleanupResult.Failed, failed)

        val cancellation = repository(secure = FakeSecureTokenStorage(mutableListOf(), throwOnClear = true))
        try {
            cancellation.clearLocalSession()
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) {
            // Expected contract.
        }
    }

    private fun repository(
        secure: FakeSecureTokenStorage = FakeSecureTokenStorage(mutableListOf()),
        metadataPreferences: FakeMetadataPreferences = FakeMetadataPreferences(),
        sessionStore: RecordingSessionStore = RecordingSessionStore(mutableListOf()),
        tokenStore: RecordingTokenStore = RecordingTokenStore(mutableListOf()),
        connection: FakeConnectionPreferences = FakeConnectionPreferences(mutableListOf()),
    ) = DefaultSecureSessionRepository(secure, metadataPreferences, tokenStore, sessionStore, connection)

    private fun metadata() = StoredPairedSessionMetadata(
        deviceId = "dev_0123456789abcdef0123456789abcdef",
        deviceName = "synthetic-device",
        userId = 42,
        authenticationMethod = "device_token",
        tokenType = "Bearer",
        capabilities = AuthenticationCapabilitiesSnapshot(true, false, true),
        pairedAtEpochMillis = 1,
        connectionApiBaseUrl = "https://example.test/wp-json/prices/v1/",
    )

    private fun session() = PairedSession("dev_0123456789abcdef0123456789abcdef", "synthetic-device", 42, connection())

    private fun connection() = StoredConnection(
        apiBaseUrl = "https://example.test/wp-json/prices/v1/",
        siteUrl = "https://example.test",
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
}

private class RecordingTokenStore(
    private val events: MutableList<String>,
    initialToken: String? = null,
) : MutableAccessTokenStore {
    private var token: String? = initialToken
    override fun currentToken(): String? = token
    override fun updateToken(token: String?) {
        events.add("token-update")
        this.token = token
    }
    override fun clear() {
        events.add("memory-token-clear")
        token = null
    }
}

private class FakeSecureTokenStorage(
    private val events: MutableList<String>,
    private val writeResult: SecureTokenWriteResult = SecureTokenWriteResult.Success,
    private val clearResult: SecureTokenWriteResult = SecureTokenWriteResult.Success,
    private val throwOnClear: Boolean = false,
) : SecureTokenStorage {
    var clearCalls = 0
    override suspend fun read(): SecureTokenReadResult = SecureTokenReadResult.Missing
    override suspend fun write(token: String): SecureTokenWriteResult {
        events.add("token-write")
        return writeResult
    }
    override suspend fun clear(): SecureTokenWriteResult {
        events.add("token-clear")
        clearCalls++
        if (throwOnClear) throw CancellationException("synthetic cancellation")
        return clearResult
    }
}

private class FakeMetadataPreferences(
    private val events: MutableList<String> = mutableListOf(),
    private val saveResult: MetadataPersistenceResult = MetadataPersistenceResult.Success,
) : PairedSessionMetadataPreferences {
    private val flow = MutableStateFlow<StoredPairedSessionMetadata?>(null)
    var saved: StoredPairedSessionMetadata? = null
    var clearCalls = 0
    override val metadata: Flow<StoredPairedSessionMetadata?> = flow.asStateFlow()
    override suspend fun readOnce(): StoredPairedSessionMetadata? = flow.value
    override suspend fun save(metadata: StoredPairedSessionMetadata): MetadataPersistenceResult {
        events.add("metadata-write")
        if (saveResult is MetadataPersistenceResult.Success) {
            saved = metadata
            flow.value = metadata
        }
        return saveResult
    }
    override suspend fun clear(): MetadataPersistenceResult {
        events.add("metadata-clear")
        clearCalls++
        saved = null
        flow.value = null
        return MetadataPersistenceResult.Success
    }
}

private class RecordingSessionStore(
    private val events: MutableList<String> = mutableListOf(),
    private val throwOnAuthenticate: Boolean = false,
) : SessionStore {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    override val state = mutableState.asStateFlow()
    var authenticateCalls = 0
    override fun authenticate(session: PairedSession) {
        events.add("session-authenticate")
        authenticateCalls++
        if (throwOnAuthenticate) throw IllegalStateException("synthetic failure")
        mutableState.value = SessionState.Authenticated(session)
    }
    override fun clear() {
        events.add("memory-clear")
        mutableState.value = SessionState.Unauthenticated
    }
}

private class FakeConnectionPreferences(private val events: MutableList<String>) : ConnectionPreferences {
    var cleared = false
    override val connection: Flow<StoredConnection?> = MutableStateFlow(null)
    override suspend fun save(connection: StoredConnection) = Unit
    override suspend fun clear() {
        events.add("connection-clear")
        cleared = true
    }
}
