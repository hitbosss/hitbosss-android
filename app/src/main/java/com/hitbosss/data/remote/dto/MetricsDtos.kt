package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

// Contrato /api/metrics (spec feature-metrics-api). Las masas/longitudes salen como {value, unit}
// ya en la unidad pedida (MeasurementDto, reutilizado de RankingDto); el mapper desenvuelve a Double.
// La entrada va plana + unit compartido.

// ===== Composición corporal =====

@Serializable
data class BodyCompositionDto(
    val weight: MeasurementDto? = null,
    val height: MeasurementDto? = null,
    val muscle: MeasurementDto? = null,
    val fat: MeasurementDto? = null,
    val weightMeasuredAt: Long? = null,
    val heightMeasuredAt: Long? = null,
    val muscleMeasuredAt: Long? = null,
    val fatMeasuredAt: Long? = null,
)

@Serializable
data class BodyHistoryPointDto(val value: Double? = null, val unit: String? = null, val measuredAt: Long? = null)

/** POST /metrics/body-composition: campos planos (solo los provistos) + unit. */
@Serializable
data class UpdateBodyCompositionDto(
    val weight: Double? = null,
    val height: Double? = null,
    val muscle: Double? = null,
    val fat: Double? = null,
    val unit: String,
)

// ===== Tendencia (body-composition/trend) =====

/** Punto de serie: semana → {date, value}; mes → {weekStart, weekEnd, average}. */
@Serializable
data class SeriesPointDto(
    val date: Long? = null,
    val value: Double? = null,
    val weekStart: Long? = null,
    val weekEnd: Long? = null,
    val average: Double? = null,
)

@Serializable
data class MetricSeriesDto(val unit: String? = null, val points: List<SeriesPointDto> = emptyList())

@Serializable
data class TrendPeriodDto(
    val rangeStart: Long? = null,
    val rangeEnd: Long? = null,
    val weight: MetricSeriesDto? = null,
    val fat: MetricSeriesDto? = null,
    val muscle: MetricSeriesDto? = null,
)

@Serializable
data class TrendDto(val period: String? = null, val current: TrendPeriodDto? = null, val previous: TrendPeriodDto? = null)

// ===== Objetivos =====

@Serializable
data class GoalDto(val metric: String? = null, val target: MeasurementDto? = null, val createdAt: Long? = null, val reached: Boolean? = null)

@Serializable
data class CreateGoalRequestDto(val metric: String, val target: Double, val unit: String)

@Serializable
data class GoalHistoryEntryDto(
    val id: Long? = null,
    val target: MeasurementDto? = null,
    val createdAt: Long? = null,
    val archivedAt: Long? = null,
    val reached: Boolean? = null,
)

// ===== Fuerza =====

@Serializable
data class StrengthStatsDto(
    val accumulatedImprovement: MeasurementDto? = null,
    val progressRate: MeasurementDto? = null,
    val communityAdvantage: MeasurementDto? = null,
    val rankingPercentage: Double? = null,
)

/** El ejercicio va en el path → no se repite en cada item. */
@Serializable
data class TrainingEntryDto(val weight: MeasurementDto? = null, val performedAt: Long? = null)

/** Punto de evolución: entreno manual o HIT real, etiquetado por `type` (training|hit). */
@Serializable
data class EvolutionPointDto(val weight: MeasurementDto? = null, val performedAt: Long? = null, val type: String? = null)

/** Mejor marca (HIT) por ejercicio — para la comparativa y el ownBest sin cruzar participations. */
@Serializable
data class StrengthBestDto(val exercise: String? = null, val best: MeasurementDto? = null)

@Serializable
data class CreateTrainingRequestDto(val weight: Double, val unit: String, val performedAt: Long? = null)

/** Objetivo de fuerza por ejercicio: la respuesta reutiliza GoalDto (sin metric); el body es {target, unit}. */
@Serializable
data class CreateStrengthGoalRequestDto(val target: Double, val unit: String)

// ===== Fotos de progreso =====

@Serializable
data class ProgressPhotoDto(
    val id: Long? = null,
    val photoUrl: String? = null,
    val takenAt: Long? = null,
    val weight: MeasurementDto? = null,
    val fat: MeasurementDto? = null,
    val muscle: MeasurementDto? = null,
)
