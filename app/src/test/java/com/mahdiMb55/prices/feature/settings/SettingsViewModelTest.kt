package com.mahdiMb55.prices.feature.settings

import com.mahdiMb55.prices.core.appinfo.AppInfo
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun injectedAppInfoMapsToSettingsUiState() {
        val provider = FakeAppInfoProvider(
            AppInfo(
                versionName = "3.2.1",
                versionCode = 321L,
                packageName = "com.mahdiMb55.prices",
                isDebugBuild = true
            )
        )

        val viewModel = SettingsViewModel(provider)

        assertEquals(
            SettingsUiState(
                versionName = "3.2.1",
                versionCode = 321L,
                packageName = "com.mahdiMb55.prices",
                isDebugBuild = true
            ),
            viewModel.uiState
        )
    }

    private class FakeAppInfoProvider(
        override val appInfo: AppInfo
    ) : AppInfoProvider
}
