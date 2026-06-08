package com.hitbosss.presentation.feature.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetEventUseCase
import com.hitbosss.domain.usecase.GetUserProfileUseCase
import com.hitbosss.domain.usecase.LeaveEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val isLoading: Boolean = true,
    val event: EventDetail? = null,
    val currentUserId: String? = null,
    val currentUserCountry: String? = null,
    val currentUserPicUrl: String? = null,
    val error: String? = null,
    val leaving: Boolean = false,
    val left: Boolean = false,
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val getEvent: GetEventUseCase,
    private val leaveEvent: LeaveEventUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: Int = savedStateHandle.get<Int>("id") ?: 0

    private val _state = MutableStateFlow(EventDetailUiState(currentUserId = getCurrentUser()?.uid))
    val state: StateFlow<EventDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getEvent(eventId)
                .onSuccess { e -> _state.update { it.copy(isLoading = false, event = e) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Error") } }
        }
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                getUserProfile(uid).onSuccess { p -> _state.update { it.copy(currentUserCountry = p.countryCode, currentUserPicUrl = p.profilePicUrl) } }
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            _state.update { it.copy(leaving = true) }
            leaveEvent(eventId)
                .onSuccess { _state.update { it.copy(leaving = false, left = true) } }
                .onFailure { e -> _state.update { it.copy(leaving = false, error = e.message ?: "No se pudo salir") } }
        }
    }
}
