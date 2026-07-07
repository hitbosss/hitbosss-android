package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

/** GET/POST /metrics/{id}/body-logs — registro de composición corporal. */
@Serializable
data class BodyLogDto(
    val id: Long? = null,
    val weight: MeasurementDto? = null,
    val bodyFatPct: Double? = null,
    val muscleMass: MeasurementDto? = null,
    val loggedAt: Long? = null,
)

@Serializable
data class CreateBodyLogRequestDto(
    val weight: Double,
    val bodyFatPct: Double? = null,
    val muscleMass: Double? = null,
    val loggedAt: Long? = null,
    val unit: String,
)

/** GET /metrics/{id}/goals — objetivo activo + historial. */
@Serializable
data class MetricGoalDto(
    val id: Long? = null,
    val type: String? = null,
    val target: MeasurementDto? = null,
    val status: String? = null,
    val createdAt: Long? = null,
    val archivedAt: Long? = null,
)

@Serializable
data class GoalsResponseDto(
    val active: MetricGoalDto? = null,
    val history: List<MetricGoalDto> = emptyList(),
)

@Serializable
data class CreateGoalRequestDto(
    val type: String,
    val exercise: String? = null,
    val target: Double,
    val unit: String,
)

/** GET /metrics/{id}/strength-history — serie temporal de un ejercicio. */
@Serializable
data class StrengthEntryDto(
    val id: Long? = null,
    val lift: MeasurementDto? = null,
    val performedAt: Long? = null,
)

@Serializable
data class StrengthHistoryDto(
    val exercise: String? = null,
    val trainings: List<StrengthEntryDto> = emptyList(),
    val hits: List<StrengthEntryDto> = emptyList(),
)

@Serializable
data class CreateStrengthLogRequestDto(
    val exercise: String,
    val lift: Double,
    val performedAt: Long? = null,
    val unit: String,
)

/** GET /metrics/{id}/photos — foto de progreso con snapshot de medidas. */
@Serializable
data class ProgressPhotoDto(
    val id: Long? = null,
    val photoUrl: String? = null,
    val takenAt: Long? = null,
    val weight: MeasurementDto? = null,
    val bodyFatPct: Double? = null,
    val muscleMass: MeasurementDto? = null,
)
