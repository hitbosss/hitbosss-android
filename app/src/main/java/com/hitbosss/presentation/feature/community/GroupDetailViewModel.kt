package com.hitbosss.presentation.feature.community

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.GroupDetail
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.DeleteGroupUseCase
import com.hitbosss.domain.usecase.GetGroupUseCase
import com.hitbosss.domain.usecase.GetUserProfileUseCase
import com.hitbosss.domain.usecase.LeaveGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
) {
    /** ¿El usuario actual es admin del grupo? (muestra "Eliminar grupo" y estilo del botón salir). */
    val isAdmin: Boolean
        get() = group?.members?.firstOrNull { it.userId == currentUserId }?.isAdmin == true

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
    private val getGroup: GetGroupUseCase,
    private val leaveGroup: LeaveGroupUseCase,
    private val deleteGroup: DeleteGroupUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: Int = savedStateHandle.get<Int>("id") ?: 0

    private val _state = MutableStateFlow(GroupDetailUiState(currentUserId = getCurrentUser()?.uid))
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getGroup(groupId)
                .onSuccess { g -> _state.update { it.copy(isLoading = false, group = g) } }
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
            leaveGroup(groupId)
                .onSuccess { _state.update { it.copy(leaving = false, left = true) } }
                .onFailure { e -> _state.update { it.copy(leaving = false, error = e.message ?: "No se pudo salir") } }
        }
    }

    fun delete() {
        viewModelScope.launch {
            _state.update { it.copy(leaving = true) }
            deleteGroup(groupId)
                .onSuccess { _state.update { it.copy(leaving = false, deleted = true) } }
                .onFailure { e -> _state.update { it.copy(leaving = false, error = e.message ?: "No se pudo eliminar") } }
        }
    }
}
