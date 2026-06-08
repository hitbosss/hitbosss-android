package com.hitbosss.domain.model

data class PersonalInfo(
    val gender: String,
    val weight: Measurement,
    val measurementSystem: String,
    val fullName: String = "",
    val username: String = "",
    val description: String = "",
    val profilePicUrl: String? = null,
    val coverPicUrl: String? = null,
    val birthDate: Long = 0,
    val countryCode: String = "",
    val height: Measurement = Measurement(0.0, "cm"),
    val socialNetworks: List<SocialNetwork> = emptyList(),
) {
    /** Peso corporal en kg (para el cálculo de Wilks). */
    val bodyWeightKg: Double
        get() = if (weight.unit.equals("lbs", true)) weight.value / 2.20462 else weight.value
}
