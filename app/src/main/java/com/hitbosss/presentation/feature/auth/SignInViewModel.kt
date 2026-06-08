package com.hitbosss.presentation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.usecase.PostLoginDestination
import com.hitbosss.domain.usecase.ResolvePostLoginUseCase
import com.hitbosss.domain.usecase.SignInWithEmailPasswordUseCase
import com.hitbosss.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean get() = email.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInWithEmailPassword: SignInWithEmailPasswordUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val resolvePostLogin: ResolvePostLoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthNavEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthNavEvent> = _events.asSharedFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value) }
    fun clearError() = _state.update { it.copy(error = null) }
    fun onGoogleError(message: String?) = _state.update { it.copy(isLoading = false, error = message) }

    fun signIn() {
        val s = _state.value
        if (!s.isFormValid) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            signInWithEmailPassword(s.email, s.password)
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toAuthMessage()) } }
                .onSuccess { resolveDestination() }
        }
    }

    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            signInWithGoogle(idToken)
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toAuthMessage()) } }
                .onSuccess { resolveDestination() }
        }
    }

    fun setLoading() = _state.update { it.copy(isLoading = true, error = null) }

    private suspend fun resolveDestination() {
        when (resolvePostLogin()) {
            PostLoginDestination.MAIN -> { _state.update { it.copy(isLoading = false) }; _events.tryEmit(AuthNavEvent.ToMain) }
            PostLoginDestination.COMPLETE_PROFILE -> { _state.update { it.copy(isLoading = false) }; _events.tryEmit(AuthNavEvent.ToCompleteProfile) }
            PostLoginDestination.STAY -> _state.update {
                it.copy(isLoading = false, error = "No se pudo verificar tu cuenta. Inténtalo de nuevo.")
            }
        }
    }
}
