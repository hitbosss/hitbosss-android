package com.hitbosss.presentation.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.SportRanking
import com.hitbosss.domain.model.UserProfile
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetRankingUseCase
import com.hitbosss.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val isOtherUser: Boolean = false,
    // Ranking por deporte (apiValue -> ranking) para calcular TOP% y el TOTAL oficial.
    val rankings: Map<String, SportRanking> = emptyMap(),
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val getRanking: GetRankingUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // userId presente => perfil ajeno (ruta user_profile/{userId}); null => perfil propio (pestaña).
    private val otherUserId: String? = savedStateHandle.get<String>("userId")

    private val _state = MutableStateFlow(ProfileUiState(isOtherUser = otherUserId != null))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
        loadRankings()
    }

    fun load() {
        val uid = otherUserId ?: getCurrentUser()?.uid
        if (uid.isNullOrBlank()) {
            _state.update { it.copy(error = "No hay sesión activa") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getUserProfile(uid)
                .onSuccess { profile -> _state.update { it.copy(isLoading = false, profile = profile) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Error") } }
        }
    }

    /** Carga el ranking de ambos deportes para calcular TOP% / TOTAL oficial (igual que ProfileRankingStore de iOS). */
    private fun loadRankings() {
        viewModelScope.launch {
            val pl = async { getRanking("powerlifting").getOrNull() }
            val cf = async { getRanking("crossfit").getOrNull() }
            val map = buildMap {
                pl.await()?.let { put("powerlifting", it) }
                cf.await()?.let { put("crossfit", it) }
            }
            if (map.isNotEmpty()) _state.update { it.copy(rankings = it.rankings + map) }
        }
    }
}
