package com.hitbosss.presentation.feature.metrics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.R
import com.hitbosss.core.RefreshCoordinator
import com.hitbosss.domain.model.MetricGoals
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.Sport
import com.hitbosss.domain.model.SportRanking
import com.hitbosss.domain.model.StrengthHistory
import com.hitbosss.domain.model.UserProfile
import com.hitbosss.domain.usecase.AddMetricGoalUseCase
import com.hitbosss.domain.usecase.AddStrengthLogUseCase
import com.hitbosss.domain.usecase.DeleteMetricGoalUseCase
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetMetricGoalsUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import com.hitbosss.domain.usecase.GetRankingUseCase
import com.hitbosss.domain.usecase.GetStrengthHistoryUseCase
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
    val history: StrengthHistory = StrengthHistory(emptyList(), emptyList()),
    val goals: MetricGoals = MetricGoals(null, emptyList()),
    // Rankings por deporte (apiValue -> ranking) para los datos competitivos y la comparativa.
    val rankings: Map<String, SportRanking> = emptyMap(),
    val profile: UserProfile? = null,
    val unitSystem: String = "metric",
    val currentUserId: String? = null,
    val hasError: Boolean = false,
    val actionError: String? = null,
    val isSaving: Boolean = false,
) {
    val weightUnit: String get() = if (unitSystem == "imperial") "lbs" else "kg"
}

/** ViewModel de la sub-pestaña FUERZA (todo depende del ejercicio seleccionado). */
@HiltViewModel
class StrengthMetricsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getStrengthHistory: GetStrengthHistoryUseCase,
    private val addStrengthLog: AddStrengthLogUseCase,
    private val getGoals: GetMetricGoalsUseCase,
    private val addGoal: AddMetricGoalUseCase,
    private val deleteGoal: DeleteMetricGoalUseCase,
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
        // Subir un HIT también cambia la gráfica y la comparativa.
        viewModelScope.launch { refreshCoordinator.ranking.collect { reload() } }
    }

    fun refresh() = reload(showRefreshing = true)

    fun refreshIfStale() {
        if (System.currentTimeMillis() - lastLoadedAt > staleMs) reload()
    }

    /** Cambiar de ejercicio: recarga historial y objetivo de ese ejercicio. */
    fun onSelectExercise(category: RankingCategory) {
        if (category == _state.value.selected) return
        _state.update { it.copy(selected = category) }
        loadExerciseData()
    }

    private fun reload(initial: Boolean = false, showRefreshing: Boolean = false) {
        if (reloading) return
        val uid = getCurrentUser()?.uid ?: return
        reloading = true
        _state.update { it.copy(isLoading = initial, isRefreshing = showRefreshing, hasError = false) }
        viewModelScope.launch {
            val infoD = async { getPersonalInfo(uid) }
            val profileD = async { getUserProfile(uid) }
            val plD = async { getRanking(Sport.Powerlifting.apiValue) }
            val cfD = async { getRanking(Sport.Crossfit.apiValue) }
            val exercise = _state.value.selected.apiKey
            val historyD = async { getStrengthHistory(uid, exercise) }
            val goalsD = async { getGoals(uid, "strength", exercise) }

            val info = infoD.await()
            val profile = profileD.await()
            val rankings = buildMap {
                plD.await().getOrNull()?.let { put(Sport.Powerlifting.apiValue, it) }
                cfD.await().getOrNull()?.let { put(Sport.Crossfit.apiValue, it) }
            }
            val history = historyD.await()
            val goals = goalsD.await()
            lastLoadedAt = System.currentTimeMillis()
            reloading = false
            if (history.isFailure && profile.isFailure) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, hasError = true) }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    unitSystem = info.getOrNull()?.measurementSystem ?: it.unitSystem,
                    profile = profile.getOrNull() ?: it.profile,
                    rankings = rankings.ifEmpty { it.rankings },
                    history = history.getOrNull() ?: it.history,
                    goals = goals.getOrNull() ?: it.goals,
                )
            }
        }
    }

    /** Recarga solo lo dependiente del ejercicio (al cambiar en el picker). */
    private fun loadExerciseData() {
        val uid = getCurrentUser()?.uid ?: return
        val exercise = _state.value.selected.apiKey
        viewModelScope.launch {
            val historyD = async { getStrengthHistory(uid, exercise) }
            val goalsD = async { getGoals(uid, "strength", exercise) }
            val history = historyD.await()
            val goals = goalsD.await()
            _state.update {
                it.copy(
                    history = history.getOrNull() ?: StrengthHistory(emptyList(), emptyList()),
                    goals = goals.getOrNull() ?: MetricGoals(null, emptyList()),
                )
            }
        }
    }

    fun onAddTraining(lift: Double) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            addStrengthLog(uid, _state.value.selected.apiKey, lift, _state.value.unitSystem)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    loadExerciseData()
                }
                .onFailure {
                    _state.update { it.copy(isSaving = false, actionError = appContext.getString(R.string.err_generic_action)) }
                }
        }
    }

    fun onAddGoal(target: Double) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            addGoal(uid, "strength", _state.value.selected.apiKey, target, _state.value.unitSystem)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    loadExerciseData()
                }
                .onFailure {
                    _state.update { it.copy(isSaving = false, actionError = appContext.getString(R.string.err_generic_action)) }
                }
        }
    }

    fun onDeleteGoal(goalId: Long) {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            deleteGoal(uid, goalId)
                .onSuccess { loadExerciseData() }
                .onFailure { _state.update { it.copy(actionError = appContext.getString(R.string.err_generic_action)) } }
        }
    }

    fun clearActionError() = _state.update { it.copy(actionError = null) }
    fun retry() = reload(initial = true)
}
