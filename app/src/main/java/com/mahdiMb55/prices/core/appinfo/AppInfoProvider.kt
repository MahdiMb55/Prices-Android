package com.mahdiMb55.prices.core.appinfo

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val packageName: String,
    val isDebugBuild: Boolean
)

interface AppInfoProvider {
    val appInfo: AppInfo
}

internal data class PackageMetadata(
    val versionName: String?,
    val versionCode: Long
)

internal fun mapAppInfo(
    packageName: String,
    isDebugBuild: Boolean,
    packageMetadata: PackageMetadata?,
    fallbackVersionName: String,
    fallbackVersionCode: Long
): AppInfo = AppInfo(
    versionName = packageMetadata?.versionName?.takeIf(String::isNotBlank) ?: fallbackVersionName,
    versionCode = packageMetadata?.versionCode ?: fallbackVersionCode,
    packageName = packageName,
    isDebugBuild = isDebugBuild
)

class AndroidAppInfoProvider(application: Application) : AppInfoProvider {
    override val appInfo: AppInfo = mapAppInfo(
        packageName = application.packageName,
        isDebugBuild = application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        packageMetadata = application.readPackageMetadata(),
        fallbackVersionName = "—",
        fallbackVersionCode = 0L
    )
}

@Suppress("DEPRECATION")
private fun Application.readPackageMetadata(): PackageMetadata? =
    try {
        packageManager.getPackageInfo(packageName, 0).let { packageInfo ->
            PackageMetadata(
                versionName = packageInfo.versionName,
                versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            )
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
