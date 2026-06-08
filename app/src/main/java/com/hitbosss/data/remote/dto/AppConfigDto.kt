package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

/** Respuesta de GET /config (force update). */
@Serializable
data class AppConfigDto(
    val minimumVersion: String? = null,
)
