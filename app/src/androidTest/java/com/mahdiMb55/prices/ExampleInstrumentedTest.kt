package com.mahdiMb55.prices

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import com.mahdiMb55.prices.core.di.AppContainerOwner

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun runtimeApplicationOwnsAvailableAppContainer() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

        assertTrue(appContext is PricesApplication)
        assertTrue(appContext is AppContainerOwner)
        val appInfo = (appContext as AppContainerOwner).appContainer.appInfoProvider.appInfo
        assertEquals("com.mahdiMb55.prices", appInfo.packageName)
        assertTrue(appInfo.versionName.isNotBlank())
        assertTrue(appInfo.versionCode >= 0L)
        val appContainer = (appContext as AppContainerOwner).appContainer
        assertSame(appContainer.pricesApiFactory, appContainer.pricesApiFactory)
    }
}
