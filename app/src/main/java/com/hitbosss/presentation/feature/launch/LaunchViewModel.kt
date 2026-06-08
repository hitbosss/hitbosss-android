package com.hitbosss.presentation.feature.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.BuildConfig
import com.hitbosss.domain.usecase.CheckMinimumVersionUseCase
import com.hitbosss.domain.usecase.CheckUserExistsUseCase
import com.hitbosss.domain.usecase.GetAppConfigUseCase
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Destino al que arranca la app, decidido en el splash (equivale a AppStartManager de iOS). */
enum class LaunchTarget { LOADING, FORCE_UPDATE, WELCOME, COMPLETE_PROFILE, MAIN }

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val getAppConfig: GetAppConfigUseCase,
    private val checkMinimumVersion: CheckMinimumVersionUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val checkUserExists: CheckUserExistsUseCase,
) : ViewModel() {

    private val _target = MutableStateFlow(LaunchTarget.LOADING)
    val target: StateFlow<LaunchTarget> = _target.asStateFlow()

    init {
        decide()
    }

    private fun decide() {
        viewModelScope.launch {
            // 1) Force update (si la API falla, no bloquea — mismo criterio que iOS).
            getAppConfig().getOrNull()?.let { config ->
                if (checkMinimumVersion(BuildConfig.VERSION_NAME, config.minimumVersion)) {
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
            checkUserExists()
                .onSuccess { result ->
                    _target.value = if (result.isUserInApi) LaunchTarget.MAIN else LaunchTarget.COMPLETE_PROFILE
                }
                .onFailure {
                    _target.value = LaunchTarget.MAIN
                }
        }
    }
}
