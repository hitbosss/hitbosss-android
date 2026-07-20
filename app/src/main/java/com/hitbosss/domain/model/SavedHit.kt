package com.hitbosss.domain.model

import kotlinx.serialization.Serializable

/** HIT guardado localmente (equivale a HitLocalModel de iOS) para reintentar la subida más tarde. */
@Serializable
data class SavedHit(
    val id: String,
    val videoFileName: String,
    val userId: String,
    val sport: String,        // apiValue
    val exercise: String,     // apiValue
    val lift: Double,
    val unit: String,         // "kg" | "lbs"
    val bodyWeightKg: Double,
    val gender: String,
    val performedAt: Double,
    val contextType: String,  // "global" | "group" | "event"
    val groupId: Int? = null,
    val eventId: Int? = null,
    val createdAt: Long,      // epoch segundos
    // Clave de idempotencia: la misma que usó la subida que falló, para que el reintento
    // desde "HITS guardados" no pueda crear un duplicado si el servidor sí la procesó.
    val clientRequestId: String? = null,
    // Estado de subida (iOS #649: el vídeo se persiste ANTES de tocar la red).
    // "uploading" = en curso; si el proceso muere, al arrancar se reconcilia a "pending".
    val status: String = STATUS_PENDING,
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_UPLOADING = "uploading"
    }
}
