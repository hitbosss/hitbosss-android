package com.hitbosss.presentation.feature.community

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.usecase.GetEventUseCase
import com.hitbosss.domain.usecase.UpdateEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import com.hitbosss.R

data class EditEventUiState(
    val name: String = "",
    val description: String = "",
    val coverUri: Uri? = null,
    val existingCoverUrl: String? = null,
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val started: Boolean = false,
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && startMillis != null && endMillis != null && endMillis!! > startMillis!!
}

/**
 * El backend de PATCH evento solo actualiza nombre/descripción/fechas/portada (no ejercicios),
 * por eso esta pantalla no incluye el selector de ejercicios.
 */
@HiltViewModel
class EditEventViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getEvent: GetEventUseCase,
    private val updateEvent: UpdateEventUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val eventId: Int = savedStateHandle.get<Int>("id") ?: 0

    val maxName = 25
    val maxDescription = 300

    private val _state = MutableStateFlow(EditEventUiState())
    val state: StateFlow<EditEventUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getEvent(eventId).onSuccess { e ->
                _state.update {
                    it.copy(
                        name = e.name,
                        description = e.description.orEmpty(),
                        existingCoverUrl = e.coverImageUrl,
                        startMillis = e.startTime * 1000,
                        endMillis = e.endTime * 1000,
                        started = e.startTime * 1000 < System.currentTimeMillis(),
                        loaded = true,
                    )
                }
            }
        }
    }

    fun onName(v: String) { if (v.length <= maxName) _state.update { it.copy(name = v) } }
    fun onDescription(v: String) { if (v.length <= maxDescription) _state.update { it.copy(description = v) } }
    fun onCoverPicked(uri: Uri) = _state.update { it.copy(coverUri = uri) }
    fun onStart(millis: Long?) = _state.update { it.copy(startMillis = millis) }
    fun onEnd(millis: Long?) = _state.update { it.copy(endMillis = millis) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun submit() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val cover = s.coverUri?.let { uriToFile(it) }
            updateEvent(
                eventId = eventId,
                name = s.name.trim(),
                description = s.description.trim(),
                startTime = s.startMillis!! / 1000,
                endTime = s.endMillis!! / 1000,
                coverPic = cover,
            ).onSuccess { refreshCoordinator.invalidateCommunity(); _state.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: context.getString(R.string.err_create_event)) } }
        }
    }

    private suspend fun uriToFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val f = File(context.cacheDir, "event_cover_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            f
        }.getOrNull()
    }
}
