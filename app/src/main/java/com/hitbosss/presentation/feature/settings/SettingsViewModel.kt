package com.hitbosss.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.SignOutUseCase
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val signOut: SignOutUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _loggedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() = _error.update { null }

    fun logout() {
        signOut()
        _loggedOut.tryEmit(Unit)
    }

    fun deleteAccount() {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _deleting.update { true }
            userRepository.deleteAccount(uid)
                .onSuccess {
                    signOut()
                    _deleting.update { false }
                    _loggedOut.tryEmit(Unit)
                }
                .onFailure { e ->
                    _deleting.update { false }
                    _error.update { context.getString(R.string.err_delete_account) }
                }
        }
    }
}
