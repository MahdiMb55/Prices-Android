package com.mahdiMb55.prices.core.di

import android.app.Application
import com.mahdiMb55.prices.core.appinfo.AndroidAppInfoProvider
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider

interface AppContainer {
    val appInfoProvider: AppInfoProvider
}

interface AppContainerOwner {
    val appContainer: AppContainer
}

class DefaultAppContainer(application: Application) : AppContainer {
    override val appInfoProvider: AppInfoProvider = AndroidAppInfoProvider(application)
}
