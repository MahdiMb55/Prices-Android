package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import com.mahdiMb55.prices.data.local.session.PairedSessionMetadataPreferences
import com.mahdiMb55.prices.data.local.session.StoredPairedSessionMetadata
import com.mahdiMb55.prices.data.remote.MutableAccessTokenStore
import com.mahdiMb55.prices.data.remote.NetworkError
import com.mahdiMb55.prices.data.remote.NetworkErrorCategory
import com.mahdiMb55.prices.data.remote.NetworkRequestExecutor
import com.mahdiMb55.prices.data.remote.NetworkResult
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import com.mahdiMb55.prices.data.remote.StoreUrlNormalizationResult
import com.mahdiMb55.prices.data.remote.StoreUrlNormalizer
import com.mahdiMb55.prices.data.security.SecureTokenReadResult
import com.mahdiMb55.prices.data.security.SecureTokenStorage
import com.mahdiMb55.prices.data.session.PairedSession
import com.mahdiMb55.prices.data.session.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

interface StartupSessionResolver {
    suspend fun resolve(): StartupResolution
}

sealed interface StartupResolution {
    data object Onboarding : StartupResolution
    data object Pairing : StartupResolution
    data class Products(val storeName: String) : StartupResolution
    data class RetryableVerificationFailure(
        val reason: StartupVerificationFailure,
        val storeName: String,
    ) : StartupResolution
    data class StorageFailure(val storeName: String?) : StartupResolution
}

enum class StartupVerificationFailure {
    SessionExpiredOrRevoked,
    AuthenticationRejected,
    InvalidStoredSession,
    RetryableNetworkFailure,
    RetryableTimeout,
    RetryableTlsFailure,
    StorageFailure,
    Unknown,
}

class DefaultStartupSessionResolver(
    private val connectionPreferences: ConnectionPreferences,
    private val metadataPreferences: PairedSessionMetadataPreferences,
    private val secureTokenStorage: SecureTokenStorage,
    private val tokenStore: MutableAccessTokenStore,
    private val sessionStore: SessionStore,
    private val secureSessionRepository: SecureSessionRepository,
    private val pricesApiFactory: PricesApiFactory,
    private val networkRequestExecutor: NetworkRequestExecutor,
) : StartupSessionResolver {
    override suspend fun resolve(): StartupResolution {
        val connection = try {
            connectionPreferences.connection.first()
        } catch (cancellation: CancellationException) {
            clearTemporaryMemory()
            throw cancellation
        } catch (_: Exception) {
            clearTemporaryMemory()
            return StartupResolution.StorageFailure(null)
        }

        if (connection == null) {
            secureSessionRepository.clearAllForStoreChange()
            return StartupResolution.Onboarding
        }

        val metadata = try {
            metadataPreferences.readOnce()
        } catch (cancellation: CancellationException) {
            clearTemporaryMemory()
            throw cancellation
        } catch (_: Exception) {
            clearTemporaryMemory()
            return StartupResolution.StorageFailure(connection.siteName)
        }

        val token = try {
            secureTokenStorage.read()
        } catch (cancellation: CancellationException) {
            clearTemporaryMemory()
            throw cancellation
        }
        return when (token) {
            SecureTokenReadResult.Missing -> clearAndPair(connection)
            SecureTokenReadResult.Corrupted,
            SecureTokenReadResult.CryptoFailure,
            SecureTokenReadResult.KeyUnavailable,
            SecureTokenReadResult.KeyInvalidated -> clearAndPair(connection)
            SecureTokenReadResult.StorageFailure -> {
                clearTemporaryMemory()
                StartupResolution.StorageFailure(connection.siteName)
            }
            is SecureTokenReadResult.Available -> {
                if (metadata == null || !isCompatible(metadata, connection)) {
                    clearAndPair(connection)
                } else {
                    verify(connection, metadata, token.token)
                }
            }
        }
    }

    private suspend fun verify(
        connection: StoredConnection,
        metadata: StoredPairedSessionMetadata,
        token: String,
    ): StartupResolution {
        tokenStore.updateToken(token)
        val baseUrl = StoreUrlNormalizer.normalize(connection.apiBaseUrl)
        if (baseUrl !is StoreUrlNormalizationResult.Valid) {
            return clearAndPair(connection)
        }

        val verification = try {
            networkRequestExecutor.execute { pricesApiFactory.create(baseUrl.baseUrl).currentSession() }
        } catch (cancellation: CancellationException) {
            clearTemporaryMemory()
            throw cancellation
        }

        return when (verification) {
            is NetworkResult.Success -> {
                val current = verification.value.data
                if (current.deviceId != metadata.deviceId || current.authenticationMethod != SUPPORTED_AUTHENTICATION_METHOD) {
                    clearAndPair(connection)
                } else {
                    try {
                        sessionStore.authenticate(
                            PairedSession(
                                deviceId = metadata.deviceId,
                                deviceName = metadata.deviceName,
                                userId = current.userId,
                                store = connection,
                            ),
                        )
                        StartupResolution.Products(connection.siteName)
                    } catch (cancellation: CancellationException) {
                        clearTemporaryMemory()
                        throw cancellation
                    } catch (_: Exception) {
                        secureSessionRepository.clearLocalSession()
                        StartupResolution.StorageFailure(connection.siteName)
                    }
                }
            }
            is NetworkResult.Failure -> mapFailure(connection, verification.error)
        }
    }

    private suspend fun mapFailure(connection: StoredConnection, error: NetworkError): StartupResolution {
        return when {
            isConfirmedAuthenticationFailure(error) -> {
                clearAndPair(connection)
            }
            error.category == NetworkErrorCategory.Serialization -> {
                clearAndPair(connection)
            }
            error.category == NetworkErrorCategory.Connectivity -> {
                clearTemporaryMemory()
                StartupResolution.RetryableVerificationFailure(
                    StartupVerificationFailure.RetryableNetworkFailure,
                    connection.siteName,
                )
            }
            error.category == NetworkErrorCategory.Timeout -> {
                clearTemporaryMemory()
                StartupResolution.RetryableVerificationFailure(
                    StartupVerificationFailure.RetryableTimeout,
                    connection.siteName,
                )
            }
            error.category == NetworkErrorCategory.Tls -> {
                clearTemporaryMemory()
                StartupResolution.RetryableVerificationFailure(
                    StartupVerificationFailure.RetryableTlsFailure,
                    connection.siteName,
                )
            }
            else -> {
                clearTemporaryMemory()
                StartupResolution.RetryableVerificationFailure(
                    StartupVerificationFailure.Unknown,
                    connection.siteName,
                )
            }
        }
    }

    private fun isConfirmedAuthenticationFailure(error: NetworkError): Boolean {
        val code = error.apiCode
        if (error.httpStatus == 401 || error.category == NetworkErrorCategory.Authentication) return true
        if (error.httpStatus == 403) {
            return code in REVOKED_AUTHENTICATION_CODES
        }
        return code in REVOKED_AUTHENTICATION_CODES
    }

    private suspend fun clearAndPair(connection: StoredConnection): StartupResolution {
        clearTemporaryMemory()
        secureSessionRepository.clearLocalSession()
        return StartupResolution.Pairing
    }

    private fun clearTemporaryMemory() {
        tokenStore.clear()
        sessionStore.clear()
    }

    private fun isCompatible(metadata: StoredPairedSessionMetadata, connection: StoredConnection): Boolean {
        val normalizedConnection = StoreUrlNormalizer.normalize(connection.apiBaseUrl)
        return metadata.schemaVersion == StoredPairedSessionMetadata.CurrentSchemaVersion &&
            metadata.deviceId.isNotBlank() &&
            metadata.deviceName.isNotBlank() &&
            metadata.authenticationMethod == SUPPORTED_AUTHENTICATION_METHOD &&
            metadata.tokenType == SUPPORTED_TOKEN_TYPE &&
            normalizedConnection is StoreUrlNormalizationResult.Valid &&
            metadata.connectionApiBaseUrl == normalizedConnection.baseUrl.value
    }

    private companion object {
        const val SUPPORTED_AUTHENTICATION_METHOD = "device_token"
        const val SUPPORTED_TOKEN_TYPE = "Bearer"
        val REVOKED_AUTHENTICATION_CODES = setOf(
            "AUTHENTICATION_REQUIRED",
            "INVALID_DEVICE_TOKEN",
            "DEVICE_REVOKED",
            "DEVICE_NOT_FOUND",
            "DEVICE_INVALID",
            "TOKEN_REVOKED",
        )
    }
}
