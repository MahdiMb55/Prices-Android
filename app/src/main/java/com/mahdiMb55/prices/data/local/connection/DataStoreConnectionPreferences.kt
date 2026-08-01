package com.mahdiMb55.prices.data.local.connection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mahdiMb55.prices.data.remote.PricesNetworkJson
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private val featureFlagsSerializer = MapSerializer(String.serializer(), Boolean.serializer())

private val Context.connectionDataStore: DataStore<Preferences> by preferencesDataStore(name = "prices_discovered_store")

class DataStoreConnectionPreferences(context: Context) : ConnectionPreferences {
    private val dataStore = context.applicationContext.connectionDataStore

    override val connection: Flow<StoredConnection?> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw exception }
        .map(::readConnection)

    override suspend fun save(connection: StoredConnection) {
        dataStore.edit { preferences ->
            preferences[SchemaVersion] = connection.schemaVersion
            preferences[Phase] = connection.phase.name
            preferences[ApiBaseUrl] = connection.apiBaseUrl
            preferences[SiteUrl] = connection.siteUrl
            preferences[SiteName] = connection.siteName
            preferences[PluginName] = connection.pluginName
            preferences[PluginVersion] = connection.pluginVersion
            preferences[ApiVersion] = connection.apiVersion
            preferences[WooCommerceAvailable] = connection.woocommerceAvailable
            connection.minimumAppVersion?.let { preferences[MinimumAppVersion] = it } ?: preferences.remove(MinimumAppVersion)
            connection.minimumAppVersionWarning?.let { preferences[MinimumAppVersionWarning] = it } ?: preferences.remove(MinimumAppVersionWarning)
            preferences[Currency] = connection.currency
            preferences[CurrencyFormat] = connection.currencyFormat
            preferences[FeatureFlags] = PricesNetworkJson.instance.encodeToString(
                featureFlagsSerializer,
                connection.featureFlags
            )
            preferences[PairingCodes] = connection.authenticationCapabilities.pairingCodes
            preferences[QrPairing] = connection.authenticationCapabilities.qrPairing
            preferences[DeviceTokens] = connection.authenticationCapabilities.deviceTokens
            preferences[DiscoveredAt] = connection.discoveredAtEpochMillis
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun readConnection(preferences: Preferences): StoredConnection? {
        return try {
            val schemaVersion = preferences[SchemaVersion] ?: return null
            if (
                schemaVersion != StoredConnection.CurrentSchemaVersion ||
                preferences[Phase] != ConnectionPhase.Discovered.name
            ) {
                return null
            }
            StoredConnection(
                apiBaseUrl = preferences[ApiBaseUrl] ?: return null,
                siteUrl = preferences[SiteUrl] ?: return null,
                siteName = preferences[SiteName] ?: return null,
                pluginName = preferences[PluginName] ?: return null,
                pluginVersion = preferences[PluginVersion] ?: return null,
                apiVersion = preferences[ApiVersion] ?: return null,
                woocommerceAvailable = preferences[WooCommerceAvailable] ?: return null,
                minimumAppVersion = preferences[MinimumAppVersion],
                minimumAppVersionWarning = preferences[MinimumAppVersionWarning],
                currency = preferences[Currency] ?: return null,
                currencyFormat = preferences[CurrencyFormat] ?: return null,
                featureFlags = PricesNetworkJson.instance.decodeFromString(
                    featureFlagsSerializer,
                    preferences[FeatureFlags] ?: return null
                ),
                authenticationCapabilities = AuthenticationCapabilitiesSnapshot(
                    pairingCodes = preferences[PairingCodes] ?: return null,
                    qrPairing = preferences[QrPairing] ?: return null,
                    deviceTokens = preferences[DeviceTokens] ?: return null
                ),
                discoveredAtEpochMillis = preferences[DiscoveredAt] ?: return null,
                schemaVersion = schemaVersion
            )
        } catch (_: SerializationException) {
            null
        }
    }

    private companion object {
        val SchemaVersion = intPreferencesKey("schema_version")
        val Phase = stringPreferencesKey("phase")
        val ApiBaseUrl = stringPreferencesKey("api_base_url")
        val SiteUrl = stringPreferencesKey("site_url")
        val SiteName = stringPreferencesKey("site_name")
        val PluginName = stringPreferencesKey("plugin_name")
        val PluginVersion = stringPreferencesKey("plugin_version")
        val ApiVersion = stringPreferencesKey("api_version")
        val WooCommerceAvailable = booleanPreferencesKey("woocommerce_available")
        val MinimumAppVersion = stringPreferencesKey("minimum_app_version")
        val MinimumAppVersionWarning = stringPreferencesKey("minimum_app_version_warning")
        val Currency = stringPreferencesKey("currency")
        val CurrencyFormat = stringPreferencesKey("currency_format")
        val FeatureFlags = stringPreferencesKey("feature_flags")
        val PairingCodes = booleanPreferencesKey("pairing_codes")
        val QrPairing = booleanPreferencesKey("qr_pairing")
        val DeviceTokens = booleanPreferencesKey("device_tokens")
        val DiscoveredAt = longPreferencesKey("discovered_at")
    }
}
