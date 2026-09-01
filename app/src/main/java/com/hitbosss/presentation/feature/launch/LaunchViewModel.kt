package com.hitbosss.presentation.feature.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.BuildConfig
import com.hitbosss.domain.repository.ConfigRepository
import com.hitbosss.domain.repository.LoginRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.update

/** Destino al que arranca la app, decidido en el splash (equivale a AppStartManager de iOS). */
enum class LaunchTarget { LOADING, FORCE_UPDATE, WELCOME, COMPLETE_PROFILE, MAIN }

/** ¿La versión instalada es menor que la mínima que exige la API? Compara por segmentos (1.2.0). */
internal fun needsForcedUpdate(currentVersion: String, minimumVersion: String): Boolean {
    fun parts(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
    val (cur, min) = parts(currentVersion) to parts(minimumVersion)
    for (i in 0 until maxOf(cur.size, min.size)) {
        val x = cur.getOrElse(i) { 0 }
        val y = min.getOrElse(i) { 0 }
        if (x != y) return x < y
    }
    return false
}

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val loginRepository: LoginRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _target = MutableStateFlow(LaunchTarget.LOADING)
    val target: StateFlow<LaunchTarget> = _target.asStateFlow()

    init {
        decide()
    }

    private fun decide() {
        viewModelScope.launch {
            // 1) Force update (si la API falla, no bloquea — mismo criterio que iOS).
            configRepository.getConfig().getOrNull()?.let { config ->
                if (needsForcedUpdate(BuildConfig.VERSION_NAME, config.minimumVersion)) {
                    _target.value = LaunchTarget.FORCE_UPDATE
                    return@launch
                }
            }

            // 2) ¿Sesión Firebase?
            if (getCurrentUser() == null) {
                _target.value = LaunchTarget.WELCOME
                return@launch
            }

            // 3) ¿Registrado en la DB? Si la API falla NO asumimos que no lo está: lo dejamos pasar.
            loginRepository.checkUserExists()
                .onSuccess { result ->
                    _target.value = if (result.isUserInApi) LaunchTarget.MAIN else LaunchTarget.COMPLETE_PROFILE
                }
                .onFailure {
                    _target.value = LaunchTarget.MAIN
                }
        }
    }
}
