package com.mahdiMb55.prices.data.repository

import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import com.mahdiMb55.prices.data.remote.DiscoveryResponseDto
import com.mahdiMb55.prices.data.remote.NetworkErrorCategory
import com.mahdiMb55.prices.data.remote.NetworkRequestExecutor
import com.mahdiMb55.prices.data.remote.NetworkResult
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import com.mahdiMb55.prices.data.remote.StoreUrlNormalizationResult
import com.mahdiMb55.prices.data.remote.StoreUrlNormalizer

interface StoreDiscoveryRepository {
    suspend fun discover(urlInput: String): StoreDiscoveryResult
}

sealed interface StoreDiscoveryResult {
    data class Success(val connection: StoredConnection) : StoreDiscoveryResult
    data class Failure(val reason: StoreDiscoveryFailure, val backendCode: String? = null) : StoreDiscoveryResult
}

enum class StoreDiscoveryFailure {
    InvalidUrl, NetworkUnavailable, Timeout, TlsFailure, ServerError, InvalidResponse,
    PricesPluginUnavailable, WooCommerceUnavailable, UnsupportedApiVersion, AppUpdateRequired,
    PairingUnavailable, Unknown
}

class DefaultStoreDiscoveryRepository(
    private val pricesApiFactory: PricesApiFactory,
    private val networkRequestExecutor: NetworkRequestExecutor,
    private val connectionPreferences: ConnectionPreferences,
    private val appInfoProvider: AppInfoProvider,
    private val now: () -> Long
) : StoreDiscoveryRepository {
    override suspend fun discover(urlInput: String): StoreDiscoveryResult {
        val normalized = StoreUrlNormalizer.normalize(StoreUrlInput.prepare(urlInput))
        if (normalized !is StoreUrlNormalizationResult.Valid) return StoreDiscoveryResult.Failure(StoreDiscoveryFailure.InvalidUrl)
        return when (val network = networkRequestExecutor.execute { pricesApiFactory.create(normalized.baseUrl).discovery() }) {
            is NetworkResult.Failure -> StoreDiscoveryResult.Failure(network.error.toDiscoveryFailure(), network.error.apiCode)
            is NetworkResult.Success -> validateAndPersist(network.value, normalized.baseUrl.value)
        }
    }

    private suspend fun validateAndPersist(dto: DiscoveryResponseDto, apiBaseUrl: String): StoreDiscoveryResult {
        if (dto.pluginName.isBlank() || dto.pluginVersion.isBlank() || dto.siteName.isBlank() || dto.currency.isBlank()) {
            return StoreDiscoveryResult.Failure(StoreDiscoveryFailure.InvalidResponse)
        }
        if (!dto.woocommerceAvailable) return StoreDiscoveryResult.Failure(StoreDiscoveryFailure.WooCommerceUnavailable)
        when (ApiVersionPolicy.check(dto.apiVersion)) {
            ApiVersionCompatibility.Unsupported -> return StoreDiscoveryResult.Failure(StoreDiscoveryFailure.UnsupportedApiVersion)
            ApiVersionCompatibility.Malformed -> return StoreDiscoveryResult.Failure(StoreDiscoveryFailure.InvalidResponse)
            ApiVersionCompatibility.Supported -> Unit
        }
        if (!dto.authentication.pairingCodes) return StoreDiscoveryResult.Failure(StoreDiscoveryFailure.PairingUnavailable)
        val site = StoreUrlNormalizer.normalize(dto.siteUrl)
        if (site !is StoreUrlNormalizationResult.Valid) return StoreDiscoveryResult.Failure(StoreDiscoveryFailure.InvalidResponse)
        val minimumCheck = MinimumAppVersionPolicy.check(dto.minimumAppVersion, appInfoProvider.appInfo.versionName)
        if (minimumCheck == MinimumAppVersionCheck.UpdateRequired) return StoreDiscoveryResult.Failure(StoreDiscoveryFailure.AppUpdateRequired)
        val connection = StoredConnection(
            apiBaseUrl = apiBaseUrl,
            siteUrl = site.baseUrl.value.removeSuffix("wp-json/prices/v1/"),
            siteName = dto.siteName,
            pluginName = dto.pluginName,
            pluginVersion = dto.pluginVersion,
            apiVersion = dto.apiVersion,
            woocommerceAvailable = dto.woocommerceAvailable,
            minimumAppVersion = dto.minimumAppVersion,
            minimumAppVersionWarning = (minimumCheck as? MinimumAppVersionCheck.Malformed)?.value,
            currency = dto.currency,
            currencyFormat = dto.currencyFormat,
            featureFlags = dto.features,
            authenticationCapabilities = AuthenticationCapabilitiesSnapshot(dto.authentication.pairingCodes, dto.authentication.qrPairing, dto.authentication.deviceTokens),
            discoveredAtEpochMillis = now()
        )
        connectionPreferences.save(connection)
        return StoreDiscoveryResult.Success(connection)
    }

    private fun com.mahdiMb55.prices.data.remote.NetworkError.toDiscoveryFailure(): StoreDiscoveryFailure = when (category) {
        NetworkErrorCategory.Connectivity -> StoreDiscoveryFailure.NetworkUnavailable
        NetworkErrorCategory.Timeout -> StoreDiscoveryFailure.Timeout
        NetworkErrorCategory.Tls -> StoreDiscoveryFailure.TlsFailure
        NetworkErrorCategory.Server -> StoreDiscoveryFailure.ServerError
        else -> StoreDiscoveryFailure.Unknown
    }
}
