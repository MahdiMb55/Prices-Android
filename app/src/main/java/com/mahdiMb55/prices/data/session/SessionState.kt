package com.mahdiMb55.prices.data.session

import com.mahdiMb55.prices.data.local.connection.StoredConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PairedSession(
    val deviceId: String,
    val deviceName: String,
    val userId: Long,
    val store: StoredConnection
)

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data class Authenticated(val session: PairedSession) : SessionState
}

interface SessionStore {
    val state: StateFlow<SessionState>
    fun authenticate(session: PairedSession)
    fun clear()
}

class InMemorySessionStore : SessionStore {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    override val state: StateFlow<SessionState> = mutableState.asStateFlow()
    override fun authenticate(session: PairedSession) { mutableState.value = SessionState.Authenticated(session) }
    override fun clear() { mutableState.value = SessionState.Unauthenticated }
}
