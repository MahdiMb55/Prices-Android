package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.session.MetadataPersistenceResult
import com.mahdiMb55.prices.data.local.session.PairedSessionMetadataPreferences
import com.mahdiMb55.prices.data.local.session.StoredPairedSessionMetadata
import com.mahdiMb55.prices.data.remote.MutableAccessTokenStore
import com.mahdiMb55.prices.data.security.SecureTokenStorage
import com.mahdiMb55.prices.data.security.SecureTokenWriteResult
import com.mahdiMb55.prices.data.session.PairedSession
import com.mahdiMb55.prices.data.session.SessionStore
import kotlinx.coroutines.CancellationException

interface SecureSessionRepository {
    fun clearInMemorySession()
    suspend fun persistVerifiedSession(
        token: String,
        metadata: StoredPairedSessionMetadata,
        session: PairedSession,
    ): PersistSessionResult

    suspend fun clearLocalSession(): SessionCleanupResult
    suspend fun clearAllForStoreChange(): SessionCleanupResult
}

sealed interface PersistSessionResult {
    data object Success : PersistSessionResult
    data object SecureTokenWriteFailed : PersistSessionResult
    data object MetadataWriteFailed : PersistSessionResult
    data object RollbackFailed : PersistSessionResult
    data object Unknown : PersistSessionResult
}

sealed interface SessionCleanupResult {
    data object Success : SessionCleanupResult
    data object Failed : SessionCleanupResult
}

class DefaultSecureSessionRepository(
    private val secureTokenStorage: SecureTokenStorage,
    private val metadataPreferences: PairedSessionMetadataPreferences,
    private val tokenStore: MutableAccessTokenStore,
    private val sessionStore: SessionStore,
    private val connectionPreferences: ConnectionPreferences,
) : SecureSessionRepository {
    override fun clearInMemorySession() {
        tokenStore.clear()
        sessionStore.clear()
    }

    override suspend fun persistVerifiedSession(
        token: String,
        metadata: StoredPairedSessionMetadata,
        session: PairedSession,
    ): PersistSessionResult {
        val tokenWrite = try {
            secureTokenStorage.write(token)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
        if (tokenWrite !is SecureTokenWriteResult.Success) {
            return if (rollback(clearMetadata = false)) {
                PersistSessionResult.SecureTokenWriteFailed
            } else {
                PersistSessionResult.RollbackFailed
            }
        }

        val metadataWrite = try {
            metadataPreferences.save(metadata)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            MetadataPersistenceResult.Failure
        }
        if (metadataWrite !is MetadataPersistenceResult.Success) {
            return if (rollback(clearMetadata = true)) {
                PersistSessionResult.MetadataWriteFailed
            } else {
                PersistSessionResult.RollbackFailed
            }
        }

        try {
            sessionStore.authenticate(session)
            return PersistSessionResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return if (rollback(clearMetadata = true)) {
                PersistSessionResult.Unknown
            } else {
                PersistSessionResult.RollbackFailed
            }
        }
    }

    override suspend fun clearLocalSession(): SessionCleanupResult {
        clearInMemorySession()
        return if (clearPersistentState(clearConnection = false)) {
            SessionCleanupResult.Success
        } else {
            SessionCleanupResult.Failed
        }
    }

    override suspend fun clearAllForStoreChange(): SessionCleanupResult {
        clearInMemorySession()
        return if (clearPersistentState(clearConnection = true)) {
            SessionCleanupResult.Success
        } else {
            SessionCleanupResult.Failed
        }
    }

    private suspend fun rollback(clearMetadata: Boolean): Boolean {
        var successful = true
        if (clearMetadata) successful = clearMetadata() && successful
        successful = clearSecureToken() && successful
        return try {
            clearInMemorySession()
            successful
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun clearPersistentState(clearConnection: Boolean): Boolean {
        var successful = true
        successful = clearSecureToken() && successful
        successful = clearMetadata() && successful
        if (clearConnection) {
            try {
                connectionPreferences.clear()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                successful = false
            }
        }
        return successful
    }

    private suspend fun clearSecureToken(): Boolean = try {
        secureTokenStorage.clear() is SecureTokenWriteResult.Success
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false
    }

    private suspend fun clearMetadata(): Boolean = try {
        metadataPreferences.clear() is MetadataPersistenceResult.Success
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false
    }
}
