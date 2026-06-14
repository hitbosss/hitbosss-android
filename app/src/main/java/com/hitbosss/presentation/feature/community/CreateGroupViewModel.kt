package com.hitbosss.presentation.feature.community

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.usecase.CreateGroupParams
import com.hitbosss.domain.usecase.CreateGroupUseCase
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

data class CreateGroupUiState(
    val name: String = "",
    val motto: String = "",
    val description: String = "",
    val coverUri: Uri? = null,
    val exercises: Set<Exercise> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank() && exercises.isNotEmpty()
}

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val createGroup: CreateGroupUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
) : ViewModel() {

    val maxName = 25
    val maxMotto = 120
    val maxDescription = 300

    private val _state = MutableStateFlow(CreateGroupUiState())
    val state: StateFlow<CreateGroupUiState> = _state.asStateFlow()

    fun onName(v: String) { if (v.length <= maxName) _state.update { it.copy(name = v) } }
    fun onMotto(v: String) { if (v.length <= maxMotto) _state.update { it.copy(motto = v) } }
    fun onDescription(v: String) { if (v.length <= maxDescription) _state.update { it.copy(description = v) } }
    fun onCoverPicked(uri: Uri) = _state.update { it.copy(coverUri = uri) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun toggleExercise(e: Exercise) = _state.update {
        it.copy(exercises = if (e in it.exercises) it.exercises - e else it.exercises + e)
    }

    fun submit() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val cover = s.coverUri?.let { uriToFile(it) }
            createGroup(
                CreateGroupParams(
                    name = s.name.trim(),
                    motto = s.motto.trim(),
                    description = s.description.trim(),
                    isPublic = true,
                    exercises = s.exercises.map { it.apiValue },
                    officialSports = officialSportsFor(s.exercises),
                    coverPic = cover,
                ),
            ).onSuccess { refreshCoordinator.invalidateCommunity(); _state.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: context.getString(R.string.err_create_group)) } }
        }
    }

    private suspend fun uriToFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val f = File(context.cacheDir, "group_cover_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            f
        }.getOrNull()
    }
}

/** Deportes con el set oficial completo entre los ejercicios elegidos (misma regla que iOS). */
internal fun officialSportsFor(exercises: Set<Exercise>): List<String> {
    val result = mutableListOf<String>()
    val pl = exercises.contains(Exercise.Squat) && exercises.contains(Exercise.BenchPress) &&
        (exercises.contains(Exercise.Deadlift) || exercises.contains(Exercise.SumoDeadlift))
    if (pl) result += "powerlifting"
    val cf = exercises.contains(Exercise.Snatch) && exercises.contains(Exercise.Clean) &&
        exercises.contains(Exercise.CleanAndJerk)
    if (cf) result += "crossfit"
    return result
}
