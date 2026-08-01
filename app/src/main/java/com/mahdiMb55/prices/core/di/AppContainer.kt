package com.mahdiMb55.prices.core.di

import android.app.Application
import com.mahdiMb55.prices.core.appinfo.AndroidAppInfoProvider
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import com.mahdiMb55.prices.data.remote.PricesApiFactory
import com.mahdiMb55.prices.data.remote.NoTokenAccessTokenProvider

interface AppContainer {
    val appInfoProvider: AppInfoProvider
    val pricesApiFactory: PricesApiFactory
}

interface AppContainerOwner {
    val appContainer: AppContainer
}

class DefaultAppContainer(application: Application) : AppContainer {
    override val appInfoProvider: AppInfoProvider = AndroidAppInfoProvider(application)
    override val pricesApiFactory: PricesApiFactory = PricesApiFactory(NoTokenAccessTokenProvider)
}
