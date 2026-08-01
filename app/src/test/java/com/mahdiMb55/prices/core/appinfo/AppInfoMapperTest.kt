package com.mahdiMb55.prices.core.appinfo

import org.junit.Assert.assertEquals
import org.junit.Test

class AppInfoMapperTest {
    @Test
    fun packageMetadataMapsToImmutableAppInfo() {
        val appInfo = mapAppInfo(
            packageName = "com.mahdiMb55.prices",
            isDebugBuild = true,
            packageMetadata = PackageMetadata(versionName = "2.4.1", versionCode = 241L),
            fallbackVersionName = "1.0",
            fallbackVersionCode = 1L
        )

        assertEquals(
            AppInfo(
                versionName = "2.4.1",
                versionCode = 241L,
                packageName = "com.mahdiMb55.prices",
                isDebugBuild = true
            ),
            appInfo
        )
    }

    @Test
    fun missingPackageMetadataUsesBuildFallbacks() {
        val appInfo = mapAppInfo(
            packageName = "com.mahdiMb55.prices",
            isDebugBuild = false,
            packageMetadata = null,
            fallbackVersionName = "1.0",
            fallbackVersionCode = 1L
        )

        assertEquals("1.0", appInfo.versionName)
        assertEquals(1L, appInfo.versionCode)
    }

    @Test
    fun blankVersionNameUsesBuildFallback() {
        val appInfo = mapAppInfo(
            packageName = "com.mahdiMb55.prices",
            isDebugBuild = false,
            packageMetadata = PackageMetadata(versionName = "  ", versionCode = 7L),
            fallbackVersionName = "1.0",
            fallbackVersionCode = 1L
        )

        assertEquals("1.0", appInfo.versionName)
        assertEquals(7L, appInfo.versionCode)
    }
}
