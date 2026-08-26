package com.hitbosss.presentation.feature.metrics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.R
import com.hitbosss.core.RefreshCoordinator
import com.hitbosss.domain.model.GoalHistoryEntry
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.Sport
import com.hitbosss.domain.model.SportRanking
import com.hitbosss.domain.model.StrengthStats
import com.hitbosss.domain.model.StrengthMark
import com.hitbosss.domain.model.UserProfile
import com.hitbosss.domain.usecase.CreateStrengthGoalUseCase
import com.hitbosss.domain.usecase.CreateTrainingUseCase
import com.hitbosss.domain.usecase.UpdateTrainingUseCase
import com.hitbosss.domain.usecase.DeleteTrainingUseCase
import com.hitbosss.domain.usecase.DeleteStrengthGoalUseCase
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import com.hitbosss.domain.usecase.GetRankingUseCase
import com.hitbosss.domain.usecase.GetStrengthGoalHistoryUseCase
import com.hitbosss.domain.usecase.GetStrengthGoalUseCase
import com.hitbosss.domain.usecase.GetStrengthStatsUseCase
import com.hitbosss.domain.usecase.GetStrengthBestsUseCase
import com.hitbosss.domain.usecase.GetStrengthEvolutionUseCase
import com.hitbosss.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StrengthMetricsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selected: RankingCategory = RankingCategory.Squat,
    val stats: StrengthStats? = null,                 // datos competitivos del servidor (del ejercicio)
    val marks: List<StrengthMark> = emptyList(),      // evolución del ejercicio (entrenos ∪ HITs, del servidor)
    val bests: Map<String, Double> = emptyMap(),      // mejor marca (HIT) por ejercicio → comparativa + ownBest
    val strengthGoal: MetricGoal? = null,             // objetivo POR EJERCICIO (currentValue = mejor marca)
    val goalHistory: List<GoalHistoryEntry> = emptyList(),
    val rankings: Map<String, SportRanking> = emptyMap(),
    val profile: UserProfile? = null,                 // HITs oficiales (participations) para la gráfica
    val unitSystem: String = "metric",
    val currentUserId: String? = null,
    val hasError: Boolean = false,
    val actionError: String? = null,
    val isSaving: Boolean = false,
) {
    val weightUnit: String get() = if (unitSystem == "imperial") "lbs" else "kg"
}

/** ViewModel de la sub-pestaña FUERZA: stats del servidor, objetivo por ejercicio y rival on-device. */
@HiltViewModel
class StrengthMetricsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getStrengthStats: GetStrengthStatsUseCase,
    private val getStrengthEvolution: GetStrengthEvolutionUseCase,
    private val getStrengthBests: GetStrengthBestsUseCase,
    private val createTraining: CreateTrainingUseCase,
    private val updateTraining: UpdateTrainingUseCase,
    private val deleteTraining: DeleteTrainingUseCase,
    private val getStrengthGoal: GetStrengthGoalUseCase,
    private val createStrengthGoal: CreateStrengthGoalUseCase,
    private val getStrengthGoalHistory: GetStrengthGoalHistoryUseCase,
    private val deleteStrengthGoal: DeleteStrengthGoalUseCase,
    private val getRanking: GetRankingUseCase,
    private val getPersonalInfo: GetPersonalInfoUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val refreshCoordinator: RefreshCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(StrengthMetricsUiState(currentUserId = null))
    val state: StateFlow<StrengthMetricsUiState> = _state.asStateFlow()

    private var reloading = false
    private var lastLoadedAt = 0L
    private val staleMs = 5 * 60 * 1000L

    init {
        _state.update { it.copy(currentUserId = getCurrentUser()?.uid) }
        reload(initial = true)
        viewModelScope.launch { refreshCoordinator.metrics.collect { reload() } }
        viewModelScope.launch { refreshCoordinator.ranking.collect { reload() } }
    }

    fun refresh() = reload(showRefreshing = true)
    fun refreshIfStale() { if (System.currentTimeMillis() - lastLoadedAt > staleMs) reload() }

    fun onSelectExercise(category: RankingCategory) {
        if (category == _state.value.selected) return
        _state.update { it.copy(selected = category, stats = null, strengthGoal = null, goalHistory = emptyList()) }
        loadExerciseData()
    }

    private fun reload(initial: Boolean = false, showRefreshing: Boolean = false) {
        if (reloading) return
        val uid = getCurrentUser()?.uid ?: return
        reloading = true
        _state.update { it.copy(isLoading = initial, isRefreshing = showRefreshing, hasError = false) }
        viewModelScope.launch {
            val unit = _state.value.unitSystem
            // Compartido: perfil (participations), rankings, sistema de medidas.
            val infoD = async { getPersonalInfo(uid) }
            val profileD = async { getUserProfile(uid) }
            val plD = async { getRanking(Sport.Powerlifting.apiValue) }
            val cfD = async { getRanking(Sport.Crossfit.apiValue) }
            val info = infoD.await()
            val profile = profileD.await()
            val rankings = buildMap {
                plD.await().getOrNull()?.let { put(Sport.Powerlifting.apiValue, it) }
                cfD.await().getOrNull()?.let { put(Sport.Crossfit.apiValue, it) }
            }
            // Mejor por ejercicio (todos): con la unidad ya resuelta de info, no la del estado (puede ser stale).
            val bests = getStrengthBests(info.getOrNull()?.measurementSystem ?: unit).getOrNull()
            lastLoadedAt = System.currentTimeMillis()
            reloading = false
            if (profile.isFailure) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, hasError = true) }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false, isRefreshing = false,
                    unitSystem = info.getOrNull()?.measurementSystem ?: it.unitSystem,
                    profile = profile.getOrNull() ?: it.profile,
                    rankings = rankings.ifEmpty { it.rankings },
                    bests = bests ?: it.bests,
                )
            }
            loadExerciseData() // stats + trainings + objetivo + historial del ejercicio seleccionado
        }
    }

    /** Datos específicos del ejercicio (cambian al elegir otro): stats, entrenamientos, objetivo, historial. */
    private fun loadExerciseData() {
        val exercise = _state.value.selected.apiKey
        val unit = _state.value.unitSystem
        viewModelScope.launch {
            val statsD = async { getStrengthStats(exercise, unit) }
            // "all": la gráfica hace su propia ventana (semana/mes) en cliente, y el "actual" del objetivo
            // = mejor marca histórica (entrenos ∪ HITs del servidor → escalable, sin depender de participations).
            val marksD = async { getStrengthEvolution(exercise, "all", unit) }
            val goalD = async { getStrengthGoal(exercise, unit) }
            val histD = async { getStrengthGoalHistory(exercise, unit) }
            val stats = statsD.await()
            val marks = marksD.await().getOrNull().orEmpty()
            val goal = goalD.await().getOrNull()
            val hist = histD.await().getOrNull().orEmpty()
            _state.update {
                it.copy(
                    stats = stats.getOrNull() ?: it.stats,
                    marks = marks,
                    // current/progreso/reached vienen ya calculados del servidor (entrenos + HITs).
                    strengthGoal = goal,
                    goalHistory = hist,
                )
            }
        }
    }

    fun onAddTraining(lift: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            createTraining(_state.value.selected.apiKey, lift, System.currentTimeMillis() / 1000, _state.value.unitSystem)
                .onSuccess { _state.update { it.copy(isSaving = false) }; refreshCoordinator.invalidateMetrics() }
                .onFailure { failSave() }
        }
    }

    fun onEditTraining(trainingId: Long, weight: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            updateTraining(trainingId, weight, _state.value.unitSystem)
                .onSuccess { _state.update { it.copy(isSaving = false) }; refreshCoordinator.invalidateMetrics() }
                .onFailure { failSave() }
        }
    }

    fun onDeleteTraining(trainingId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            deleteTraining(trainingId)
                .onSuccess { _state.update { it.copy(isSaving = false) }; refreshCoordinator.invalidateMetrics() }
                .onFailure { failSave() }
        }
    }

    /** Objetivo de fuerza del ejercicio seleccionado. */
    fun onAddStrengthGoal(target: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            createStrengthGoal(_state.value.selected.apiKey, target, _state.value.unitSystem)
                .onSuccess { _state.update { it.copy(isSaving = false) }; loadExerciseData() }
                .onFailure { failSave() }
        }
    }

    fun onDeleteStrengthGoal(goalId: Long) {
        viewModelScope.launch {
            deleteStrengthGoal(goalId)
                .onSuccess { loadExerciseData() }
                .onFailure { _state.update { it.copy(actionError = appContext.getString(R.string.err_generic_action)) } }
        }
    }

    private fun failSave() = _state.update { it.copy(isSaving = false, actionError = appContext.getString(R.string.err_generic_action)) }

    fun clearActionError() = _state.update { it.copy(actionError = null) }
    fun retry() = reload(initial = true)
}
