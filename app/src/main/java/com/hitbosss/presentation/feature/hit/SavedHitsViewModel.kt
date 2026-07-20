package com.hitbosss.presentation.feature.hit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.core.upload.HitUploadManager
import com.hitbosss.data.local.SavedHitStore
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.SavedHit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SavedHitsUiState(
    val hits: List<SavedHit> = emptyList(),
    val selectedFilter: SavedHitsFilter = SavedHitsFilter.Ranking,
    val selectedId: String? = null,
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val success: Boolean = false,
    val error: Boolean = false,
    val deleteTarget: SavedHit? = null,
) {
    /** HITs del contexto seleccionado (1:1 con filteredHits de iOS). */
    val filteredHits: List<SavedHit> get() = hits.filter { it.contextType == selectedFilter.contextType }
}

/** Filtro por contexto de los HITs guardados (1:1 con RankingTypeFilter de iOS). */
enum class SavedHitsFilter(@androidx.annotation.StringRes val label: Int, val contextType: String) {
    Ranking(com.hitbosss.R.string.tab_ranking, "global"),
    Grupos(com.hitbosss.R.string.community_tab_groups, "group"),
    Eventos(com.hitbosss.R.string.community_tab_events, "event"),
}

/**
 * Pantalla "HITS guardados": lista, borra y reintenta la subida de los HITs guardados localmente.
 * iOS #649: la subida corre en HitUploadManager (sigue aunque salgas de la pantalla) y aquí
 * solo se observa su estado.
 */
@HiltViewModel
class SavedHitsViewModel @Inject constructor(
    private val store: SavedHitStore,
    private val uploadManager: HitUploadManager,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedHitsUiState())
    val state: StateFlow<SavedHitsUiState> = _state.asStateFlow()

    init {
        load()
        // Refleja el estado global de subida; al terminar (borra el hit subido) recarga la lista.
        viewModelScope.launch {
            var wasUploading = false
            uploadManager.state.collect { st ->
                _state.update { it.copy(isUploading = st.inProgress, progress = st.progress) }
                if (wasUploading && !st.inProgress) {
                    load()
                    _state.update { it.copy(selectedId = null) }
                }
                wasUploading = st.inProgress
            }
        }
    }

    fun load() {
        val hits = store.getAll().sortedByDescending { it.createdAt }
        _state.update { it.copy(hits = hits, selectedId = it.selectedId?.takeIf { id -> hits.any { h -> h.id == id } }) }
    }

    fun select(id: String) = _state.update { it.copy(selectedId = if (it.selectedId == id) null else id) }

    /** Cambia el filtro de contexto y deselecciona (igual que iOS). */
    fun setFilter(filter: SavedHitsFilter) = _state.update { it.copy(selectedFilter = filter, selectedId = null) }

    /** URI del vídeo local guardado (para "Ver HIT"), o null si no existe el fichero. */
    fun videoUri(hit: SavedHit): String? =
        store.videoFile(hit).takeIf { it.exists() }?.let { android.net.Uri.fromFile(it).toString() }

    fun showError() = _state.update { it.copy(error = true) }

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
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.setStatus(hit.id, SavedHit.STATUS_UPLOADING) }
            if (!uploadManager.start(hit.copy(status = SavedHit.STATUS_UPLOADING))) {
                withContext(Dispatchers.IO) { store.setStatus(hit.id, SavedHit.STATUS_PENDING) }
                _state.update { it.copy(error = true) }
            }
        }
    }

    fun cancelUpload() = uploadManager.cancel()

    fun dismissSuccess() = _state.update { it.copy(success = false) }
    fun dismissError() = _state.update { it.copy(error = false) }

    fun exerciseTitle(apiValue: String): String =
        Exercise.entries.firstOrNull { it.apiValue.equals(apiValue, true) }?.title ?: apiValue
}
