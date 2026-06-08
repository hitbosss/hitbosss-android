package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

/** GET /users/personalInformation/{id} — datos del perfil (Wilks + editor). */
@Serializable
data class PersonalInfoDto(
    val id: String? = null,
    val profilePic: String? = null,
    val coverPic: String? = null,
    val username: String? = null,
    val fullName: String? = null,
    val description: String? = null,
    val gender: String? = null,
    val birthDate: Long? = null,
    val countryCode: String? = null,
    val weight: MeasurementDto? = null,
    val height: MeasurementDto? = null,
    val measurementSystem: String? = null,
    val socialNetworks: List<SocialNetworkDto> = emptyList(),
)
