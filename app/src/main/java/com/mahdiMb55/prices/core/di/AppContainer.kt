package com.mahdiMb55.prices.core.di

import android.app.Application
import com.mahdiMb55.prices.core.appinfo.AndroidAppInfoProvider
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import com.mahdiMb55.prices.data.remote.NoTokenAccessTokenProvider
import com.mahdiMb55.prices.data.remote.ApiErrorParser
import com.mahdiMb55.prices.data.remote.NetworkRequestExecutor
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import com.mahdiMb55.prices.data.local.connection.DataStoreConnectionPreferences
import com.mahdiMb55.prices.data.repository.DefaultStoreDiscoveryRepository
import com.mahdiMb55.prices.data.repository.StoreDiscoveryRepository

interface AppContainer {
    val appInfoProvider: AppInfoProvider
    val pricesApiFactory: PricesApiFactory
    val connectionPreferences: ConnectionPreferences
    val storeDiscoveryRepository: StoreDiscoveryRepository
}

interface AppContainerOwner {
    val appContainer: AppContainer
}

class DefaultAppContainer(application: Application) : AppContainer {
    override val appInfoProvider: AppInfoProvider = AndroidAppInfoProvider(application)
    override val pricesApiFactory: PricesApiFactory = PricesApiFactory(NoTokenAccessTokenProvider)
    override val connectionPreferences: ConnectionPreferences = DataStoreConnectionPreferences(application)
    override val storeDiscoveryRepository: StoreDiscoveryRepository = DefaultStoreDiscoveryRepository(
        pricesApiFactory = pricesApiFactory,
        networkRequestExecutor = NetworkRequestExecutor(ApiErrorParser()),
        connectionPreferences = connectionPreferences,
        appInfoProvider = appInfoProvider,
        now = { System.currentTimeMillis() }
    )
}
