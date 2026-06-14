package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable
import androidx.compose.foundation.layout.height

/** Body de POST /users/{id} (el id va en la ruta). */
@Serializable
data class CreateUserRequestDto(
    val username: String,
    val gender: String,
    val countryCode: String,
    val height: Double,
    val weight: Double,
    val birthDate: Long,
    val unit: String,
)

@Serializable
data class CreateUserResponseDto(
    val message: String? = null,
    val username: String? = null,
)
