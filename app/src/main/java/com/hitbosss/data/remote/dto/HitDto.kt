package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

/** Respuesta de POST /ranking/uploadhit. */
@Serializable
data class UploadHitResponseDto(
    val message: String? = null,
    val videoUrl: String? = null,
    val hitId: Int? = null,
)
