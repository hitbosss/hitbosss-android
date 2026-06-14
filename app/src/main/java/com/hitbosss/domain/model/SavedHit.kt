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
)
