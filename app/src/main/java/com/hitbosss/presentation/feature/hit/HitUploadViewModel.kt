package com.hitbosss.presentation.feature.hit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado del modal "Registra tu HIT": peso introducido + unidad del usuario. */
data class HitSheetUiState(
    val lift: String = "",
    val personalInfo: PersonalInfo? = null,
    val pendingCount: Int = 0,   // HITs guardados sin subir (badge "Saved HITS")
) {
    /** "KG" si métrico, "LB" si imperial. */
    val unitLabel: String
        get() = if (personalInfo?.measurementSystem.equals("imperial", true)) "LB" else "KG"

    /** Unidad para la API/cálculo ("kg" | "lbs"). */
    val unit: String
        get() = if (personalInfo?.measurementSystem.equals("imperial", true)) "lbs" else "kg"

    /** Igual que iOS: peso válido entre 1 y 600. */
    val canRecord: Boolean
        get() = lift.toDoubleOrNull()?.let { it in 1.0..600.0 } == true
}

@HiltViewModel
class HitUploadViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getPersonalInfo: GetPersonalInfoUseCase,
    private val savedHitStore: com.hitbosss.data.local.SavedHitStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HitSheetUiState())
    val state: StateFlow<HitSheetUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(pendingCount = savedHitStore.getAll().size) }
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                getPersonalInfo(uid).onSuccess { info -> _state.update { it.copy(personalInfo = info) } }
            }
        }
    }

    fun onLiftChange(value: String) =
        _state.update { it.copy(lift = value.filter { c -> c.isDigit() || c == '.' }) }
}
