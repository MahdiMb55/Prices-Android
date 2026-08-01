package com.mahdiMb55.prices.feature.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mahdiMb55.prices.data.local.connection.StoredConnection
import com.mahdiMb55.prices.data.repository.PairingCodeFormat
import com.mahdiMb55.prices.data.repository.PairingFailure
import com.mahdiMb55.prices.data.repository.PairingRepository
import com.mahdiMb55.prices.data.repository.PairingResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PairingUiState(
    val connection: StoredConnection?,
    val pairingCode: String = "",
    val deviceName: String = "Prices Android",
    val isSubmitting: Boolean = false,
    val failure: PairingFailure? = null,
    val success: Boolean = false
) {
    val canSubmit: Boolean get() = connection != null && PairingCodeFormat.isValid(pairingCode) && deviceName.isNotBlank() && !isSubmitting
}

class PairingViewModel(
    private val repository: PairingRepository,
    connection: StoredConnection?
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PairingUiState(connection))
    val uiState: StateFlow<PairingUiState> = mutableUiState.asStateFlow()
    private var job: Job? = null
    private var requestId = 0L
    fun updateCode(value: String) { mutableUiState.value = mutableUiState.value.copy(pairingCode = PairingCodeFormat.normalize(value), failure = null) }
    fun updateDeviceName(value: String) { mutableUiState.value = mutableUiState.value.copy(deviceName = value, failure = null) }
    fun submit() {
        if (!mutableUiState.value.canSubmit || job?.isActive == true) return
        val id = ++requestId; val code = mutableUiState.value.pairingCode; val name = mutableUiState.value.deviceName
        mutableUiState.value = mutableUiState.value.copy(isSubmitting = true, failure = null)
        job = viewModelScope.launch { when (val result = repository.exchange(code, name)) {
            is PairingResult.Success -> if (id == requestId) mutableUiState.value = mutableUiState.value.copy(isSubmitting = false, pairingCode = "", success = true)
            is PairingResult.Failure -> if (id == requestId) mutableUiState.value = mutableUiState.value.copy(isSubmitting = false, failure = result.reason)
        } }
    }
    fun consumeSuccess() { mutableUiState.value = mutableUiState.value.copy(success = false) }
}

class PairingViewModelFactory(private val repository: PairingRepository, private val connection: StoredConnection?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PairingViewModel::class.java)) { @Suppress("UNCHECKED_CAST") return PairingViewModel(repository, connection) as T }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
