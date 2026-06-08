package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

/** Cuerpo de la denuncia de un hit (igual que iOS: {comment}). */
@Serializable
data class ReportRequestDto(
    val comment: String? = null,
)
