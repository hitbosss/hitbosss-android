package com.hitbosss.presentation.feature.community

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.Sport
import com.hitbosss.domain.usecase.CreateEventParams
import com.hitbosss.domain.usecase.CreateEventUseCase
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

data class CreateEventUiState(
    val name: String = "",
    val description: String = "",
    val coverUri: Uri? = null,
    val exercises: Set<Exercise> = emptySet(),
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && exercises.isNotEmpty() &&
            startMillis != null && endMillis != null && endMillis!! > startMillis!!
}

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val createEvent: CreateEventUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
) : ViewModel() {

    val maxName = 25
    val maxDescription = 300

    private val _state = MutableStateFlow(CreateEventUiState())
    val state: StateFlow<CreateEventUiState> = _state.asStateFlow()

    fun onName(v: String) { if (v.length <= maxName) _state.update { it.copy(name = v) } }
    fun onDescription(v: String) { if (v.length <= maxDescription) _state.update { it.copy(description = v) } }
    fun onCoverPicked(uri: Uri) = _state.update { it.copy(coverUri = uri) }
    fun onStart(millis: Long?) = _state.update { it.copy(startMillis = millis) }
    fun onEnd(millis: Long?) = _state.update { it.copy(endMillis = millis) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun toggleExercise(e: Exercise) = _state.update {
        it.copy(exercises = if (e in it.exercises) it.exercises - e else it.exercises + e)
    }

    fun submit() {
        val s = _state.value
        if (!s.isValid) return
        // El deporte del evento lo determinan los ejercicios elegidos.
        val sport = s.exercises.firstOrNull()?.sport ?: Sport.Powerlifting
        val hasOfficial = officialSportsFor(s.exercises).contains(sport.apiValue)
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val cover = s.coverUri?.let { uriToFile(it) }
            createEvent(
                CreateEventParams(
                    name = s.name.trim(),
                    description = s.description.trim(),
                    isPublic = true,
                    sport = sport.apiValue,
                    exercises = s.exercises.map { it.apiValue },
                    hasOfficial = hasOfficial,
                    startTime = s.startMillis!! / 1000,
                    endTime = s.endMillis!! / 1000,
                    coverPic = cover,
                ),
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
