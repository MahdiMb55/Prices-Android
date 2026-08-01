package com.mahdiMb55.prices.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class DiscoveryResponseDto(
    @SerialName("plugin_name") val pluginName: String,
    @SerialName("plugin_version") val pluginVersion: String,
    @SerialName("api_version") val apiVersion: String,
    @SerialName("woocommerce_available") val woocommerceAvailable: Boolean,
    @SerialName("minimum_app_version") val minimumAppVersion: String?,
    @SerialName("authentication_methods") val authenticationMethods: List<String>,
    val authentication: AuthenticationCapabilitiesDto,
    val features: Map<String, Boolean>,
    @SerialName("site_name") val siteName: String,
    @SerialName("site_url") val siteUrl: String,
    val currency: String,
    @SerialName("currency_format") val currencyFormat: String,
    val settings: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class AuthenticationCapabilitiesDto(
    @SerialName("pairing_codes") val pairingCodes: Boolean,
    @SerialName("qr_pairing") val qrPairing: Boolean,
    @SerialName("device_tokens") val deviceTokens: Boolean,
    @SerialName("application_passwords") val applicationPasswords: Boolean,
    val oauth: Boolean,
    @SerialName("custom_access_refresh_tokens") val customAccessRefreshTokens: Boolean
)

@Serializable
internal data class ApiErrorEnvelopeDto(val error: ApiErrorDto)

@Serializable
internal data class ApiErrorDto(
    val code: String,
    val message: String,
    val details: JsonElement,
    @SerialName("request_id") val requestId: String
)

@Serializable
data class PairingExchangeRequestDto(
    @SerialName("pairing_code") val pairingCode: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_identifier") val deviceIdentifier: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("android_version") val androidVersion: String
)

@Serializable
data class PairingExchangeEnvelopeDto(val data: PairingExchangeDataDto) {
    override fun toString(): String = "PairingExchangeEnvelopeDto(data=redacted)"
}

@Serializable
data class PairingExchangeDataDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("device_token") val deviceToken: String,
    @SerialName("authentication_method") val authenticationMethod: String
) {
    override fun toString(): String = "PairingExchangeDataDto(deviceId=$deviceId, tokenType=$tokenType, deviceToken=redacted, authenticationMethod=$authenticationMethod)"
}

@Serializable
data class CurrentSessionEnvelopeDto(val data: CurrentSessionDataDto)

@Serializable
data class CurrentSessionDataDto(
    @SerialName("user_id") val userId: Long,
    @SerialName("device_id") val deviceId: String,
    @SerialName("authentication_method") val authenticationMethod: String
)
