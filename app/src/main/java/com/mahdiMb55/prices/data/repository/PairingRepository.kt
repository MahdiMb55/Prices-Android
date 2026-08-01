package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
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
import com.mahdiMb55.prices.data.session.SessionStore
import kotlinx.coroutines.flow.first

interface PairingRepository {
    suspend fun exchange(pairingCode: String, deviceName: String): PairingResult
    fun clearSession()
}

sealed interface PairingResult {
    data class Success(val session: PairedSession) : PairingResult
    data class Failure(val reason: PairingFailure, val backendCode: String? = null) : PairingResult
}

enum class PairingFailure {
    NoDiscoveredStore, InvalidCodeFormat, InvalidOrExpiredCode, TooManyAttempts,
    PairingDisabled, DeviceLimitReached, PermissionDenied, NetworkUnavailable, Timeout,
    TlsFailure, ServerError, InvalidResponse, VerificationFailed, Unknown
}

class DefaultPairingRepository(
    private val connectionPreferences: ConnectionPreferences,
    private val pricesApiFactory: PricesApiFactory,
    private val executor: NetworkRequestExecutor,
    private val tokenStore: MutableAccessTokenStore,
    private val sessionStore: SessionStore,
    private val appInfoProvider: AppInfoProvider,
    private val androidVersion: String
) : PairingRepository {
    override suspend fun exchange(pairingCode: String, deviceName: String): PairingResult {
        clearSession()
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
            clearSession()
            return PairingResult.Failure(PairingFailure.VerificationFailed)
        }
        val session = PairedSession(exchange.deviceId, deviceName.trim(), verification.value.data.userId, connection)
        sessionStore.authenticate(session)
        return PairingResult.Success(session)
    }

    override fun clearSession() { tokenStore.clear(); sessionStore.clear() }

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
