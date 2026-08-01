package com.mahdiMb55.prices.data.local.connection

import kotlinx.coroutines.flow.Flow

interface ConnectionPreferences {
    val connection: Flow<StoredConnection?>

    suspend fun save(connection: StoredConnection)

    suspend fun clear()
}
