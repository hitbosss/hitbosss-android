package com.hitbosss.presentation.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.EventsResult
import com.hitbosss.domain.model.GroupsResult
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetUserEventsUseCase
import com.hitbosss.domain.usecase.GetUserGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CommunityTab { Groups, Events }

data class CommunityUiState(
    val selected: CommunityTab = CommunityTab.Groups,
    val isLoading: Boolean = false,
    val groups: GroupsResult? = null,
    val events: EventsResult? = null,
    val error: String? = null,
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getUserGroups: GetUserGroupsUseCase,
    private val getUserEvents: GetUserEventsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val state: StateFlow<CommunityUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun select(tab: CommunityTab) = _state.update { it.copy(selected = tab) }

    fun load() {
        val uid = getCurrentUser()?.uid
        if (uid.isNullOrBlank()) {
            _state.update { it.copy(error = "No hay sesión activa") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val groups = getUserGroups(uid).getOrNull()
            val events = getUserEvents(uid).getOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    groups = groups,
                    events = events,
                    error = if (groups == null && events == null) "No se pudo cargar la comunidad" else null,
                )
            }
        }
    }
}
