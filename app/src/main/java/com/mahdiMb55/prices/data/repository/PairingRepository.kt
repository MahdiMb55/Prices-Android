package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.session.StoredPairedSessionMetadata
import com.mahdiMb55.prices.data.remote.MutableAccessTokenStore
import com.mahdiMb55.prices.data.remote.NetworkError
import com.mahdiMb55.prices.data.remote.NetworkErrorCategory
import com.mahdiMb55.prices.data.remote.NetworkRequestExecutor
import com.mahdiMb55.prices.data.remote.NetworkResult
import com.mahdiMb55.prices.data.remote.PairingExchangeRequestDto
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import com.mahdiMb55.prices.data.remote.StoreUrlNormalizationResult
import com.mahdiMb55.prices.data.remote.StoreUrlNormalizer
import com.mahdiMb55.prices.data.session.PairedSession
import kotlinx.coroutines.flow.first

interface PairingRepository {
    suspend fun exchange(pairingCode: String, deviceName: String): PairingResult
    suspend fun clearSession(): SessionCleanupResult
    suspend fun clearForStoreChange(): SessionCleanupResult
}

sealed interface PairingResult {
    data class Success(val session: PairedSession) : PairingResult
    data class Failure(val reason: PairingFailure, val backendCode: String? = null) : PairingResult
}

enum class PairingFailure {
    NoDiscoveredStore, InvalidCodeFormat, InvalidOrExpiredCode, TooManyAttempts,
    PairingDisabled, DeviceLimitReached, PermissionDenied, NetworkUnavailable, Timeout,
    TlsFailure, ServerError, InvalidResponse, VerificationFailed, SecureSessionSaveFailed, Unknown
}

class DefaultPairingRepository(
    private val connectionPreferences: ConnectionPreferences,
    private val pricesApiFactory: PricesApiFactory,
    private val executor: NetworkRequestExecutor,
    private val tokenStore: MutableAccessTokenStore,
    private val secureSessionRepository: SecureSessionRepository,
    private val appInfoProvider: AppInfoProvider,
    private val androidVersion: String,
    private val now: () -> Long = { System.currentTimeMillis() },
) : PairingRepository {
    override suspend fun exchange(pairingCode: String, deviceName: String): PairingResult {
        secureSessionRepository.clearInMemorySession()
        if (!PairingCodeFormat.isValid(pairingCode)) return PairingResult.Failure(PairingFailure.InvalidCodeFormat)
        val connection = connectionPreferences.connection.first() ?: return PairingResult.Failure(PairingFailure.NoDiscoveredStore)
        val baseUrl = StoreUrlNormalizer.normalize(connection.apiBaseUrl)
        if (baseUrl !is StoreUrlNormalizationResult.Valid) return PairingResult.Failure(PairingFailure.NoDiscoveredStore)
        val response = executor.execute {
            pricesApiFactory.create(baseUrl.baseUrl).exchangePairingCode(
                PairingExchangeRequestDto(pairingCode, deviceName.trim(), "prices-android", appInfoProvider.appInfo.versionName, androidVersion)
            )
        }
        if (response is NetworkResult.Failure) return response.asFailure()
        val exchange = (response as NetworkResult.Success).value.data
        if (exchange.deviceToken.isBlank() || exchange.tokenType != "Bearer" || !exchange.deviceId.matches(Regex("dev_[A-Fa-f0-9]{32}")) || exchange.authenticationMethod != "device_token") {
            return PairingResult.Failure(PairingFailure.InvalidResponse)
        }
        tokenStore.updateToken(exchange.deviceToken)
        val verification = executor.execute { pricesApiFactory.create(baseUrl.baseUrl).currentSession() }
        if (verification !is NetworkResult.Success || verification.value.data.deviceId != exchange.deviceId || verification.value.data.authenticationMethod != "device_token") {
            secureSessionRepository.clearInMemorySession()
            return PairingResult.Failure(PairingFailure.VerificationFailed)
        }
        val session = PairedSession(exchange.deviceId, deviceName.trim(), verification.value.data.userId, connection)
        val metadata = StoredPairedSessionMetadata(
            deviceId = exchange.deviceId,
            deviceName = deviceName.trim(),
            userId = verification.value.data.userId,
            authenticationMethod = exchange.authenticationMethod,
            tokenType = exchange.tokenType,
            capabilities = connection.authenticationCapabilities,
            pairedAtEpochMillis = now(),
            connectionApiBaseUrl = connection.apiBaseUrl,
        )
        return when (secureSessionRepository.persistVerifiedSession(exchange.deviceToken, metadata, session)) {
            PersistSessionResult.Success -> PairingResult.Success(session)
            PersistSessionResult.SecureTokenWriteFailed,
            PersistSessionResult.MetadataWriteFailed,
            PersistSessionResult.RollbackFailed,
            PersistSessionResult.Unknown -> PairingResult.Failure(PairingFailure.SecureSessionSaveFailed)
        }
    }

    override suspend fun clearSession(): SessionCleanupResult = secureSessionRepository.clearLocalSession()

    override suspend fun clearForStoreChange(): SessionCleanupResult = secureSessionRepository.clearAllForStoreChange()

    private fun NetworkResult.Failure.asFailure(): PairingResult.Failure = PairingResult.Failure(
        when (error.apiCode) {
            "PAIRING_FAILED" -> PairingFailure.InvalidOrExpiredCode
            "RATE_LIMITED" -> PairingFailure.TooManyAttempts
            "PAIRING_DISABLED" -> PairingFailure.PairingDisabled
            "DEVICE_LIMIT_REACHED" -> PairingFailure.DeviceLimitReached
            "PERMISSION_DENIED", "AUTHENTICATION_REQUIRED" -> PairingFailure.PermissionDenied
            else -> when (error.category) {
                NetworkErrorCategory.Connectivity -> PairingFailure.NetworkUnavailable
                NetworkErrorCategory.Timeout -> PairingFailure.Timeout
                NetworkErrorCategory.Tls -> PairingFailure.TlsFailure
                NetworkErrorCategory.Server -> PairingFailure.ServerError
                NetworkErrorCategory.Serialization -> PairingFailure.InvalidResponse
                else -> PairingFailure.Unknown
            }
        }, error.apiCode
    )
}

object PairingCodeFormat {
    private val pattern = Regex("[0-9]{8}")
    fun normalize(value: String): String = value.filter(Char::isDigit)
    fun isValid(value: String): Boolean = pattern.matches(value)
    fun display(value: String): String = normalize(value).chunked(4).joinToString("-")
}
