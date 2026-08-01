package com.mahdiMb55.prices.data.local.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStorePairedSessionMetadataPreferencesInstrumentedTest {
    private lateinit var preferences: DataStorePairedSessionMetadataPreferences

    @Before fun setUp() {
        preferences = DataStorePairedSessionMetadataPreferences(
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
        )
    }

    @After fun tearDown() = runTest {
        preferences.clear()
    }

    @Test fun initiallyAbsentAndClearAreSafe() = runTest {
        preferences.clear()
        assertNull(preferences.readOnce())
        assertTrue(preferences.clear() === MetadataPersistenceResult.Success)
    }

    @Test fun saveReadUpdateAndFlowEmissionWork() = runTest {
        val first = metadata("first")
        val updated = metadata("updated")
        preferences.save(first)
        assertEquals(first, preferences.metadata.first())
        preferences.save(updated)
        assertEquals(updated, preferences.readOnce())
    }

    @Test fun metadataHasNoTokenOrPairingCodeFields() = runTest {
        val saved = metadata("safe")
        preferences.save(saved)
        val printable = preferences.readOnce().toString()
        assertTrue(printable.contains("redacted"))
        assertTrue(!printable.contains("token"))
        assertTrue(!printable.contains("pairing"))
    }

    private fun metadata(deviceName: String) = StoredPairedSessionMetadata(
        deviceId = "dev_0123456789abcdef0123456789abcdef",
        deviceName = deviceName,
        userId = 42,
        authenticationMethod = "device_token",
        tokenType = "Bearer",
        capabilities = AuthenticationCapabilitiesSnapshot(true, false, true),
        pairedAtEpochMillis = 1,
        connectionApiBaseUrl = "https://example.test/wp-json/prices/v1/",
    )
}
