package com.hitbosss.presentation.feature.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.repository.CommunityRepository
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.hitbosss.R
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class EventDetailUiState(
    val isLoading: Boolean = true,
    val event: EventDetail? = null,
    val currentUserId: String? = null,
    val currentUserCountry: String? = null,
    val currentUserPicUrl: String? = null,
    val error: String? = null,
    val leaving: Boolean = false,
    val left: Boolean = false,
    val deleted: Boolean = false,
    val reportSent: Boolean = false,
    val reportFailed: Boolean = false,
) {
    /** ¿El usuario actual es admin del evento? (el creador siempre cuenta, fix iOS #645) */
    val isAdmin: Boolean
        get() = event?.members?.firstOrNull { it.userId == currentUserId }?.isAdmin == true ||
            (currentUserId != null && event?.createdBy?.id == currentUserId)

    /** Igual que el grupo: solo se bloquea salir si es el único admin con otros miembros. */
    val canLeaveDirectly: Boolean
        get() {
            val members = event?.members ?: return true
            val adminCount = members.count { it.isAdmin }
            return !isAdmin || adminCount > 1 || members.size == 1
        }

    /** El evento ya terminó (no se puede editar). */
    val isPast: Boolean
        get() = event?.let { it.endTime * 1000 < System.currentTimeMillis() } == true
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: Int = savedStateHandle.get<Int>("id") ?: 0

    private val _state = MutableStateFlow(EventDetailUiState(currentUserId = getCurrentUser()?.uid))
    val state: StateFlow<EventDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            communityRepository.getEvent(eventId)
                .onSuccess { e ->
                    // Abrir un evento te auto-une (backend) → refresca "Mis eventos".
                    refreshCoordinator.invalidateCommunity()
                    _state.update { it.copy(isLoading = false, event = e) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = context.getString(R.string.common_unexpected_error_msg)) } }
        }
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                userRepository.getUserProfile(uid).onSuccess { p -> _state.update { it.copy(currentUserCountry = p.countryCode, currentUserPicUrl = p.profilePicUrl) } }
            }
        }
        // Recarga el ranking de ESTE evento tras subir un HIT en su contexto.
        viewModelScope.launch {
            refreshCoordinator.event.collect { id -> if (id == eventId) reloadEvent() }
        }
    }

    private fun reloadEvent() {
        viewModelScope.launch {
            communityRepository.getEvent(eventId).onSuccess { e -> _state.update { it.copy(event = e) } }
        }
    }

    /** Reintento desde la pantalla de error no controlado. */
    fun retry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            communityRepository.getEvent(eventId)
                .onSuccess { e -> refreshCoordinator.invalidateCommunity(); _state.update { it.copy(isLoading = false, event = e) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = context.getString(R.string.common_unexpected_error_msg)) } }
        }
    }

    fun leave() {
        viewModelScope.launch {
            _state.update { it.copy(leaving = true) }
            communityRepository.leaveEvent(eventId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); _state.update { it.copy(leaving = false, left = true) } }
                .onFailure { e -> _state.update { it.copy(leaving = false, error = context.getString(R.string.err_leave)) } }
        }
    }

    fun delete() {
        viewModelScope.launch {
            _state.update { it.copy(leaving = true) }
            communityRepository.deleteEvent(eventId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); _state.update { it.copy(leaving = false, deleted = true) } }
                .onFailure { e -> _state.update { it.copy(leaving = false, error = context.getString(R.string.err_delete)) } }
        }
    }

    /** Denuncia el evento (comentario opcional). Silencioso, igual que el report de HIT. */
    fun report(comment: String) {
        viewModelScope.launch {
            communityRepository.reportEvent(eventId, comment.ifBlank { null })
                .onSuccess { _state.update { it.copy(reportSent = true) } }
                .onFailure { _state.update { it.copy(reportFailed = true) } }
        }
    }

    fun clearReportResult() = _state.update { it.copy(reportSent = false, reportFailed = false) }

    /** Resetea (anula) el hit de un participante (solo admin) y recarga el ranking. */
    fun resetHit(hitId: Int) {
        viewModelScope.launch {
            communityRepository.resetEventHit(eventId, hitId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); reloadEvent() }
                .onFailure { e -> _state.update { it.copy(error = context.getString(R.string.err_generic_action)) } }
        }
    }

    fun makeAdmin(userId: String) {
        viewModelScope.launch {
            communityRepository.makeEventAdmin(eventId, userId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); reloadEvent() }
                .onFailure { e -> _state.update { it.copy(error = context.getString(R.string.err_generic_action)) } }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            communityRepository.removeEventMember(eventId, userId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); reloadEvent() }
                .onFailure { e -> _state.update { it.copy(error = context.getString(R.string.err_generic_action)) } }
        }
    }
}
