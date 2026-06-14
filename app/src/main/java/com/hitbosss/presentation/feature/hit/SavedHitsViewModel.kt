package com.hitbosss.presentation.feature.hit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.data.local.SavedHitStore
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.SavedHit
import com.hitbosss.domain.usecase.UploadHitParams
import com.hitbosss.domain.usecase.UploadHitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow

data class SavedHitsUiState(
    val hits: List<SavedHit> = emptyList(),
    val selectedId: String? = null,
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val success: Boolean = false,
    val error: Boolean = false,
    val deleteTarget: SavedHit? = null,
)

/** Pantalla "HITS guardados": lista, borra y reintenta la subida de los HITs guardados localmente. */
@HiltViewModel
class SavedHitsViewModel @Inject constructor(
    private val store: SavedHitStore,
    private val uploadHit: UploadHitUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
) : ViewModel() {

    private var uploadJob: Job? = null
    private val _state = MutableStateFlow(SavedHitsUiState())
    val state: StateFlow<SavedHitsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val hits = store.getAll().sortedByDescending { it.createdAt }
        _state.update { it.copy(hits = hits, selectedId = it.selectedId?.takeIf { id -> hits.any { h -> h.id == id } }) }
    }

    fun select(id: String) = _state.update { it.copy(selectedId = if (it.selectedId == id) null else id) }

    fun askDelete(hit: SavedHit) = _state.update { it.copy(deleteTarget = hit) }
    fun dismissDelete() = _state.update { it.copy(deleteTarget = null) }
    fun confirmDelete() {
        _state.value.deleteTarget?.let { store.delete(it.id) }
        _state.update { it.copy(deleteTarget = null) }
        load()
    }

    fun uploadSelected() {
        if (_state.value.isUploading) return
        val hit = _state.value.hits.firstOrNull { it.id == _state.value.selectedId } ?: return
        uploadJob = viewModelScope.launch {
            _state.update { it.copy(isUploading = true, progress = 0f, error = false) }
            var lastPct = -1
            uploadHit(
                UploadHitParams(
                    userId = hit.userId,
                    sport = hit.sport,
                    exercise = hit.exercise,
                    lift = hit.lift,
                    unit = hit.unit,
                    bodyWeightKg = hit.bodyWeightKg,
                    gender = hit.gender,
                    videoFile = store.videoFile(hit),
                    performedAt = hit.performedAt,
                    contextType = hit.contextType,
                    groupId = hit.groupId,
                    eventId = hit.eventId,
                    onProgress = { p ->
                        val pct = (p * 100).toInt()
                        if (pct != lastPct) { lastPct = pct; _state.update { it.copy(progress = p) } }
                    },
                ),
            ).onSuccess {
                store.delete(hit.id)
                // Recarga lo afectado según el contexto del HIT.
                when (hit.contextType) {
                    "group" -> { hit.groupId?.let { refreshCoordinator.invalidateGroup(it) }; refreshCoordinator.invalidateProfile() }
                    "event" -> { hit.eventId?.let { refreshCoordinator.invalidateEvent(it) }; refreshCoordinator.invalidateProfile() }
                    else -> refreshCoordinator.onHitUploaded()
                }
                _state.update { it.copy(isUploading = false, success = true, selectedId = null) }
                load()
            }.onFailure {
                _state.update { it.copy(isUploading = false, error = true) }
            }
        }
    }

    fun cancelUpload() {
        uploadJob?.cancel()
        _state.update { it.copy(isUploading = false, progress = 0f) }
    }

    fun dismissSuccess() = _state.update { it.copy(success = false) }
    fun dismissError() = _state.update { it.copy(error = false) }

    fun exerciseTitle(apiValue: String): String =
        Exercise.entries.firstOrNull { it.apiValue.equals(apiValue, true) }?.title ?: apiValue
}
