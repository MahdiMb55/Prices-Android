package com.mahdiMb55.prices.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mahdiMb55.prices.data.repository.StoreDiscoveryFailure
import com.mahdiMb55.prices.data.repository.StoreDiscoveryRepository
import com.mahdiMb55.prices.data.repository.StoreDiscoveryResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val storeUrl: String = "",
    val isDiscovering: Boolean = false,
    val error: StoreDiscoveryFailure? = null,
    val discoveryCompleted: Boolean = false
)

class OnboardingViewModel(
    private val repository: StoreDiscoveryRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()
    private var discoveryJob: Job? = null
    private var requestId = 0L

    fun updateStoreUrl(value: String) {
        mutableUiState.value = mutableUiState.value.copy(storeUrl = value, error = null)
    }

    fun discover() {
        discoveryJob?.cancel()
        val currentRequestId = ++requestId
        val url = mutableUiState.value.storeUrl
        mutableUiState.value = mutableUiState.value.copy(isDiscovering = true, error = null, discoveryCompleted = false)
        discoveryJob = viewModelScope.launch {
            when (val result = repository.discover(url)) {
                is StoreDiscoveryResult.Success -> if (currentRequestId == requestId) {
                    mutableUiState.value = mutableUiState.value.copy(isDiscovering = false, discoveryCompleted = true)
                }
                is StoreDiscoveryResult.Failure -> if (currentRequestId == requestId) {
                    mutableUiState.value = mutableUiState.value.copy(isDiscovering = false, error = result.reason)
                }
            }
        }
    }
}

class OnboardingViewModelFactory(
    private val repository: StoreDiscoveryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
