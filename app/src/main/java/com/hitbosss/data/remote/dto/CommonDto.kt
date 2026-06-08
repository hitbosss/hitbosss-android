package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

/** Respuesta genérica de acciones (leave, delete, create, report…). */
@Serializable
data class MessageResponseDto(
    val message: String? = null,
    val success: Boolean? = null,
)
