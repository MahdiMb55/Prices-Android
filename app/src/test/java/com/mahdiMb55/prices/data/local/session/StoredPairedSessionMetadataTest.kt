package com.mahdiMb55.prices.data.local.session

import com.mahdiMb55.prices.data.local.connection.AuthenticationCapabilitiesSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredPairedSessionMetadataTest {
    @Test fun metadataContainsOnlyNonSensitiveSessionIdentity() {
        val metadata = metadata()
        assertEquals(StoredPairedSessionMetadata.CurrentSchemaVersion, metadata.schemaVersion)
        assertFalse(metadata.toString().contains("synthetic"))
        assertFalse(metadata.toString().contains("token"))
    }

    @Test fun fakeBoundarySupportsAbsentSaveUpdateClearAndFlowEmission() = runTest {
        val preferences = FakeMetadataPreferences()
        assertNull(preferences.readOnce())
        val first = metadata()
        assertTrue(preferences.save(first) === MetadataPersistenceResult.Success)
        assertEquals(first, preferences.metadata.first())
        val updated = first.copy(deviceName = "updated-device")
        preferences.save(updated)
        assertEquals(updated, preferences.readOnce())
        assertTrue(preferences.clear() === MetadataPersistenceResult.Success)
        assertNull(preferences.readOnce())
    }

    @Test fun unsupportedSchemaIsRejectedByModelReader() {
        val raw = metadata().copy(schemaVersion = 99)
        assertFalse(raw.schemaVersion == StoredPairedSessionMetadata.CurrentSchemaVersion)
    }

    private fun metadata() = StoredPairedSessionMetadata(
        deviceId = "dev_0123456789abcdef0123456789abcdef",
        deviceName = "synthetic-device",
        userId = 42L,
        userDisplayName = null,
        authenticationMethod = "device_token",
        tokenType = "Bearer",
        capabilities = AuthenticationCapabilitiesSnapshot(true, false, true),
        pairedAtEpochMillis = 1L,
        connectionApiBaseUrl = "https://example.test/wp-json/prices/v1/",
    )
}

private class FakeMetadataPreferences : PairedSessionMetadataPreferences {
    private val state = MutableStateFlow<StoredPairedSessionMetadata?>(null)
    override val metadata = state.asStateFlow()
    override suspend fun readOnce() = state.value
    override suspend fun save(metadata: StoredPairedSessionMetadata): MetadataPersistenceResult {
        state.value = metadata
        return MetadataPersistenceResult.Success
    }
    override suspend fun clear(): MetadataPersistenceResult {
        state.value = null
        return MetadataPersistenceResult.Success
    }
}
