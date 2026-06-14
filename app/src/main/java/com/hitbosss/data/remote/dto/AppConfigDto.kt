package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.update

/** Respuesta de GET /config (force update). */
@Serializable
data class AppConfigDto(
    val minimumVersion: String? = null,
)
