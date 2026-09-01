package com.hitbosss.presentation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.hitbosss.domain.repository.AuthRepository

data class RecoverPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = email.isNotBlank()
}

@HiltViewModel
class RecoverPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(RecoverPasswordUiState())
    val state: StateFlow<RecoverPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun send() {
        val s = _state.value
        if (!s.isFormValid) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.sendPasswordReset(s.email)
                .onSuccess { _state.update { it.copy(isLoading = false, sent = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toRecoverMessage(appContext)) } }
        }
    }
}
