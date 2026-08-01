package com.mahdiMb55.prices.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mahdiMb55.prices.data.local.connection.ConnectionPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data class Ready(val destination: StartupDestination) : StartupUiState
}

class StartupViewModel(connectionPreferences: ConnectionPreferences) : ViewModel() {
    private val mutableUiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableUiState.value = StartupUiState.Ready(StartupDestination.from(connectionPreferences.connection.first()))
        }
    }
}

class StartupViewModelFactory(
    private val connectionPreferences: ConnectionPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StartupViewModel(connectionPreferences) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
