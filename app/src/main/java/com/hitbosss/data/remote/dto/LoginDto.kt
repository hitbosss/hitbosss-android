package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

/** Respuesta de GET /login. */
@Serializable
data class LoginResponseDto(
    val isUserInApi: Boolean? = null,
    val measurementSystem: String? = null,
)
