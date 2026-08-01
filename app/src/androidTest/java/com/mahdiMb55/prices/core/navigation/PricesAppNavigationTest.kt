package com.mahdiMb55.prices.core.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import com.mahdiMb55.prices.app.PricesApp
import com.mahdiMb55.prices.core.appinfo.AppInfo
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.core.designsystem.PricesTheme
import com.mahdiMb55.prices.core.di.AppContainer
import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import com.mahdiMb55.prices.data.remote.NoTokenAccessTokenProvider
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import com.mahdiMb55.prices.data.remote.InMemoryAccessTokenProvider
import com.mahdiMb55.prices.data.remote.MutableAccessTokenStore
import com.mahdiMb55.prices.data.repository.StoreDiscoveryRepository
import com.mahdiMb55.prices.data.repository.PairingRepository
import com.mahdiMb55.prices.data.repository.PairingResult
import com.mahdiMb55.prices.data.session.InMemorySessionStore
import com.mahdiMb55.prices.data.session.SessionStore
import com.mahdiMb55.prices.data.repository.StoreDiscoveryResult
import com.mahdiMb55.prices.data.repository.SecureSessionRepository
import com.mahdiMb55.prices.data.repository.SessionCleanupResult
import com.mahdiMb55.prices.data.repository.PersistSessionResult
import com.mahdiMb55.prices.data.repository.StartupResolution
import com.mahdiMb55.prices.data.repository.StartupSessionResolver
import com.mahdiMb55.prices.data.local.session.PairedSessionMetadataPreferences
import com.mahdiMb55.prices.data.local.session.MetadataPersistenceResult
import com.mahdiMb55.prices.data.local.session.StoredPairedSessionMetadata
import com.mahdiMb55.prices.data.security.SecureTokenReadResult
import com.mahdiMb55.prices.data.security.SecureTokenStorage
import com.mahdiMb55.prices.data.security.SecureTokenWriteResult
import com.mahdiMb55.prices.data.session.PairedSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

class PricesAppNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun absentSnapshotStartsStoreDiscovery() {
        setAppContent(FakeConnectionPreferences(null))
        composeRule.onNodeWithText("Discover store").assertIsDisplayed()
    }

    @Test
    fun discoveredSnapshotStartsDiscoveredStoreScreen() {
        setAppContent(FakeConnectionPreferences(discoveredConnection()))
        composeRule.onNodeWithText("Status: Discovered").assertIsDisplayed()
    }

    @Test
    fun verifiedRestoredSessionStartsProductsWithoutPairingScreen() {
        val preferences = FakeConnectionPreferences(discoveredConnection())
        composeRule.setContent {
            PricesTheme(darkTheme = false, dynamicColor = false) {
                PricesApp(testAppContainer(preferences, StartupResolution.Products("Example Store")))
            }
        }
        composeRule.onNodeWithText("Products").assertIsDisplayed()
        composeRule.onAllNodesWithText("Connect device").assertCountEquals(0)
    }

    @Test
    fun retryableVerificationShowsTruthfulRetryAction() {
        val preferences = FakeConnectionPreferences(discoveredConnection())
        composeRule.setContent {
            PricesTheme(darkTheme = false, dynamicColor = false) {
                PricesApp(
                    testAppContainer(
                        preferences,
                        StartupResolution.RetryableVerificationFailure(
                            com.mahdiMb55.prices.data.repository.StartupVerificationFailure.RetryableNetworkFailure,
                            "Example Store",
                        ),
                    ),
                )
            }
        }
        composeRule.onNodeWithText("We could not verify this device right now.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun rtlDiscoveryRendersWithoutCrashing() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PricesTheme(darkTheme = false, dynamicColor = false) {
                    PricesApp(testAppContainer(FakeConnectionPreferences(null)))
                }
            }
        }
        composeRule.onNodeWithText("Discover store").assertIsDisplayed()
    }

    private fun setAppContent(preferences: ConnectionPreferences) {
        composeRule.setContent { PricesTheme(darkTheme = false, dynamicColor = false) { PricesApp(testAppContainer(preferences)) } }
    }

    private fun testAppContainer(
        preferences: ConnectionPreferences,
        startupResolution: StartupResolution? = null,
    ) = object : AppContainer {
        override val accessTokenStore: MutableAccessTokenStore = InMemoryAccessTokenProvider()
        override val sessionStore: SessionStore = InMemorySessionStore().apply {
            if (startupResolution is StartupResolution.Products) {
                authenticate(
                    PairedSession(
                        deviceId = "dev_0123456789abcdef0123456789abcdef",
                        deviceName = "test-device",
                        userId = 42L,
                        store = discoveredConnection(),
                    ),
                )
            }
        }
        override val pricesApiFactory = PricesApiFactory(accessTokenStore)
        override val connectionPreferences = preferences
        override val storeDiscoveryRepository = object : StoreDiscoveryRepository {
            override suspend fun discover(urlInput: String): StoreDiscoveryResult = error("Not used by this test")
        }
        override val pairingRepository = object : PairingRepository {
            override suspend fun exchange(pairingCode: String, deviceName: String): PairingResult = error("Not used by this test")
            override suspend fun clearSession() = SessionCleanupResult.Success
            override suspend fun clearForStoreChange() = SessionCleanupResult.Success
        }
        override val secureTokenStorage: SecureTokenStorage = object : SecureTokenStorage {
            override suspend fun read() = SecureTokenReadResult.Missing
            override suspend fun write(token: String) = SecureTokenWriteResult.Success
            override suspend fun clear() = SecureTokenWriteResult.Success
        }
        override val pairedSessionMetadataPreferences: PairedSessionMetadataPreferences = object : PairedSessionMetadataPreferences {
            override val metadata: Flow<StoredPairedSessionMetadata?> = MutableStateFlow(null)
            override suspend fun readOnce(): StoredPairedSessionMetadata? = null
            override suspend fun save(metadata: StoredPairedSessionMetadata) = MetadataPersistenceResult.Success
            override suspend fun clear() = MetadataPersistenceResult.Success
        }
        override val secureSessionRepository: SecureSessionRepository = object : SecureSessionRepository {
            override fun clearInMemorySession() = Unit
            override suspend fun persistVerifiedSession(token: String, metadata: StoredPairedSessionMetadata, session: PairedSession) = PersistSessionResult.Success
            override suspend fun clearLocalSession() = SessionCleanupResult.Success
            override suspend fun clearAllForStoreChange() = SessionCleanupResult.Success
        }
        override val startupSessionResolver: StartupSessionResolver = object : StartupSessionResolver {
            override suspend fun resolve() = startupResolution ?: if (preferences.connection.first() == null) {
                StartupResolution.Onboarding
            } else {
                StartupResolution.Pairing
            }
        }
        override val appInfoProvider: AppInfoProvider = object : AppInfoProvider {
            override val appInfo = AppInfo("9.8.7", 987L, "com.mahdiMb55.prices.test", true)
        }
    }

    private class FakeConnectionPreferences(initial: StoredConnection?) : ConnectionPreferences {
        private val value = MutableStateFlow(initial)
        override val connection: Flow<StoredConnection?> = value
        override suspend fun save(connection: StoredConnection) { value.value = connection }
        override suspend fun clear() { value.value = null }
    }

    private fun discoveredConnection() = StoredConnection(
        apiBaseUrl = "https://example.test/wp-json/prices/v1/", siteUrl = "https://example.test/",
        siteName = "Example Store", pluginName = "Prices", pluginVersion = "1.0", apiVersion = "prices/v1",
        woocommerceAvailable = true, minimumAppVersion = null, minimumAppVersionWarning = null,
        currency = "USD", currencyFormat = "%s", featureFlags = emptyMap(),
        authenticationCapabilities = AuthenticationCapabilitiesSnapshot(true, false, false), discoveredAtEpochMillis = 1
    )
}
