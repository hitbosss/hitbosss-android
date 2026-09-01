package com.hitbosss.presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.Participation
import com.hitbosss.domain.repository.HitRepository
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HiddenHitsUiState(
    val isLoading: Boolean = false,
    val hits: List<Participation> = emptyList(),
    val isProcessing: Boolean = false,   // restaurando o borrando
    val error: String? = null,
)

@HiltViewModel
class HiddenHitsViewModel @Inject constructor(
    private val hitRepository: HitRepository,
    private val userRepository: UserRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(HiddenHitsUiState())
    val state: StateFlow<HiddenHitsUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val uid = getCurrentUser()?.uid
            val unit = uid?.let { userRepository.getPersonalInfo(it).getOrNull()?.measurementSystem } ?: "metric"
            hitRepository.getHiddenHits(unit)
                .onSuccess { hits -> _state.update { it.copy(isLoading = false, hits = hits) } }
                .onFailure { _state.update { it.copy(isLoading = false, error = "load") } }
        }
    }

    /** Restaura un HIT oculto (lo vuelve a mostrar en el perfil). */
    fun restore(hitId: Int) = act { hitRepository.toggleHitVisibility(hitId, false) }

    /** Elimina permanentemente un HIT oculto. */
    fun delete(hitId: Int) = act { hitRepository.deleteHit(hitId) }

    private fun act(block: suspend () -> Result<Unit>) {
        if (_state.value.isProcessing) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            block()
                .onSuccess {
                    refreshCoordinator.onHitUploaded()  // invalida perfil + ranking
                    _state.update { it.copy(isProcessing = false) }
                    load()
                }
                .onFailure { _state.update { it.copy(isProcessing = false, error = "action") } }
        }
    }
}
