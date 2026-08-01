package com.mahdiMb55.prices.data.local.session

import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot

data class StoredPairedSessionMetadata(
    val deviceId: String,
    val deviceName: String,
    val userId: Long,
    val userDisplayName: String? = null,
    val authenticationMethod: String,
    val tokenType: String,
    val capabilities: AuthenticationCapabilitiesSnapshot,
    val pairedAtEpochMillis: Long,
    val connectionApiBaseUrl: String,
    val schemaVersion: Int = CurrentSchemaVersion,
) {
    override fun toString(): String = "StoredPairedSessionMetadata(redacted)"

    companion object {
        const val CurrentSchemaVersion = 1
    }
}
