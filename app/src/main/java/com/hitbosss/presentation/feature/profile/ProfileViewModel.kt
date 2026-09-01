package com.hitbosss.presentation.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.SportRanking
import com.hitbosss.domain.model.UserProfile
import com.hitbosss.domain.repository.HitRepository
import com.hitbosss.domain.repository.RankingRepository
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.hitbosss.R
import android.content.Context
import com.hitbosss.domain.model.Participation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlinx.coroutines.flow.asSharedFlow

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val profile: UserProfile? = null,
    val isOtherUser: Boolean = false,
    // Ranking por deporte (apiValue -> ranking) para calcular TOP% y el TOTAL oficial.
    val rankings: Map<String, SportRanking> = emptyMap(),
    val error: String? = null,
    val isProcessingHit: Boolean = false,   // borrando o preparando edición de un HIT
    val actionError: String? = null,        // error de borrar/editar (popup "Error inesperado")
    val reportSent: Boolean = false,        // denuncia de perfil enviada (popup de confirmación)
    val reportFailed: Boolean = false,
)

/** Evento de navegación para editar un HIT ya subido (con el vídeo ya descargado en local). */
data class EditHitNav(
    val exercise: String,
    val weight: Double,
    val hitId: Int,
    val performedAt: Double,
    val localPath: String,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val hitRepository: HitRepository,
    private val rankingRepository: RankingRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val appContext: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // userId presente => perfil ajeno (ruta user_profile/{userId}); null => perfil propio (pestaña).
    private val otherUserId: String? = savedStateHandle.get<String>("userId")

    private val _state = MutableStateFlow(ProfileUiState(isOtherUser = otherUserId != null))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _editEvents = MutableSharedFlow<EditHitNav>(extraBufferCapacity = 1)
    val editEvents: SharedFlow<EditHitNav> = _editEvents.asSharedFlow()

    private var reloading = false

    init {
        load()
        loadRankings()
        // Recarga el perfil propio tras acciones (subir HIT, editar perfil). El ajeno no escucha.
        if (otherUserId == null) {
            viewModelScope.launch { refreshCoordinator.profile.collect { reloadAll(showRefreshing = false) } }
        }
    }

    /** Pull-to-refresh manual. */
    fun refresh() = reloadAll(showRefreshing = true)

    /** Denuncia el perfil visitado (solo perfil ajeno). */
    fun reportUser(comment: String) {
        val target = otherUserId ?: return
        viewModelScope.launch {
            userRepository.reportUser(target, comment.ifBlank { null })
                .onSuccess { _state.update { it.copy(reportSent = true) } }
                .onFailure { _state.update { it.copy(reportFailed = true) } }
        }
    }

    fun clearReportResult() = _state.update { it.copy(reportSent = false, reportFailed = false) }

    // Refresco por antigüedad (al cambiar a la pestaña tras mucho tiempo), silencioso. Solo perfil propio.
    private var lastLoadedAt = 0L
    private val staleMs = 5 * 60 * 1000L
    fun refreshIfStale() {
        if (otherUserId == null && System.currentTimeMillis() - lastLoadedAt > staleMs) reloadAll(showRefreshing = false)
    }

    /** Recarga perfil + rankings sin el spinner de pantalla completa. Guarda de concurrencia. */
    private fun reloadAll(showRefreshing: Boolean) {
        if (reloading) return
        val uid = otherUserId ?: getCurrentUser()?.uid
        if (uid.isNullOrBlank()) return
        reloading = true
        lastLoadedAt = System.currentTimeMillis()
        viewModelScope.launch {
            if (showRefreshing) _state.update { it.copy(isRefreshing = true) }
            val profileD = async { userRepository.getUserProfile(uid) }
            val pl = async { rankingRepository.getRanking("powerlifting").getOrNull() }
            val cf = async { rankingRepository.getRanking("crossfit").getOrNull() }
            profileD.await().onSuccess { p -> _state.update { it.copy(profile = p, error = null) } }
            val map = buildMap {
                pl.await()?.let { put("powerlifting", it) }
                cf.await()?.let { put("crossfit", it) }
            }
            _state.update { it.copy(rankings = if (map.isNotEmpty()) it.rankings + map else it.rankings, isRefreshing = false) }
            reloading = false
        }
    }

    fun load() {
        val uid = otherUserId ?: getCurrentUser()?.uid
        if (uid.isNullOrBlank()) {
            _state.update { it.copy(error = appContext.getString(R.string.err_no_session)) }
            return
        }
        lastLoadedAt = System.currentTimeMillis()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            userRepository.getUserProfile(uid)
                .onSuccess { profile -> _state.update { it.copy(isLoading = false, profile = profile) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = appContext.getString(R.string.common_unexpected_error_msg)) } }
        }
    }

    /** Carga el ranking de ambos deportes para calcular TOP% / TOTAL oficial (igual que ProfileRankingStore de iOS). */
    private fun loadRankings() {
        viewModelScope.launch {
            val pl = async { rankingRepository.getRanking("powerlifting").getOrNull() }
            val cf = async { rankingRepository.getRanking("crossfit").getOrNull() }
            val map = buildMap {
                pl.await()?.let { put("powerlifting", it) }
                cf.await()?.let { put("crossfit", it) }
            }
            if (map.isNotEmpty()) _state.update { it.copy(rankings = it.rankings + map) }
        }
    }

    fun clearActionError() = _state.update { it.copy(actionError = null) }

    /** Elimina un HIT propio (long-press → Eliminar). Recarga el perfil + ranking al terminar. */
    fun deleteHit(hitId: Int) {
        if (_state.value.isProcessingHit) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessingHit = true, actionError = null) }
            hitRepository.deleteHit(hitId)
                .onSuccess {
                    refreshCoordinator.onHitUploaded()  // invalida ranking + perfil
                    reloadAll(showRefreshing = false)
                    _state.update { it.copy(isProcessingHit = false) }
                }
                .onFailure { _state.update { it.copy(isProcessingHit = false, actionError = "delete") } }
        }
    }

    /** Oculta/muestra un HIT propio (long-press → Ocultar). Recarga perfil + ranking al terminar. */
    fun toggleHitVisibility(hitId: Int, hidden: Boolean) {
        if (_state.value.isProcessingHit) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessingHit = true, actionError = null) }
            hitRepository.toggleHitVisibility(hitId, hidden)
                .onSuccess {
                    refreshCoordinator.onHitUploaded()  // invalida ranking + perfil (el hit desaparece/reaparece)
                    reloadAll(showRefreshing = false)
                    _state.update { it.copy(isProcessingHit = false) }
                }
                .onFailure { _state.update { it.copy(isProcessingHit = false, actionError = "hide") } }
        }
    }

    /**
     * Prepara la edición de un HIT propio: descarga el vídeo remoto a local y emite el evento de
     * navegación a EditVideo. (La pantalla de edición trabaja con un fichero local.)
     */
    fun startEditHit(p: Participation) {
        if (_state.value.isProcessingHit) return
        val hitId = p.hitId ?: return
        val url = p.videoUrl?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            _state.update { it.copy(isProcessingHit = true, actionError = null) }
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val dest = File(appContext.cacheDir, "edit_hit_$hitId.mp4")
                    URL(url).openStream().use { input -> dest.outputStream().use { out -> input.copyTo(out) } }
                    dest
                }.getOrNull()
            }
            _state.update { it.copy(isProcessingHit = false) }
            if (file != null) {
                _editEvents.tryEmit(
                    EditHitNav(
                        exercise = p.exercise,
                        weight = p.maxLift?.value ?: 0.0,
                        hitId = hitId,
                        performedAt = p.performedAt,
                        localPath = file.absolutePath,
                    ),
                )
            } else {
                _state.update { it.copy(actionError = "edit") }
            }
        }
    }
}
