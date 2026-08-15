package com.hitbosss.presentation.feature.metrics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.R
import com.hitbosss.core.RefreshCoordinator
import com.hitbosss.domain.model.BodyComposition
import com.hitbosss.domain.model.BodyHistoryPoint
import com.hitbosss.domain.model.BodyTrend
import com.hitbosss.domain.model.GoalHistoryEntry
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.usecase.CreateMetricGoalUseCase
import com.hitbosss.domain.usecase.DeleteMetricGoalUseCase
import com.hitbosss.domain.usecase.GetGoalHistoryUseCase
import com.hitbosss.domain.usecase.GetBodyCompositionUseCase
import com.hitbosss.domain.usecase.GetBodyHistoryUseCase
import com.hitbosss.domain.usecase.GetBodyTrendUseCase
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetMetricGoalUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import com.hitbosss.domain.usecase.GetProgressPhotosUseCase
import com.hitbosss.domain.usecase.UpdateBodyCompositionUseCase
import com.hitbosss.domain.usecase.DeleteProgressPhotoUseCase
import com.hitbosss.domain.usecase.UploadProgressPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Las 3 body-metrics de la pestaña Físico. */
enum class PhysicalMetric(val apiKey: String) { Weight("weight"), Fat("fat"), Muscle("muscle") }

data class MetricsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val personalInfo: PersonalInfo? = null,
    val composition: BodyComposition? = null,
    val goals: Map<PhysicalMetric, MetricGoal?> = emptyMap(),
    // Historial de objetivos por métrica, cargado bajo demanda al desplegar.
    val goalHistory: Map<PhysicalMetric, List<GoalHistoryEntry>> = emptyMap(),
    val photos: List<ProgressPhoto> = emptyList(),
    // Serie de evolución cacheada por (métrica, timeframe week/month).
    val history: Map<Pair<PhysicalMetric, String>, List<BodyHistoryPoint>> = emptyMap(),
    // Tendencia (actual vs anterior) cacheada por período ("week"/"month").
    val trend: Map<String, BodyTrend> = emptyMap(),
    val hasError: Boolean = false,
    val actionError: String? = null,
    // Título del popup de acción; null = título genérico "algo salió mal".
    val actionErrorTitle: String? = null,
    val isSaving: Boolean = false,
) {
    val unitSystem: String get() = personalInfo?.measurementSystem ?: "metric"
    val weightUnit: String get() = if (unitSystem == "imperial") "lbs" else "kg"
    fun currentValue(metric: PhysicalMetric): Double? = when (metric) {
        PhysicalMetric.Weight -> composition?.weightKg
        PhysicalMetric.Fat -> composition?.fatKg      // grasa en masa (kg/lbs), no %
        PhysicalMetric.Muscle -> composition?.muscleKg
    }
    fun unitFor(metric: PhysicalMetric): String = weightUnit
}

/** ViewModel de la sub-pestaña FÍSICO (composición, objetivos, evolución, fotos). */
@HiltViewModel
class MetricsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getPersonalInfo: GetPersonalInfoUseCase,
    private val getComposition: GetBodyCompositionUseCase,
    private val getHistory: GetBodyHistoryUseCase,
    private val getTrend: GetBodyTrendUseCase,
    private val updateComposition: UpdateBodyCompositionUseCase,
    private val getGoal: GetMetricGoalUseCase,
    private val createGoal: CreateMetricGoalUseCase,
    private val getGoalHistoryUseCase: GetGoalHistoryUseCase,
    private val deleteGoalUseCase: DeleteMetricGoalUseCase,
    private val getPhotos: GetProgressPhotosUseCase,
    private val uploadPhoto: UploadProgressPhotoUseCase,
    private val deletePhoto: DeleteProgressPhotoUseCase,
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
        viewModelScope.launch { refreshCoordinator.profile.collect { reload() } }
    }

    fun refresh() = reload(showRefreshing = true)
    fun refreshIfStale() { if (System.currentTimeMillis() - lastLoadedAt > staleMs) reload() }

    private fun reload(initial: Boolean = false, showRefreshing: Boolean = false) {
        if (reloading) return
        val uid = getCurrentUser()?.uid ?: return
        reloading = true
        _state.update { it.copy(isLoading = initial, isRefreshing = showRefreshing, hasError = false) }
        viewModelScope.launch {
            // La unidad la manda el servidor en personalInfo (measurement_system). Hay que traer info
            // PRIMERO y derivar la unidad de ahí; si no, composición/objetivos se piden con la unidad
            // vieja (o "metric") mientras altura/gráfica usan la nueva → valores en kg con etiqueta lbs.
            val info = getPersonalInfo(uid)
            val unit = info.getOrNull()?.measurementSystem
                ?: _state.value.personalInfo?.measurementSystem ?: "metric"
            val compD = async { getComposition(unit) }
            val goalsD = PhysicalMetric.entries.associateWith { m -> async { getGoal(m.apiKey, unit) } }
            val photosD = async { getPhotos(unit) }
            val comp = compD.await()
            val goals = goalsD.mapValues { it.value.await().getOrNull()?.withProgress(comp.getOrNull()) }
            val photos = photosD.await()
            lastLoadedAt = System.currentTimeMillis()
            reloading = false
            if (info.isFailure && comp.isFailure) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, hasError = true) }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false, isRefreshing = false,
                    personalInfo = info.getOrNull() ?: it.personalInfo,
                    composition = comp.getOrNull() ?: it.composition,
                    goals = goals,
                    photos = photos.getOrNull() ?: it.photos,
                    history = emptyMap(), // se recarga bajo demanda
                    trend = emptyMap(), // idem
                    goalHistory = emptyMap(), // idem: se refresca al desplegar
                )
            }
            // Precarga la evolución semanal de peso (lo primero que se ve).
            loadHistory(PhysicalMetric.Weight, "week")
        }
    }

    /** Carga (y cachea) la serie de una métrica en un timeframe (week/month). */
    fun loadHistory(metric: PhysicalMetric, timeframe: String) {
        val key = metric to timeframe
        if (_state.value.history.containsKey(key)) return
        viewModelScope.launch {
            getHistory(metric.apiKey, timeframe, _state.value.unitSystem).onSuccess { points ->
                _state.update { it.copy(history = it.history + (key to points)) }
            }
        }
    }

    /** Carga (y cachea) la tendencia (actual vs anterior) de un período. tzOffset del dispositivo. */
    fun loadTrend(period: String) {
        if (_state.value.trend.containsKey(period)) return
        val tzOffset = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
        viewModelScope.launch {
            getTrend(period, _state.value.unitSystem, tzOffset).onSuccess { bt ->
                _state.update { it.copy(trend = it.trend + (period to bt)) }
            }
        }
    }

    /** Carga (y cachea) el historial de objetivos de una métrica al desplegar "Historial de objetivos". */
    fun loadGoalHistory(metric: PhysicalMetric) {
        if (_state.value.goalHistory.containsKey(metric)) return
        viewModelScope.launch {
            getGoalHistoryUseCase(metric.apiKey, _state.value.unitSystem).onSuccess { list ->
                _state.update { it.copy(goalHistory = it.goalHistory + (metric to list)) }
            }
        }
    }

    /** Borra una entrada del historial (pulsación larga → confirmación). Recarga tras borrar. */
    fun onDeleteGoal(goalId: Long) {
        viewModelScope.launch {
            deleteGoalUseCase(goalId)
                .onSuccess { refreshCoordinator.invalidateMetrics() }
                .onFailure { _state.update { it.copy(actionError = appContext.getString(R.string.err_generic_action)) } }
        }
    }

    fun onAddRecord(metric: PhysicalMetric, value: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val unit = _state.value.unitSystem
            val result = when (metric) {
                PhysicalMetric.Weight -> updateComposition(weight = value, unit = unit)
                PhysicalMetric.Fat -> updateComposition(fat = value, unit = unit)
                PhysicalMetric.Muscle -> updateComposition(muscle = value, unit = unit)
            }
            finishSave(result, alsoProfile = metric == PhysicalMetric.Weight)
        }
    }

    /** Editor de composición: solo grasa y músculo (masa). "Otras" lo deriva la UI (peso − músculo − grasa). */
    fun onEditComposition(fat: Double?, muscle: Double?) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            finishSave(updateComposition(fat = fat, muscle = muscle, unit = _state.value.unitSystem))
        }
    }

    fun onCreateGoal(metric: PhysicalMetric, target: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            createGoal(metric.apiKey, target, _state.value.unitSystem)
                .onSuccess { _state.update { it.copy(isSaving = false) }; refreshCoordinator.invalidateMetrics() }
                .onFailure { failSave() }
        }
    }

    /** Lápiz de "Medidas corporales": peso + altura juntos en una fila de body_log (spec Módulo 1). */
    fun onEditMeasures(height: Double?, weight: Double?) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val unit = _state.value.unitSystem
            val result = updateComposition(
                weight = weight?.takeIf { it > 0 }, height = height?.takeIf { it > 0 }, unit = unit,
            )
            _state.update { it.copy(isSaving = false) }
            if (result.isFailure) _state.update { it.copy(actionError = appContext.getString(R.string.err_generic_action)) }
            else { refreshCoordinator.invalidateMetrics(); refreshCoordinator.invalidateProfile() }
        }
    }

    fun onAddPhoto(uri: android.net.Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val f = java.io.File(appContext.cacheDir, "progress_${System.currentTimeMillis()}.jpg")
                    appContext.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                    f
                }.getOrNull()
            }
            if (file == null) { failSave(); return@launch }
            uploadPhoto(file, _state.value.unitSystem)
                .onSuccess { _state.update { it.copy(isSaving = false) }; refreshCoordinator.invalidateMetrics() }
                .onFailure { e ->
                    // El backend responde 409 con {error} para dos casos de la subida de fotos (Retrofit no
                    // trae el body en e.message → leerlo del errorBody, igual que CompleteProfileViewModel).
                    val body = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string().orEmpty()
                    when {
                        body.contains("METRICS_REQUIRED") -> showActionError(
                            R.string.metrics_photo_needs_update, R.string.metrics_photo_needs_update_title)
                        body.contains("PHOTO_LIMIT_REACHED") -> showActionError(R.string.metrics_photo_limit)
                        else -> failSave()
                    }
                }
        }
    }

    fun onDeletePhoto(photoId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            deletePhoto(photoId)
                .onSuccess { _state.update { it.copy(isSaving = false) }; refreshCoordinator.invalidateMetrics() }
                .onFailure { failSave() }
        }
    }

    private fun finishSave(result: Result<Unit>, alsoProfile: Boolean = false) {
        _state.update { it.copy(isSaving = false) }
        if (result.isSuccess) {
            refreshCoordinator.invalidateMetrics()
            if (alsoProfile) refreshCoordinator.invalidateProfile()
        } else _state.update { it.copy(actionError = appContext.getString(R.string.err_generic_action)) }
    }

    /** Rellena currentValue/progress del objetivo desde la composición actual (la API da target + reached).
     *  "Alcanzado" lo decide el SERVIDOR (con dirección real según baseline); la barra es direccional
     *  (te acercas al target por arriba o por abajo), así un objetivo de BAJAR peso no sale "cumplido"
     *  por estar por encima del target. */
    private fun MetricGoal.withProgress(comp: BodyComposition?): MetricGoal {
        val current = when (metric) {
            "weight" -> comp?.weightKg
            "fat" -> comp?.fatKg
            "muscle" -> comp?.muscleKg
            else -> null
        } ?: 0.0
        val prog = when {
            reached == true -> 1.0
            targetValue <= 0.0 || current <= 0.0 -> 0.0
            current < targetValue -> current / targetValue   // por debajo del target → subiendo hacia él
            else -> targetValue / current                    // por encima del target → bajando hacia él
        }.coerceIn(0.0, 1.0)
        return copy(currentValue = current, progress = prog)
    }

    private fun failSave() = _state.update { it.copy(isSaving = false, actionError = appContext.getString(R.string.err_generic_action)) }

    /** Muestra el popup de acción con un mensaje (y título opcional). Corta el estado de guardado. */
    private fun showActionError(msgRes: Int, titleRes: Int? = null) = _state.update {
        it.copy(isSaving = false, actionError = appContext.getString(msgRes), actionErrorTitle = titleRes?.let(appContext::getString))
    }

    fun clearActionError() = _state.update { it.copy(actionError = null, actionErrorTitle = null) }
    fun retry() = reload(initial = true)
}
