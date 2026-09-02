package com.hitbosss.presentation.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.RankingEntry
import com.hitbosss.domain.model.Sport
import com.hitbosss.domain.model.SportRanking
import com.hitbosss.domain.repository.RankingRepository
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RankingUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
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
    // Ubicación GPS del usuario para el filtro "Current" + popup si deniega el permiso (igual que iOS).
    val userLat: Double? = null,
    val userLng: Double? = null,
    val showLocationPermissionPopup: Boolean = false,
) {
    private val sportEnum get() = Sport.entries.firstOrNull { it.apiValue == sport } ?: Sport.Powerlifting

    val categories: List<RankingCategory> get() = RankingCategory.forSport(sportEnum)

    val isOfficial: Boolean
        get() = selectedCategory == RankingCategory.PlOfficial || selectedCategory == RankingCategory.CfOfficial

    /** Por puntos se usa levelWilks; por peso, levelWeight (igual que iOS). */
    val orderByPoints: Boolean get() = orderBy == RankingOrder.Points
    private fun RankingEntry.level() = if (orderByPoints) levelWilks else levelWeight

    /** Edad en años a partir del timestamp unix (segundos), igual que calculateAge de iOS. */
    private fun ageFrom(birthDateSeconds: Long): Int {
        val birth = java.util.Calendar.getInstance().apply { timeInMillis = birthDateSeconds * 1000 }
        val now = java.util.Calendar.getInstance()
        var age = now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
        if (now.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) age--
        return age
    }

    /** Distancia en km entre dos coordenadas (Haversine vía android.location.Location). */
    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val res = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, res)
        return res[0] / 1000.0
    }

    private val allEntries: List<RankingEntry> get() = ranking?.byCategory?.get(selectedCategory).orEmpty()

    /** Indica si hay algún filtro activo (para resaltar el botón de filtro en azul). */
    val hasActiveFilters: Boolean
        get() = selectedLevels.isNotEmpty() || selectedAges.isNotEmpty() ||
            selectedGender != RankingGender.Both || selectedLocation != RankingLocation.World

    val displayedEntries: List<RankingEntry>
        // Se ordena el ranking COMPLETO y se asigna la posición global (rankedBy) ANTES de filtrar: así, con
        // filtros/búsqueda activos, cada fila conserva su posición real del ranking (originalPosition de iOS),
        // no una renumeración 1..N del subconjunto.
        get() = allEntries
            .rankedBy(orderBy)
            .filter { e ->
                (searchText.isBlank() || e.username.contains(searchText, ignoreCase = true)) &&
                    (selectedLevels.isEmpty() || (e.level() ?: "") in selectedLevels) &&
                    (selectedGender.apiValue == null || e.gender?.equals(selectedGender.apiValue, true) == true) &&
                    (selectedAges.isEmpty() || (e.birthDate > 0 && selectedAges.any { it.matches(ageFrom(e.birthDate)) })) &&
                    (selectedLocation != RankingLocation.National || currentUserCountry == null ||
                        e.countryCode.equals(currentUserCountry, true)) &&
                    // "Current" (igual que iOS): entradas con ubicación dentro de 10 km del GPS del usuario.
                    (selectedLocation != RankingLocation.Current ||
                        (userLat != null && userLng != null && e.latitude != null && e.longitude != null &&
                            distanceKm(userLat, userLng, e.latitude, e.longitude) <= 10.0))
            }

    /**
     * Fila "Tú": la entrada del usuario dentro de la lista ya ordenada/filtrada (displayedEntries),
     * para que su posición coincida siempre con la de la lista bajo el criterio actual (peso/points),
     * igual que iOS (RankingListView usa el mismo displayedRanking para la fila y para "Tú").
     */
    val currentUserEntry: RankingEntry? get() = displayedEntries.firstOrNull { it.userId == currentUserId }

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
                    RequiredExercise(com.hitbosss.R.string.exercise_squat, hasUser(RankingCategory.Squat)),
                    RequiredExercise(com.hitbosss.R.string.exercise_bench, hasUser(RankingCategory.BenchPress)),
                    RequiredExercise(com.hitbosss.R.string.exercise_deadlift_sumo, hasUser(RankingCategory.Deadlift) || hasUser(RankingCategory.SumoDeadlift)),
                )
                Sport.Crossfit -> listOf(
                    RequiredExercise(com.hitbosss.R.string.exercise_snatch, hasUser(RankingCategory.Snatch)),
                    RequiredExercise(com.hitbosss.R.string.exercise_clean, hasUser(RankingCategory.Clean)),
                    RequiredExercise(com.hitbosss.R.string.exercise_clean_jerk, hasUser(RankingCategory.CleanAndJerk)),
                )
            }
        }

    /** Se muestra la card si el usuario ha completado menos de 3 grupos (iOS: completedExercises.count < 3). */
    val notParticipating: Boolean get() = requiredExercises.count { it.done } < 3
}

/** Un ejercicio requerido para participar en el ranking oficial + si el usuario ya lo tiene. */
data class RequiredExercise(@androidx.annotation.StringRes val labelRes: Int, val done: Boolean)

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingRepository: RankingRepository,
    private val userRepository: UserRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(RankingUiState(currentUserId = getCurrentUser()?.uid))
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

    private var reloading = false

    init {
        load(_state.value.sport)
        loadCurrentUserProfile()
        // Recarga dirigida por acción (p. ej. tras subir un HIT) sin resetear la pestaña.
        viewModelScope.launch { refreshCoordinator.ranking.collect { reloadData(showRefreshing = false) } }
    }

    private fun loadCurrentUserProfile() {
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                userRepository.getUserProfile(uid).onSuccess { p ->
                    // Foto fiable para la fila "Tú": la entrada del ranking a veces no trae profilePic.
                    _state.update { it.copy(currentUserCountry = p.countryCode, currentUserPicUrl = p.profilePicUrl) }
                }
            }
        }
    }

    /** Pull-to-refresh manual. */
    fun refresh() = reloadData(showRefreshing = true)

    // Marca de tiempo de la última carga; para el refresco por antigüedad (refreshIfStale).
    private var lastLoadedAt = 0L
    private val staleMs = 5 * 60 * 1000L

    /** Recarga en segundo plano (sin spinner) solo si los datos llevan mucho tiempo sin refrescarse. */
    fun refreshIfStale() {
        if (System.currentTimeMillis() - lastLoadedAt > staleMs) reloadData(showRefreshing = false)
    }

    /** Recarga el ranking del deporte actual (sin tocar la pestaña/filtros). Guarda de concurrencia. */
    private fun reloadData(showRefreshing: Boolean) {
        if (reloading) return
        reloading = true
        lastLoadedAt = System.currentTimeMillis()
        viewModelScope.launch {
            if (showRefreshing) _state.update { it.copy(isRefreshing = true) }
            rankingRepository.getRanking(_state.value.sport)
                .onSuccess { r -> _state.update { it.copy(ranking = r, error = null) } }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "Error") } }
            getCurrentUser()?.uid?.let { uid ->
                userRepository.getUserProfile(uid).onSuccess { p ->
                    _state.update { it.copy(currentUserCountry = p.countryCode, currentUserPicUrl = p.profilePicUrl) }
                }
            }
            _state.update { it.copy(isRefreshing = false) }
            reloading = false
        }
    }

    fun load(sport: String) {
        val official = RankingCategory.forSport(
            Sport.entries.firstOrNull { it.apiValue == sport } ?: Sport.Powerlifting,
        ).first()
        lastLoadedAt = System.currentTimeMillis()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, sport = sport, selectedCategory = official) }
            rankingRepository.getRanking(sport)
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

    /** World/National se aplican directos (filtrado cliente). "Current" pasa por onCurrentLocation*. */
    fun setLocation(location: RankingLocation) = _state.update { it.copy(selectedLocation = location) }

    /** GPS concedido: guarda la ubicación, activa el filtro "Current" y la sube al backend (igual que iOS). */
    fun onCurrentLocationGranted(lat: Double, lng: Double) {
        _state.update { it.copy(selectedLocation = RankingLocation.Current, userLat = lat, userLng = lng) }
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                userRepository.updateProfile(uid, mapOf("latitude" to lat.toString(), "longitude" to lng.toString()), null, null)
            }
        }
    }

    /** Permiso denegado o sin ubicación: se mantiene la opción previa y se avisa con un popup. */
    fun onCurrentLocationDenied() = _state.update { it.copy(showLocationPermissionPopup = true) }

    fun dismissLocationPopup() = _state.update { it.copy(showLocationPermissionPopup = false) }

    fun toggleAge(age: AgeCategory) = _state.update {
        it.copy(selectedAges = if (age in it.selectedAges) it.selectedAges - age else it.selectedAges + age)
    }

    fun setGender(gender: RankingGender) = _state.update { it.copy(selectedGender = gender) }
}
