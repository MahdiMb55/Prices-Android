package com.mahdiMb55.prices.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mahdiMb55.prices.data.repository.SecureSessionRepository
import com.mahdiMb55.prices.data.repository.StartupResolution
import com.mahdiMb55.prices.data.repository.StartupSessionResolver
import com.mahdiMb55.prices.data.repository.StartupVerificationFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data object VerifyingSession : StartupUiState
    data object Onboarding : StartupUiState
    data object Pairing : StartupUiState
    data class Products(val storeName: String) : StartupUiState
    data class RetryableVerificationFailure(
        val reason: StartupVerificationFailure,
        val storeName: String,
    ) : StartupUiState
    data class StorageFailure(val storeName: String?) : StartupUiState
}

class StartupViewModel(
    private val resolver: StartupSessionResolver,
    private val secureSessionRepository: SecureSessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = mutableUiState.asStateFlow()
    private var resolutionJob: Job? = null
    private var attemptId = 0L

    init {
        initialize()
    }

    fun initialize() {
        if (resolutionJob?.isActive == true) return
        startResolution()
    }

    fun retryVerification() {
        if (resolutionJob?.isActive == true) return
        if (mutableUiState.value !is StartupUiState.RetryableVerificationFailure &&
            mutableUiState.value !is StartupUiState.StorageFailure
        ) return
        startResolution()
    }

    fun returnToPairing() {
        cancelResolution()
        resolutionJob = viewModelScope.launch {
            secureSessionRepository.clearLocalSession()
            mutableUiState.value = StartupUiState.Pairing
        }
    }

    fun changeStore() {
        cancelResolution()
        resolutionJob = viewModelScope.launch {
            secureSessionRepository.clearAllForStoreChange()
            mutableUiState.value = StartupUiState.Onboarding
        }
    }

    private fun startResolution() {
        val currentAttempt = ++attemptId
        resolutionJob = viewModelScope.launch {
            mutableUiState.value = StartupUiState.VerifyingSession
            val resolution = try {
                resolver.resolve()
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
            if (currentAttempt != attemptId) return@launch
            mutableUiState.value = resolution.toUiState()
        }
    }

    private fun cancelResolution() {
        attemptId++
        resolutionJob?.cancel()
        resolutionJob = null
    }

    private fun StartupResolution.toUiState(): StartupUiState = when (this) {
        StartupResolution.Onboarding -> StartupUiState.Onboarding
        StartupResolution.Pairing -> StartupUiState.Pairing
        is StartupResolution.Products -> StartupUiState.Products(storeName)
        is StartupResolution.RetryableVerificationFailure -> StartupUiState.RetryableVerificationFailure(reason, storeName)
        is StartupResolution.StorageFailure -> StartupUiState.StorageFailure(storeName)
    }
}

class StartupViewModelFactory(
    private val resolver: StartupSessionResolver,
    private val secureSessionRepository: SecureSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StartupViewModel(resolver, secureSessionRepository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
