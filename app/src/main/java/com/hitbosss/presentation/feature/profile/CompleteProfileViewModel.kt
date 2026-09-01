package com.hitbosss.presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.CreateUserData
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt
import com.hitbosss.R
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.compose.foundation.layout.height

enum class MeasurementSystem(val apiValue: String) { Metric("metric"), Imperial("imperial") }

data class CompleteProfileUiState(
    val gender: String = "",                 // "male" | "female"
    val countryCode: String = "ES",
    val system: MeasurementSystem = MeasurementSystem.Metric,
    val heightCm: Int = 170,
    val heightFeet: Int = 5,
    val heightInches: Int = 7,
    val weightInteger: Int = 70,             // kg si métrico, lb si imperial
    val weightDecimal: Int = 0,
    val birthDay: Int = 1,
    val birthMonth: Int = 1,                 // 1..12
    val birthYear: Int = Calendar.getInstance().get(Calendar.YEAR) - 25,
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val usernameTaken: Boolean = false,
    val success: Boolean = false,
) {
    /** Altura final en cm. */
    val heightCmValue: Double
        get() = if (system == MeasurementSystem.Metric) heightCm.toDouble()
        else ((heightFeet * 12 + heightInches) * 2.54)

    /** Peso final en kg. */
    val weightKgValue: Double
        get() {
            val v = weightInteger + weightDecimal / 10.0
            return if (system == MeasurementSystem.Metric) v else v * 0.45359237
        }

    val birthMillis: Long
        get() = Calendar.getInstance().apply {
            clear(); set(birthYear, birthMonth - 1, birthDay)
        }.timeInMillis

    val isUsernameValid: Boolean get() = isValidUsername(username)

    fun validateStep(step: Int): Boolean = when (step) {
        1 -> gender.isNotBlank()
        2 -> countryCode.length == 2
        3 -> true
        4 -> true
        5 -> true
        6 -> isUsernameValid
        else -> false
    }
}

/** 3-16 alfanuméricos; admite '.' o '_' no consecutivos ni al inicio/fin. */
fun isValidUsername(u: String): Boolean {
    if (u.length !in 3..16) return false
    if (!Regex("^[a-zA-Z0-9._]+$").matches(u)) return false
    if (u.first() == '.' || u.first() == '_' || u.last() == '.' || u.last() == '_') return false
    if (Regex("[._]{2}").containsMatchIn(u)) return false
    return true
}

@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CompleteProfileUiState())
    val state: StateFlow<CompleteProfileUiState> = _state.asStateFlow()

    fun onGender(v: String) = _state.update { it.copy(gender = v) }
    fun onCountry(v: String) = _state.update { it.copy(countryCode = v.uppercase().take(2)) }
    fun onSystem(s: MeasurementSystem) = _state.update { it.copy(system = s) }
    fun onHeightCm(v: Int) = _state.update { it.copy(heightCm = v) }
    fun onHeightFeet(v: Int) = _state.update { it.copy(heightFeet = v) }
    fun onHeightInches(v: Int) = _state.update { it.copy(heightInches = v) }
    fun onWeightInteger(v: Int) = _state.update { it.copy(weightInteger = v) }
    fun onWeightDecimal(v: Int) = _state.update { it.copy(weightDecimal = v) }
    fun onBirth(day: Int, month: Int, year: Int) = _state.update { it.copy(birthDay = day, birthMonth = month, birthYear = year) }
    fun onUsername(v: String) = _state.update { it.copy(username = v.take(16).filterNot { c -> c.isWhitespace() }, usernameTaken = false) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun submit() {
        val s = _state.value
        if (!s.validateStep(6)) return
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, usernameTaken = false) }
            userRepository.createUser(
                uid,
                CreateUserData(
                    username = s.username.trim(),
                    gender = s.gender,
                    countryCode = s.countryCode,
                    height = s.heightCmValue.roundToInt().toDouble(),
                    weight = (s.weightKgValue * 10).roundToInt() / 10.0,
                    birthDate = s.birthMillis / 1000,
                    unit = s.system.apiValue,
                ),
            ).onSuccess { _state.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e ->
                    // Retrofit no incluye el body en e.message: hay que leer el errorBody del 409.
                    // El backend usa 409 para dos casos: "Username already exists" (nombre cogido)
                    // y "User already exists" (este uid ya tiene perfil → continuar como éxito).
                    val body = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string().orEmpty()
                    when {
                        body.contains("Username already exists", true) ->
                            _state.update { it.copy(isLoading = false, usernameTaken = true, error = null) }
                        body.contains("User already exists", true) ->
                            _state.update { it.copy(isLoading = false, success = true) }
                        else ->
                            _state.update { it.copy(isLoading = false, error = context.getString(R.string.err_create_profile)) }
                    }
                }
        }
    }
}
