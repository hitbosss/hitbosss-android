package com.hitbosss.presentation.feature.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.GroupDetail
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
import androidx.compose.foundation.layout.size

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val group: GroupDetail? = null,
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
    /** ¿El usuario actual es admin del grupo? (muestra "Eliminar grupo" y estilo del botón salir). */
    // El creador siempre cuenta como admin aunque no figure como tal en members (fix iOS #645).
    val isAdmin: Boolean
        get() = group?.members?.firstOrNull { it.userId == currentUserId }?.isAdmin == true ||
            (currentUserId != null && group?.createdBy?.id == currentUserId)

    /**
     * Igual que iOS canLeaveGroupDirectly(): puede salir directo si no es admin, o hay más de un
     * admin, o es el único miembro. Solo se bloquea si es el único admin y quedan otros miembros.
     */
    val canLeaveDirectly: Boolean
        get() {
            val members = group?.members ?: return true
            val adminCount = members.count { it.isAdmin }
            return !isAdmin || adminCount > 1 || members.size == 1
        }
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: Int = savedStateHandle.get<Int>("id") ?: 0

    private val _state = MutableStateFlow(GroupDetailUiState(currentUserId = getCurrentUser()?.uid))
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            communityRepository.getGroup(groupId)
                .onSuccess { g ->
                    // Abrir un grupo te auto-une (backend) → refresca "Mis grupos".
                    refreshCoordinator.invalidateCommunity()
                    _state.update { it.copy(isLoading = false, group = g) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = context.getString(R.string.common_unexpected_error_msg)) } }
        }
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                userRepository.getUserProfile(uid).onSuccess { p -> _state.update { it.copy(currentUserCountry = p.countryCode, currentUserPicUrl = p.profilePicUrl) } }
            }
        }
        // Recarga el ranking de ESTE grupo tras subir un HIT en su contexto.
        viewModelScope.launch {
            refreshCoordinator.group.collect { id -> if (id == groupId) reloadGroup() }
        }
    }

    private fun reloadGroup() {
        viewModelScope.launch {
            communityRepository.getGroup(groupId).onSuccess { g -> _state.update { it.copy(group = g) } }
        }
    }

    /** Reintento desde la pantalla de error no controlado. */
    fun retry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            communityRepository.getGroup(groupId)
                .onSuccess { g -> refreshCoordinator.invalidateCommunity(); _state.update { it.copy(isLoading = false, group = g) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = context.getString(R.string.common_unexpected_error_msg)) } }
        }
    }

    fun leave() {
        viewModelScope.launch {
            _state.update { it.copy(leaving = true) }
            communityRepository.leaveGroup(groupId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); _state.update { it.copy(leaving = false, left = true) } }
                .onFailure { e -> _state.update { it.copy(leaving = false, error = context.getString(R.string.err_leave)) } }
        }
    }

    fun delete() {
        viewModelScope.launch {
            _state.update { it.copy(leaving = true) }
            communityRepository.deleteGroup(groupId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); _state.update { it.copy(leaving = false, deleted = true) } }
                .onFailure { e -> _state.update { it.copy(leaving = false, error = context.getString(R.string.err_delete)) } }
        }
    }

    /** Denuncia el grupo (comentario opcional). Silencioso, igual que el report de HIT. */
    fun report(comment: String) {
        viewModelScope.launch {
            communityRepository.reportGroup(groupId, comment.ifBlank { null })
                .onSuccess { _state.update { it.copy(reportSent = true) } }
                .onFailure { _state.update { it.copy(reportFailed = true) } }
        }
    }

    fun clearReportResult() = _state.update { it.copy(reportSent = false, reportFailed = false) }

    /** Da admin a un miembro (solo admin) y recarga la lista. */
    fun makeAdmin(userId: String) {
        viewModelScope.launch {
            communityRepository.makeGroupAdmin(groupId, userId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); reloadGroup() }
                .onFailure { e -> _state.update { it.copy(error = context.getString(R.string.err_generic_action)) } }
        }
    }

    /** Expulsa a un miembro (solo admin) y recarga la lista. */
    fun removeMember(userId: String) {
        viewModelScope.launch {
            communityRepository.removeGroupMember(groupId, userId)
                .onSuccess { refreshCoordinator.invalidateCommunity(); reloadGroup() }
                .onFailure { e -> _state.update { it.copy(error = context.getString(R.string.err_generic_action)) } }
        }
    }
}
