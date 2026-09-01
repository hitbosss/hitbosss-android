package com.hitbosss.presentation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.repository.AuthRepository
import com.hitbosss.domain.usecase.PostLoginDestination
import com.hitbosss.domain.usecase.ResolvePostLoginUseCase
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
import com.hitbosss.R
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    // 1:1 con iOS SignUpViewModel.isFormValid: no vacío + coincide (Firebase valida la fuerza).
    val isFormValid: Boolean
        get() = email.isNotBlank() && password.isNotBlank() &&
            confirmPassword.isNotBlank() && password == confirmPassword

    // Validaciones en vivo (se muestran solo cuando el campo ya tiene contenido).
    val passwordTooShort: Boolean get() = password.isNotEmpty() && password.length < 6
    val passwordsMismatch: Boolean get() = confirmPassword.isNotEmpty() && password != confirmPassword
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context,
    private val resolvePostLogin: ResolvePostLoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthNavEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthNavEvent> = _events.asSharedFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v) }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v) }
    fun clearError() = _state.update { it.copy(error = null) }
    fun setLoading() = _state.update { it.copy(isLoading = true, error = null) }
    fun onGoogleError(message: String?) = _state.update { it.copy(isLoading = false, error = appContext.getString(R.string.common_unexpected_error_msg)) }

    fun signUp() {
        val s = _state.value
        if (!s.isFormValid) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.createUserWithEmailPassword(s.email, s.password)
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toAuthMessage(appContext)) } }
                .onSuccess { resolveDestination() }
        }
    }

    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithGoogle(idToken)
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.toAuthMessage(appContext)) } }
                .onSuccess { resolveDestination() }
        }
    }

    private suspend fun resolveDestination() {
        when (resolvePostLogin()) {
            PostLoginDestination.MAIN -> { _state.update { it.copy(isLoading = false) }; _events.tryEmit(AuthNavEvent.ToMain) }
            PostLoginDestination.COMPLETE_PROFILE -> { _state.update { it.copy(isLoading = false) }; _events.tryEmit(AuthNavEvent.ToCompleteProfile) }
            PostLoginDestination.STAY -> _state.update { it.copy(isLoading = false, error = appContext.getString(R.string.err_complete_signup)) }
        }
    }
}
