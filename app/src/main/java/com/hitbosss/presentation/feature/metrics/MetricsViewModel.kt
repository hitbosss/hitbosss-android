package com.hitbosss.presentation.feature.metrics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.R
import com.hitbosss.core.RefreshCoordinator
import com.hitbosss.domain.model.BodyLog
import com.hitbosss.domain.model.MetricGoals
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.usecase.AddBodyLogUseCase
import com.hitbosss.domain.usecase.AddMetricGoalUseCase
import com.hitbosss.domain.usecase.DeleteMetricGoalUseCase
import com.hitbosss.domain.usecase.GetBodyLogsUseCase
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.AddProgressPhotoUseCase
import com.hitbosss.domain.usecase.DeleteProgressPhotoUseCase
import com.hitbosss.domain.usecase.GetMetricGoalsUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import com.hitbosss.domain.usecase.GetProgressPhotosUseCase
import com.hitbosss.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetricsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val personalInfo: PersonalInfo? = null,
    val bodyLogs: List<BodyLog> = emptyList(),
    val weightGoals: MetricGoals = MetricGoals(null, emptyList()),
    val photos: List<ProgressPhoto> = emptyList(),
    val hasError: Boolean = false,
    val actionError: String? = null,   // error de añadir/borrar (popup)
    val isSaving: Boolean = false,
) {
    /** "metric" | "imperial" según preferencia del usuario. */
    val unitSystem: String get() = personalInfo?.measurementSystem ?: "metric"

    /** Etiqueta de la unidad de peso del usuario (kg/lbs). */
    val weightUnit: String get() = if (unitSystem == "imperial") "lbs" else "kg"
}

/** ViewModel de la sub-pestaña FÍSICO (medidas, objetivo de peso, evolución). */
@HiltViewModel
class MetricsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getPersonalInfo: GetPersonalInfoUseCase,
    private val getBodyLogs: GetBodyLogsUseCase,
    private val addBodyLog: AddBodyLogUseCase,
    private val getGoals: GetMetricGoalsUseCase,
    private val addGoal: AddMetricGoalUseCase,
    private val deleteGoal: DeleteMetricGoalUseCase,
    private val getPhotos: GetProgressPhotosUseCase,
    private val addPhoto: AddProgressPhotoUseCase,
    private val deletePhoto: DeleteProgressPhotoUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val refreshCoordinator: RefreshCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(MetricsUiState())
    val state: StateFlow<MetricsUiState> = _state.asStateFlow()

    private var reloading = false
    private var lastLoadedAt = 0L
    private val staleMs = 5 * 60 * 1000L

    init {
        reload(initial = true)
        viewModelScope.launch { refreshCoordinator.metrics.collect { reload() } }
        // El peso/altura también cambian al editar el perfil.
        viewModelScope.launch { refreshCoordinator.profile.collect { reload() } }
    }

    fun refresh() = reload(showRefreshing = true)

    fun refreshIfStale() {
        if (System.currentTimeMillis() - lastLoadedAt > staleMs) reload()
    }

    private fun reload(initial: Boolean = false, showRefreshing: Boolean = false) {
        if (reloading) return
        val uid = getCurrentUser()?.uid ?: return
        reloading = true
        _state.update { it.copy(isLoading = initial, isRefreshing = showRefreshing, hasError = false) }
        viewModelScope.launch {
            val infoD = async { getPersonalInfo(uid) }
            val logsD = async { getBodyLogs(uid) }
            val goalsD = async { getGoals(uid, "weight") }
            val photosD = async { getPhotos(uid) }
            val info = infoD.await()
            val logs = logsD.await()
            val goals = goalsD.await()
            val photos = photosD.await()
            lastLoadedAt = System.currentTimeMillis()
            reloading = false
            if (info.isFailure && logs.isFailure) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, hasError = true) }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    personalInfo = info.getOrNull() ?: it.personalInfo,
                    bodyLogs = logs.getOrNull() ?: it.bodyLogs,
                    weightGoals = goals.getOrNull() ?: it.weightGoals,
                    photos = photos.getOrNull() ?: it.photos,
                )
            }
        }
    }

    /** Añadir registro corporal (peso obligatorio; grasa/músculo opcionales, en unidad del usuario). */
    fun onAddBodyLog(weight: Double, bodyFatPct: Double?, muscleMass: Double?) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            addBodyLog(uid, weight, bodyFatPct, muscleMass, _state.value.unitSystem)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    refreshCoordinator.invalidateMetrics()
                    refreshCoordinator.invalidateProfile() // el peso actual del perfil cambia
                }
                .onFailure {
                    _state.update { it.copy(isSaving = false, actionError = appContext.getString(R.string.err_generic_action)) }
                }
        }
    }

    /** Nuevo objetivo de peso (archiva el activo en el backend). */
    fun onAddWeightGoal(target: Double) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            addGoal(uid, "weight", null, target, _state.value.unitSystem)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    refreshCoordinator.invalidateMetrics()
                }
                .onFailure {
                    _state.update { it.copy(isSaving = false, actionError = appContext.getString(R.string.err_generic_action)) }
                }
        }
    }

    /**
     * Lápiz de "Medidas corporales": la altura va al perfil (PATCH personal-info) y el peso
     * crea un registro corporal (histórico + sincroniza el perfil en el backend).
     */
    fun onEditMeasures(height: Double?, weight: Double?) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val heightResult = if (height != null && height > 0) {
                updateProfile(uid, mapOf("height" to height.toInt().toString(), "unit" to _state.value.unitSystem), null, null)
            } else Result.success(Unit)
            val weightResult = if (weight != null && weight > 0) {
                addBodyLog(uid, weight, null, null, _state.value.unitSystem).map { }
            } else Result.success(Unit)
            _state.update { it.copy(isSaving = false) }
            if (heightResult.isFailure || weightResult.isFailure) {
                _state.update { it.copy(actionError = appContext.getString(R.string.err_generic_action)) }
            } else {
                refreshCoordinator.invalidateMetrics()
                refreshCoordinator.invalidateProfile()
            }
        }
    }

    /** Eliminar un objetivo (activo o del historial). */
    fun onDeleteGoal(goalId: Long) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            deleteGoal(uid, goalId)
                .onSuccess { refreshCoordinator.invalidateMetrics() }
                .onFailure { _state.update { it.copy(actionError = appContext.getString(R.string.err_generic_action)) } }
        }
    }

    /** Subir foto de progreso desde el picker del sistema. */
    fun onAddPhoto(uri: android.net.Uri) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val f = java.io.File(appContext.cacheDir, "progress_${System.currentTimeMillis()}.jpg")
                    appContext.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                    f
                }.getOrNull()
            }
            if (file == null) {
                _state.update { it.copy(isSaving = false, actionError = appContext.getString(R.string.err_generic_action)) }
                return@launch
            }
            addPhoto(uid, file, unit = _state.value.unitSystem)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    refreshCoordinator.invalidateMetrics()
                }
                .onFailure {
                    _state.update { it.copy(isSaving = false, actionError = appContext.getString(R.string.err_generic_action)) }
                }
        }
    }

    fun onDeletePhoto(photoId: Long) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            deletePhoto(uid, photoId)
                .onSuccess { refreshCoordinator.invalidateMetrics() }
                .onFailure { _state.update { it.copy(actionError = appContext.getString(R.string.err_generic_action)) } }
        }
    }

    fun clearActionError() = _state.update { it.copy(actionError = null) }
    fun retry() = reload(initial = true)
}
