package com.hitbosss.presentation.feature.community

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.usecase.CreateGroupParams
import com.hitbosss.domain.usecase.GetGroupUseCase
import com.hitbosss.domain.usecase.UpdateGroupUseCase
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

data class EditGroupUiState(
    val name: String = "",
    val motto: String = "",
    val description: String = "",
    val coverUri: Uri? = null,
    val existingCoverUrl: String? = null,
    val isPublic: Boolean = true,
    val originalIsPublic: Boolean = true,
    val exercises: Set<Exercise> = emptySet(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank() && exercises.isNotEmpty()
}

@HiltViewModel
class EditGroupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getGroup: GetGroupUseCase,
    private val updateGroup: UpdateGroupUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: Int = savedStateHandle.get<Int>("id") ?: 0

    val maxName = 25
    val maxMotto = 120
    val maxDescription = 300

    private val _state = MutableStateFlow(EditGroupUiState())
    val state: StateFlow<EditGroupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getGroup(groupId).onSuccess { g ->
                _state.update {
                    it.copy(
                        name = g.name,
                        motto = g.motto.orEmpty(),
                        description = g.description.orEmpty(),
                        existingCoverUrl = g.coverImageUrl,
                        isPublic = g.isPublic,
                        originalIsPublic = g.isPublic,
                        exercises = g.exercises.mapNotNull { api -> Exercise.entries.firstOrNull { e -> e.apiValue == api } }.toSet(),
                        loaded = true,
                    )
                }
            }
        }
    }

    fun onName(v: String) { if (v.length <= maxName) _state.update { it.copy(name = v) } }
    fun onMotto(v: String) { if (v.length <= maxMotto) _state.update { it.copy(motto = v) } }
    fun onDescription(v: String) { if (v.length <= maxDescription) _state.update { it.copy(description = v) } }
    fun onCoverPicked(uri: Uri) = _state.update { it.copy(coverUri = uri) }
    fun onVisibility(public: Boolean) = _state.update { it.copy(isPublic = public) }
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
            updateGroup(
                groupId,
                CreateGroupParams(
                    name = s.name.trim(),
                    motto = s.motto.trim(),
                    description = s.description.trim(),
                    isPublic = s.isPublic,
                    exercises = s.exercises.map { it.apiValue },
                    officialSports = officialSportsFor(s.exercises),
                    coverPic = cover,
                ),
            ).onSuccess { refreshCoordinator.invalidateCommunity(); _state.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = context.getString(R.string.err_create_group)) } }
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
