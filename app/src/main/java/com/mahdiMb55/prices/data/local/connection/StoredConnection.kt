package com.mahdiMb55.prices.data.local.connection

data class StoredConnection(
    val apiBaseUrl: String,
    val siteUrl: String,
    val siteName: String,
    val pluginName: String,
    val pluginVersion: String,
    val apiVersion: String,
    val woocommerceAvailable: Boolean,
    val minimumAppVersion: String?,
    val minimumAppVersionWarning: String?,
    val currency: String,
    val currencyFormat: String,
    val featureFlags: Map<String, Boolean>,
    val authenticationCapabilities: AuthenticationCapabilitiesSnapshot,
    val discoveredAtEpochMillis: Long,
    val schemaVersion: Int = CurrentSchemaVersion,
    val phase: ConnectionPhase = ConnectionPhase.Discovered
) {
    companion object {
        const val CurrentSchemaVersion = 1
    }
}

enum class ConnectionPhase { Discovered }

data class AuthenticationCapabilitiesSnapshot(
    val pairingCodes: Boolean,
    val qrPairing: Boolean,
    val deviceTokens: Boolean
)
