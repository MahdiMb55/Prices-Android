package com.mahdiMb55.prices.core.di

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer must be provided at the PricesApp boundary")
}
