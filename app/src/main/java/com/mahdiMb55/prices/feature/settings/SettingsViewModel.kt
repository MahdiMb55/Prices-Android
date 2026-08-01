package com.mahdiMb55.prices.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mahdiMb55.prices.core.appinfo.AppInfoProvider

data class SettingsUiState(
    val versionName: String,
    val versionCode: Long,
    val packageName: String,
    val isDebugBuild: Boolean
)

class SettingsViewModel(appInfoProvider: AppInfoProvider) : ViewModel() {
    val uiState: SettingsUiState = appInfoProvider.appInfo.let { appInfo ->
        SettingsUiState(
            versionName = appInfo.versionName,
            versionCode = appInfo.versionCode,
            packageName = appInfo.packageName,
            isDebugBuild = appInfo.isDebugBuild
        )
    }
}

class SettingsViewModelFactory(
    private val appInfoProvider: AppInfoProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(appInfoProvider) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
