package com.hitbosss.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Expone el sistema de medida del usuario (kg/lbs) para las tablas de umbrales del tutorial. */
@HiltViewModel
class TutorialViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    userRepository: UserRepository,
) : ViewModel() {
    private val _isMetric = MutableStateFlow(true)
    val isMetric: StateFlow<Boolean> = _isMetric.asStateFlow()

    init {
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                userRepository.getPersonalInfo(uid).onSuccess { info ->
                    _isMetric.value = !info.measurementSystem.equals("imperial", true)
                }
            }
        }
    }
}
