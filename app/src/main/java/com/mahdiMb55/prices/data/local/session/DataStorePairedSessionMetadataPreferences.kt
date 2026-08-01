package com.mahdiMb55.prices.data.local.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pairedSessionMetadataDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "prices_paired_session_metadata",
)

class DataStorePairedSessionMetadataPreferences(context: Context) : PairedSessionMetadataPreferences {
    private val dataStore = context.applicationContext.pairedSessionMetadataDataStore

    override val metadata: Flow<StoredPairedSessionMetadata?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw exception
        }
        .map(::readMetadata)

    override suspend fun readOnce(): StoredPairedSessionMetadata? = metadata.first()

    override suspend fun save(metadata: StoredPairedSessionMetadata): MetadataPersistenceResult = try {
        dataStore.edit { preferences ->
            preferences[SchemaVersion] = metadata.schemaVersion
            preferences[DeviceId] = metadata.deviceId
            preferences[DeviceName] = metadata.deviceName
            preferences[UserId] = metadata.userId
            metadata.userDisplayName?.let { preferences[UserDisplayName] = it }
                ?: preferences.remove(UserDisplayName)
            preferences[AuthenticationMethod] = metadata.authenticationMethod
            preferences[TokenType] = metadata.tokenType
            preferences[PairingCodes] = metadata.capabilities.pairingCodes
            preferences[QrPairing] = metadata.capabilities.qrPairing
            preferences[DeviceTokens] = metadata.capabilities.deviceTokens
            preferences[PairedAt] = metadata.pairedAtEpochMillis
            preferences[ConnectionApiBaseUrl] = metadata.connectionApiBaseUrl
        }
        MetadataPersistenceResult.Success
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        MetadataPersistenceResult.Failure
    }

    override suspend fun clear(): MetadataPersistenceResult = try {
        dataStore.edit { it.clear() }
        MetadataPersistenceResult.Success
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        MetadataPersistenceResult.Failure
    }

    private fun readMetadata(preferences: Preferences): StoredPairedSessionMetadata? {
        return try {
            val schemaVersion = preferences[SchemaVersion] ?: return null
            if (schemaVersion != StoredPairedSessionMetadata.CurrentSchemaVersion) return null
            StoredPairedSessionMetadata(
                deviceId = preferences[DeviceId] ?: return null,
                deviceName = preferences[DeviceName] ?: return null,
                userId = preferences[UserId] ?: return null,
                userDisplayName = preferences[UserDisplayName],
                authenticationMethod = preferences[AuthenticationMethod] ?: return null,
                tokenType = preferences[TokenType] ?: return null,
                capabilities = AuthenticationCapabilitiesSnapshot(
                    pairingCodes = preferences[PairingCodes] ?: return null,
                    qrPairing = preferences[QrPairing] ?: return null,
                    deviceTokens = preferences[DeviceTokens] ?: return null,
                ),
                pairedAtEpochMillis = preferences[PairedAt] ?: return null,
                connectionApiBaseUrl = preferences[ConnectionApiBaseUrl] ?: return null,
                schemaVersion = schemaVersion,
            )
        } catch (_: Exception) {
            null
        }
    }

    internal companion object {
        const val DATA_STORE_NAME = "prices_paired_session_metadata"
        val SchemaVersion = intPreferencesKey("schema_version")
        val DeviceId = stringPreferencesKey("device_id")
        val DeviceName = stringPreferencesKey("device_name")
        val UserId = longPreferencesKey("user_id")
        val UserDisplayName = stringPreferencesKey("user_display_name")
        val AuthenticationMethod = stringPreferencesKey("authentication_method")
        val TokenType = stringPreferencesKey("token_type")
        val PairingCodes = booleanPreferencesKey("pairing_codes")
        val QrPairing = booleanPreferencesKey("qr_pairing")
        val DeviceTokens = booleanPreferencesKey("device_tokens")
        val PairedAt = longPreferencesKey("paired_at")
        val ConnectionApiBaseUrl = stringPreferencesKey("connection_api_base_url")
    }
}
