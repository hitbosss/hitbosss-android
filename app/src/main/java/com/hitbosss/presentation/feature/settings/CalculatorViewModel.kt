package com.hitbosss.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.util.WilksCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow

data class CalculatorUiState(
    val gender: String = "male",       // "male" | "female"
    val bodyWeight: String = "",
    val lift: String = "",
    val unit: String = "kg",           // "kg" | "lbs" (sistema del usuario, igual que iOS)
    val score: Double = 0.0,
)

/**
 * Calculadora de Points (1:1 con SettingsPointsCalculatorView de iOS): la unidad se toma del sistema
 * de medida del usuario y el resultado solo se recalcula al pulsar "Calcular Points".
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    init {
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                userRepository.getPersonalInfo(uid).onSuccess { info ->
                    val u = if (info.measurementSystem.equals("imperial", true)) "lbs" else "kg"
                    _state.update { it.copy(unit = u) }
                }
            }
        }
    }

    fun onGender(value: String) = _state.update { it.copy(gender = value) }
    fun onBodyWeight(value: String) = _state.update { it.copy(bodyWeight = value.filterDecimal()) }
    fun onLift(value: String) = _state.update { it.copy(lift = value.filterDecimal()) }

    fun calculate() {
        val s = _state.value
        val bw = s.bodyWeight.toDoubleOrNull()
        val lf = s.lift.toDoubleOrNull()
        if (bw == null || lf == null) {
            _state.update { it.copy(score = 0.0) }
            return
        }
        // WilksCalculator espera kg; si el usuario es imperial, convertimos antes (igual que iOS).
        val factor = if (s.unit == "lbs") WilksCalculator.KG_TO_LBS else 1.0
        val score = WilksCalculator.calculate(bw / factor, lf / factor, s.gender)
        _state.update { it.copy(score = score) }
    }

    private fun String.filterDecimal() = filter { it.isDigit() || it == '.' }
}
