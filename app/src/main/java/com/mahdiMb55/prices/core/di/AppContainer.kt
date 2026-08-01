package com.mahdiMb55.prices.core.di

import android.app.Application
import android.os.Build
import com.mahdiMb55.prices.core.appinfo.AndroidAppInfoProvider
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import com.mahdiMb55.prices.data.remote.NoTokenAccessTokenProvider
import com.mahdiMb55.prices.data.remote.InMemoryAccessTokenProvider
import com.mahdiMb55.prices.data.remote.MutableAccessTokenStore
import com.mahdiMb55.prices.data.remote.ApiErrorParser
import com.mahdiMb55.prices.data.remote.NetworkRequestExecutor
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.connection.DataStoreConnectionPreferences
import com.mahdiMb55.prices.data.repository.DefaultStoreDiscoveryRepository
import com.mahdiMb55.prices.data.repository.StoreDiscoveryRepository
import com.mahdiMb55.prices.data.repository.DefaultPairingRepository
import com.mahdiMb55.prices.data.repository.PairingRepository
import com.mahdiMb55.prices.data.repository.DefaultSecureSessionRepository
import com.mahdiMb55.prices.data.repository.SecureSessionRepository
import com.mahdiMb55.prices.data.repository.DefaultStartupSessionResolver
import com.mahdiMb55.prices.data.repository.StartupSessionResolver
import com.mahdiMb55.prices.data.local.session.DataStorePairedSessionMetadataPreferences
import com.mahdiMb55.prices.data.local.session.PairedSessionMetadataPreferences
import com.mahdiMb55.prices.data.security.AndroidKeystoreTokenStorage
import com.mahdiMb55.prices.data.security.SecureTokenStorage
import com.mahdiMb55.prices.data.session.InMemorySessionStore
import com.mahdiMb55.prices.data.session.SessionStore

interface AppContainer {
    val appInfoProvider: AppInfoProvider
    val pricesApiFactory: PricesApiFactory
    val connectionPreferences: ConnectionPreferences
    val storeDiscoveryRepository: StoreDiscoveryRepository
    val accessTokenStore: MutableAccessTokenStore
    val sessionStore: SessionStore
    val secureTokenStorage: SecureTokenStorage
    val pairedSessionMetadataPreferences: PairedSessionMetadataPreferences
    val secureSessionRepository: SecureSessionRepository
    val startupSessionResolver: StartupSessionResolver
    val pairingRepository: PairingRepository
}

interface AppContainerOwner {
    val appContainer: AppContainer
}

class DefaultAppContainer(application: Application) : AppContainer {
    override val appInfoProvider: AppInfoProvider = AndroidAppInfoProvider(application)
    override val accessTokenStore: MutableAccessTokenStore = InMemoryAccessTokenProvider()
    override val sessionStore: SessionStore = InMemorySessionStore()
    override val connectionPreferences: ConnectionPreferences = DataStoreConnectionPreferences(application)
    override val secureTokenStorage: SecureTokenStorage = AndroidKeystoreTokenStorage(application)
    override val pairedSessionMetadataPreferences: PairedSessionMetadataPreferences =
        DataStorePairedSessionMetadataPreferences(application)
    override val secureSessionRepository: SecureSessionRepository = DefaultSecureSessionRepository(
        secureTokenStorage = secureTokenStorage,
        metadataPreferences = pairedSessionMetadataPreferences,
        tokenStore = accessTokenStore,
        sessionStore = sessionStore,
        connectionPreferences = connectionPreferences,
    )
    override val pricesApiFactory: PricesApiFactory = PricesApiFactory(accessTokenStore)
    override val startupSessionResolver: StartupSessionResolver = DefaultStartupSessionResolver(
        connectionPreferences = connectionPreferences,
        metadataPreferences = pairedSessionMetadataPreferences,
        secureTokenStorage = secureTokenStorage,
        tokenStore = accessTokenStore,
        sessionStore = sessionStore,
        secureSessionRepository = secureSessionRepository,
        pricesApiFactory = pricesApiFactory,
        networkRequestExecutor = NetworkRequestExecutor(ApiErrorParser()),
    )
    override val storeDiscoveryRepository: StoreDiscoveryRepository = DefaultStoreDiscoveryRepository(
        pricesApiFactory = pricesApiFactory,
        networkRequestExecutor = NetworkRequestExecutor(ApiErrorParser()),
        connectionPreferences = connectionPreferences,
        appInfoProvider = appInfoProvider,
        now = { System.currentTimeMillis() }
    )
    override val pairingRepository: PairingRepository = DefaultPairingRepository(
        connectionPreferences = connectionPreferences,
        pricesApiFactory = pricesApiFactory,
        executor = NetworkRequestExecutor(ApiErrorParser()),
        tokenStore = accessTokenStore,
        secureSessionRepository = secureSessionRepository,
        appInfoProvider = appInfoProvider,
        androidVersion = Build.VERSION.RELEASE ?: "Android",
    )
}
