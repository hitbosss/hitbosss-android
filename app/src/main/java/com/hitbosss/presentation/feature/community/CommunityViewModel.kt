package com.hitbosss.presentation.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.EventsResult
import com.hitbosss.domain.model.GroupsResult
import com.hitbosss.domain.repository.CommunityRepository
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

enum class CommunityTab { Groups, Events }

data class CommunityUiState(
    val selected: CommunityTab = CommunityTab.Groups,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val groups: GroupsResult? = null,
    val events: EventsResult? = null,
    val error: String? = null,
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    @ApplicationContext private val context: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val state: StateFlow<CommunityUiState> = _state.asStateFlow()

    private var reloading = false

    init {
        load()
        // Recarga tras crear/entrar/salir de grupos o eventos.
        viewModelScope.launch { refreshCoordinator.community.collect { reload(showRefreshing = false) } }
    }

    fun select(tab: CommunityTab) = _state.update { it.copy(selected = tab) }

    /** Pull-to-refresh manual. */
    fun refresh() = reload(showRefreshing = true)

    fun load() = reload(showRefreshing = false, firstLoad = true)

    // Refresco por antigüedad (al cambiar a esta pestaña tras mucho tiempo), silencioso.
    private var lastLoadedAt = 0L
    private val staleMs = 5 * 60 * 1000L
    fun refreshIfStale() {
        if (System.currentTimeMillis() - lastLoadedAt > staleMs) reload(showRefreshing = false)
    }

    private fun reload(showRefreshing: Boolean, firstLoad: Boolean = false) {
        if (reloading) return
        val uid = getCurrentUser()?.uid
        if (uid.isNullOrBlank()) {
            _state.update { it.copy(error = context.getString(R.string.err_no_session)) }
            return
        }
        reloading = true
        lastLoadedAt = System.currentTimeMillis()
        viewModelScope.launch {
            if (firstLoad) _state.update { it.copy(isLoading = true, error = null) }
            if (showRefreshing) _state.update { it.copy(isRefreshing = true) }
            val groups = communityRepository.getUserGroups(uid).getOrNull()
            val events = communityRepository.getUserEvents(uid).getOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    groups = groups ?: it.groups,
                    events = events ?: it.events,
                    error = if (groups == null && events == null && it.groups == null && it.events == null) context.getString(R.string.err_load_community) else null,
                )
            }
            reloading = false
        }
    }
}
