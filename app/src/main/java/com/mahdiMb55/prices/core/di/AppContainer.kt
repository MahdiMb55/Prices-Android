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
import com.mahdiMb55.prices.data.session.InMemorySessionStore
import com.mahdiMb55.prices.data.session.SessionStore

interface AppContainer {
    val appInfoProvider: AppInfoProvider
    val pricesApiFactory: PricesApiFactory
    val connectionPreferences: ConnectionPreferences
    val storeDiscoveryRepository: StoreDiscoveryRepository
    val accessTokenStore: MutableAccessTokenStore
    val sessionStore: SessionStore
    val pairingRepository: PairingRepository
}

interface AppContainerOwner {
    val appContainer: AppContainer
}

class DefaultAppContainer(application: Application) : AppContainer {
    override val appInfoProvider: AppInfoProvider = AndroidAppInfoProvider(application)
    override val accessTokenStore: MutableAccessTokenStore = InMemoryAccessTokenProvider()
    override val sessionStore: SessionStore = InMemorySessionStore()
    override val pricesApiFactory: PricesApiFactory = PricesApiFactory(accessTokenStore)
    override val connectionPreferences: ConnectionPreferences = DataStoreConnectionPreferences(application)
    override val storeDiscoveryRepository: StoreDiscoveryRepository = DefaultStoreDiscoveryRepository(
        pricesApiFactory = pricesApiFactory,
        networkRequestExecutor = NetworkRequestExecutor(ApiErrorParser()),
        connectionPreferences = connectionPreferences,
        appInfoProvider = appInfoProvider,
        now = { System.currentTimeMillis() }
    )
    override val pairingRepository: PairingRepository = DefaultPairingRepository(
        connectionPreferences, pricesApiFactory, NetworkRequestExecutor(ApiErrorParser()), accessTokenStore,
        sessionStore, appInfoProvider, Build.VERSION.RELEASE ?: "Android"
    )
}
