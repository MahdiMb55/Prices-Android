package com.mahdiMb55.prices

import android.app.Application
import com.mahdiMb55.prices.core.di.AppContainer
import com.mahdiMb55.prices.core.di.AppContainerOwner
import com.mahdiMb55.prices.core.di.DefaultAppContainer

class PricesApplication : Application(), AppContainerOwner {
    override val appContainer: AppContainer by lazy(LazyThreadSafetyMode.NONE) {
        DefaultAppContainer(this)
    }
}
