package com.mahdiMb55.prices.data.local.session

import kotlinx.coroutines.flow.Flow

interface PairedSessionMetadataPreferences {
    val metadata: Flow<StoredPairedSessionMetadata?>
    suspend fun readOnce(): StoredPairedSessionMetadata?
    suspend fun save(metadata: StoredPairedSessionMetadata): MetadataPersistenceResult
    suspend fun clear(): MetadataPersistenceResult
}

sealed interface MetadataPersistenceResult {
    data object Success : MetadataPersistenceResult
    data object Failure : MetadataPersistenceResult
}
