package com.hitbosss.presentation.feature.community

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.Member
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetEventUseCase
import com.hitbosss.domain.usecase.GetGroupUseCase
import com.hitbosss.domain.usecase.MakeEventAdminUseCase
import com.hitbosss.domain.usecase.MakeGroupAdminUseCase
import com.hitbosss.domain.usecase.RemoveEventMemberUseCase
import com.hitbosss.domain.usecase.RemoveGroupMemberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.hitbosss.R

data class CommunityMembersUiState(
    val isLoading: Boolean = true,
    val members: List<Member> = emptyList(),
    val currentUserId: String? = null,
    val creatorId: String? = null,
    val error: String? = null,
) {
    val isCurrentUserAdmin: Boolean
        get() = members.firstOrNull { it.userId == currentUserId }?.isAdmin == true
}

/** Lista de miembros de un grupo o participantes de un evento (1:1 con CommunityMembersView de iOS). */
@HiltViewModel
class CommunityMembersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getGroup: GetGroupUseCase,
    private val getEvent: GetEventUseCase,
    private val makeGroupAdmin: MakeGroupAdminUseCase,
    private val removeGroupMember: RemoveGroupMemberUseCase,
    private val makeEventAdmin: MakeEventAdminUseCase,
    private val removeEventMember: RemoveEventMemberUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val type: String = savedStateHandle.get<String>("type") ?: "group"
    private val id: Int = savedStateHandle.get<Int>("id") ?: 0
    private val isGroup: Boolean get() = type == "group"

    private val _state = MutableStateFlow(CommunityMembersUiState(currentUserId = getCurrentUser()?.uid))
    val state: StateFlow<CommunityMembersUiState> = _state.asStateFlow()

    init { reload() }

    private fun reload() {
        viewModelScope.launch {
            if (isGroup) getGroup(id).onSuccess { g -> _state.update { it.copy(isLoading = false, members = g.members, creatorId = g.createdBy?.id) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = context.getString(R.string.common_unexpected_error_msg)) } }
            else getEvent(id).onSuccess { e -> _state.update { it.copy(isLoading = false, members = e.members, creatorId = e.createdBy?.id) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = context.getString(R.string.common_unexpected_error_msg)) } }
        }
    }

    fun makeAdmin(userId: String) {
        viewModelScope.launch {
            val result = if (isGroup) makeGroupAdmin(id, userId) else makeEventAdmin(id, userId)
            result.onSuccess { refreshCoordinator.invalidateCommunity(); reload() }
                .onFailure { e -> _state.update { it.copy(error = context.getString(R.string.err_generic_action)) } }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            val result = if (isGroup) removeGroupMember(id, userId) else removeEventMember(id, userId)
            result.onSuccess { refreshCoordinator.invalidateCommunity(); reload() }
                .onFailure { e -> _state.update { it.copy(error = context.getString(R.string.err_generic_action)) } }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
