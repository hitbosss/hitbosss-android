package com.hitbosss.presentation.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.RankingEntry
import com.hitbosss.domain.model.Sport
import com.hitbosss.domain.model.SportRanking
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetRankingUseCase
import com.hitbosss.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RankingUiState(
    val isLoading: Boolean = false,
    val sport: String = "powerlifting",
    val ranking: SportRanking? = null,
    val selectedCategory: RankingCategory = RankingCategory.PlOfficial,
    val searchText: String = "",
    val selectedLevels: Set<String> = emptySet(),   // keys: elite/advanced/...
    val orderBy: RankingOrder = RankingOrder.Lift,
    val selectedLocation: RankingLocation = RankingLocation.World,
    val selectedAges: Set<AgeCategory> = emptySet(),
    val selectedGender: RankingGender = RankingGender.Both,
    val currentUserId: String? = null,
    val currentUserCountry: String? = null,
    val currentUserPicUrl: String? = null,
    val error: String? = null,
) {
    private val sportEnum get() = Sport.entries.firstOrNull { it.apiValue == sport } ?: Sport.Powerlifting

    val categories: List<RankingCategory> get() = RankingCategory.forSport(sportEnum)

    val isOfficial: Boolean
        get() = selectedCategory == RankingCategory.PlOfficial || selectedCategory == RankingCategory.CfOfficial

    /** Por puntos se usa levelWilks; por peso, levelWeight (igual que iOS). */
    val orderByPoints: Boolean get() = orderBy == RankingOrder.Points
    private fun RankingEntry.level() = if (orderByPoints) levelWilks else levelWeight

    private val allEntries: List<RankingEntry> get() = ranking?.byCategory?.get(selectedCategory).orEmpty()

    /** Indica si hay algún filtro activo (para resaltar el botón de filtro en azul). */
    val hasActiveFilters: Boolean
        get() = selectedLevels.isNotEmpty() || selectedAges.isNotEmpty() ||
            selectedGender != RankingGender.Both || selectedLocation != RankingLocation.World

    val displayedEntries: List<RankingEntry>
        get() = allEntries
            .filter { e ->
                (searchText.isBlank() || e.username.contains(searchText, ignoreCase = true)) &&
                    (selectedLevels.isEmpty() || (e.level() ?: "") in selectedLevels) &&
                    (selectedGender.apiValue == null || e.gender?.equals(selectedGender.apiValue, true) == true) &&
                    (selectedLocation != RankingLocation.National || currentUserCountry == null ||
                        e.countryCode.equals(currentUserCountry, true))
            }
            // Orden: por peso levantado o por puntos (wilks). Re-numera la posición mostrada.
            .sortedWith(
                if (orderByPoints) compareByDescending { it.score }
                else compareByDescending<RankingEntry> { it.lift?.value ?: 0.0 }.thenByDescending { it.score },
            )
            .mapIndexed { i, e -> e.copy(rank = i + 1) }

    /** Fila "Tú": entrada del usuario actual en esta categoría (o null si no participa). */
    val currentUserEntry: RankingEntry? get() = allEntries.firstOrNull { it.userId == currentUserId }

    /**
     * Ejercicios requeridos para la card "Aún no participas" (igual que iOS):
     * 3 grupos por deporte; en powerlifting el 3º combina Peso muerto / Peso muerto sumo.
     */
    val requiredExercises: List<RequiredExercise>
        get() {
            val r = ranking ?: return emptyList()
            fun hasUser(cat: RankingCategory) = r.byCategory[cat]?.any { it.userId == currentUserId } == true
            return when (sportEnum) {
                Sport.Powerlifting -> listOf(
                    RequiredExercise("Sentadilla", hasUser(RankingCategory.Squat)),
                    RequiredExercise("Press banca", hasUser(RankingCategory.BenchPress)),
                    RequiredExercise("Peso muerto/Sumo", hasUser(RankingCategory.Deadlift) || hasUser(RankingCategory.SumoDeadlift)),
                )
                Sport.Crossfit -> listOf(
                    RequiredExercise("Snatch", hasUser(RankingCategory.Snatch)),
                    RequiredExercise("Clean", hasUser(RankingCategory.Clean)),
                    RequiredExercise("Clean & Jerk", hasUser(RankingCategory.CleanAndJerk)),
                )
            }
        }

    /** Se muestra la card si el usuario ha completado menos de 3 grupos (iOS: completedExercises.count < 3). */
    val notParticipating: Boolean get() = requiredExercises.count { it.done } < 3
}

/** Un ejercicio requerido para participar en el ranking oficial + si el usuario ya lo tiene. */
data class RequiredExercise(val label: String, val done: Boolean)

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val getRanking: GetRankingUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getUserProfile: GetUserProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RankingUiState(currentUserId = getCurrentUser()?.uid))
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

    init {
        load(_state.value.sport)
        // País del usuario actual (para la fila "Tú" aunque no esté clasificado).
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                getUserProfile(uid).onSuccess { p ->
                    // Foto fiable para la fila "Tú": la entrada del ranking a veces no trae profilePic.
                    _state.update { it.copy(currentUserCountry = p.countryCode, currentUserPicUrl = p.profilePicUrl) }
                }
            }
        }
    }

    fun load(sport: String) {
        val official = RankingCategory.forSport(
            Sport.entries.firstOrNull { it.apiValue == sport } ?: Sport.Powerlifting,
        ).first()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, sport = sport, selectedCategory = official) }
            getRanking(sport)
                .onSuccess { ranking -> _state.update { it.copy(isLoading = false, ranking = ranking) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Error") } }
        }
    }

    fun selectCategory(category: RankingCategory) = _state.update { it.copy(selectedCategory = category) }
    fun onSearch(text: String) = _state.update { it.copy(searchText = text) }

    fun toggleLevel(level: String) = _state.update {
        it.copy(selectedLevels = if (level in it.selectedLevels) it.selectedLevels - level else it.selectedLevels + level)
    }

    fun setOrder(order: RankingOrder) = _state.update { it.copy(orderBy = order) }

    fun setLocation(location: RankingLocation) = _state.update { it.copy(selectedLocation = location) }

    fun toggleAge(age: AgeCategory) = _state.update {
        it.copy(selectedAges = if (age in it.selectedAges) it.selectedAges - age else it.selectedAges + age)
    }

    fun setGender(gender: RankingGender) = _state.update { it.copy(selectedGender = gender) }
}
