package com.hitbosss.presentation.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import com.hitbosss.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.abs

/** Sistema de medidas (igual que MeasurementSystem de iOS). */
enum class MeasureSystem(val apiValue: String, val display: String) {
    Metric("metric", "Métrico (Kg, Cm)"),
    Imperial("imperial", "Imperial (Lb, Ft)"),
}

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val fullName: String = "",
    val username: String = "",
    val description: String = "",
    val facebook: String = "",
    val instagram: String = "",
    val tiktok: String = "",
    val x: String = "",
    val gender: String = "male",
    val birthDate: Long? = null,           // millis
    val countryCode: String = "",
    val system: MeasureSystem = MeasureSystem.Metric,
    val weightText: String = "",
    val heightCmText: String = "",
    val heightFeetText: String = "",
    val heightInchesText: String = "",
    val profilePicUrl: String? = null,
    val coverPicUrl: String? = null,
    val profilePicUri: Uri? = null,
    val coverPicUri: Uri? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val original: PersonalInfo? = null,
) {
    val canSave: Boolean get() = hasChanges && fullName.isNotBlank() && username.isNotBlank() && !saving
    val weightUnit: String get() = if (system == MeasureSystem.Metric) "KG" else "LB"

    val hasChanges: Boolean
        get() {
            val o = original ?: return false
            return fullName != o.fullName ||
                username != o.username ||
                description != o.description ||
                gender != o.gender ||
                countryCode != o.countryCode ||
                system.apiValue != o.measurementSystem ||
                (birthDate ?: 0L) / 1000 != o.birthDate ||
                abs((weightText.replace(',', '.').toDoubleOrNull() ?: 0.0) - o.weight.value) > 0.1 ||
                socialsChanged(o) ||
                profilePicUri != null || coverPicUri != null
        }

    private fun socialsChanged(o: PersonalInfo): Boolean {
        fun orig(name: String) = o.socialNetworks.firstOrNull { it.name.equals(name, true) }?.username ?: ""
        return facebook != orig("facebook") || instagram != orig("instagram") ||
            tiktok != orig("tiktok") || x != orig("x")
    }
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getPersonalInfo: GetPersonalInfoUseCase,
    private val updateProfile: UpdateProfileUseCase,
) : ViewModel() {

    val maxNameLength = 25
    val maxUsernameLength = 16
    val maxDescriptionLength = 150
    val maxSocialLength = 30

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        val uid = getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            getPersonalInfo(uid).onSuccess { p ->
                fun social(name: String) = p.socialNetworks.firstOrNull { it.name.equals(name, true) }?.username ?: ""
                val metric = !p.measurementSystem.equals("imperial", true)
                _state.update {
                    it.copy(
                        isLoading = false,
                        original = p,
                        fullName = p.fullName,
                        username = p.username,
                        description = p.description,
                        gender = p.gender.ifBlank { "male" },
                        birthDate = if (p.birthDate > 0) p.birthDate * 1000 else null,
                        countryCode = p.countryCode,
                        system = if (metric) MeasureSystem.Metric else MeasureSystem.Imperial,
                        weightText = if (p.weight.value > 0) trimNum(p.weight.value) else "",
                        heightCmText = if (metric && p.height.value > 0) p.height.value.toInt().toString() else "",
                        heightFeetText = if (!metric && p.height.value > 0) (p.height.value.toInt() / 12).toString() else "",
                        heightInchesText = if (!metric && p.height.value > 0) (p.height.value.toInt() % 12).toString() else "",
                        facebook = social("facebook"), instagram = social("instagram"),
                        tiktok = social("tiktok"), x = social("x"),
                        profilePicUrl = p.profilePicUrl, coverPicUrl = p.coverPicUrl,
                    )
                }
            }.onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun onFullName(v: String) { if (v.length <= maxNameLength) _state.update { it.copy(fullName = v) } }
    fun onUsername(v: String) { if (v.length <= maxUsernameLength) _state.update { it.copy(username = v) } }
    fun onDescription(v: String) { if (v.length <= maxDescriptionLength) _state.update { it.copy(description = v) } }
    fun onFacebook(v: String) { if (v.length <= maxSocialLength) _state.update { it.copy(facebook = v) } }
    fun onInstagram(v: String) { if (v.length <= maxSocialLength) _state.update { it.copy(instagram = v) } }
    fun onTiktok(v: String) { if (v.length <= maxSocialLength) _state.update { it.copy(tiktok = v) } }
    fun onX(v: String) { if (v.length <= maxSocialLength) _state.update { it.copy(x = v) } }
    fun onGender(g: String) = _state.update { it.copy(gender = g) }
    fun onBirthDate(millis: Long) = _state.update { it.copy(birthDate = millis) }
    fun onCountry(code: String) = _state.update { it.copy(countryCode = code) }
    fun onSystem(s: MeasureSystem) = _state.update { it.copy(system = s) }
    fun onWeight(v: String) = _state.update { it.copy(weightText = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    fun onHeightCm(v: String) = _state.update { it.copy(heightCmText = v.filter { c -> c.isDigit() }) }
    fun onHeightFeet(v: String) = _state.update { it.copy(heightFeetText = v.filter { c -> c.isDigit() }) }
    fun onHeightInches(v: String) = _state.update { it.copy(heightInchesText = v.filter { c -> c.isDigit() }) }
    fun onProfilePicked(uri: Uri) = _state.update { it.copy(profilePicUri = uri) }
    fun onCoverPicked(uri: Uri) = _state.update { it.copy(coverPicUri = uri) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun save() {
        val s = _state.value
        val o = s.original ?: return
        val uid = getCurrentUser()?.uid ?: return
        if (!s.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val weight = s.weightText.replace(',', '.').toDoubleOrNull() ?: 0.0
            val height = if (s.system == MeasureSystem.Metric) {
                (s.heightCmText.toIntOrNull() ?: 0).toDouble()
            } else {
                ((s.heightFeetText.toIntOrNull() ?: 0) * 12 + (s.heightInchesText.toIntOrNull() ?: 0)).toDouble()
            }

            val fields = buildMap {
                if (s.fullName != o.fullName) put("fullName", s.fullName.trim())
                if (s.username != o.username) put("username", s.username.trim())
                if (s.countryCode != o.countryCode) put("countryCode", s.countryCode)
                if (s.description != o.description) put("description", s.description)
                if (s.gender != o.gender) put("gender", s.gender)
                if (weight > 0) put("weight", trimNum(weight))
                if (height > 0) put("height", height.toInt().toString())
                put("unit", s.system.apiValue)
                if (s.system.apiValue != o.measurementSystem) put("measurementSystem", s.system.apiValue)
                (s.birthDate?.let { it / 1000 } ?: 0L).let { if (it != o.birthDate && it > 0) put("birthDate", it.toString()) }
                buildSocialsJson(s)?.let { put("socialNetworks", it) }
            }

            val profileFile = s.profilePicUri?.let { uriToFile(it, "profile") }
            val coverFile = s.coverPicUri?.let { uriToFile(it, "cover") }

            updateProfile(uid, fields, profileFile, coverFile)
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Error") } }
        }
    }

    /** JSON de redes sociales (igual que iOS: name/url/username) o null si no cambian. */
    private fun buildSocialsJson(s: EditProfileUiState): String? {
        val o = s.original ?: return null
        fun orig(name: String) = o.socialNetworks.firstOrNull { it.name.equals(name, true) }?.username ?: ""
        val changed = s.facebook != orig("facebook") || s.instagram != orig("instagram") ||
            s.tiktok != orig("tiktok") || s.x != orig("x")
        if (!changed) return null
        val items = listOf(
            Triple("instagram", "https://instagram.com/", s.instagram),
            Triple("x", "https://x.com/", s.x),
            Triple("tiktok", "https://tiktok.com/@", s.tiktok),
            Triple("facebook", "https://facebook.com/", s.facebook),
        ).filter { it.third.isNotBlank() }
        return items.joinToString(",", "[", "]") { (name, base, user) ->
            """{"name":"$name","url":"$base$user","username":"$user"}"""
        }
    }

    private suspend fun uriToFile(uri: Uri, prefix: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)!!.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        }.getOrNull()
    }

    private fun trimNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
}
