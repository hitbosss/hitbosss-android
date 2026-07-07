package com.hitbosss.domain.model

/** Registro de composición corporal (peso obligatorio, grasa/músculo opcionales). */
data class BodyLog(
    val id: Long,
    val weight: Measurement,
    val bodyFatPct: Double?,
    val muscleMass: Measurement?,
    val loggedAt: Long,
)

/** Objetivo de peso corporal (exercise == null) o de fuerza por ejercicio. */
data class MetricGoal(
    val id: Long,
    val type: String, // "weight" | "strength"
    val target: Measurement,
    val status: String, // "active" | "archived"
    val createdAt: Long,
    val archivedAt: Long?,
)

/** Objetivo activo + historial de archivados. */
data class MetricGoals(
    val active: MetricGoal?,
    val history: List<MetricGoal>,
)

/** Punto de la gráfica de fuerza. */
data class StrengthEntry(
    val id: Long,
    val lift: Measurement,
    val performedAt: Long,
)

/** Serie temporal de un ejercicio: entrenamientos manuales + HITs oficiales (con vídeo). */
data class StrengthHistory(
    val trainings: List<StrengthEntry>,
    val hits: List<StrengthEntry>,
)

/** Foto de progreso con snapshot de medidas de su fecha. */
data class ProgressPhoto(
    val id: Long,
    val photoUrl: String,
    val takenAt: Long,
    val weight: Measurement?,
    val bodyFatPct: Double?,
    val muscleMass: Measurement?,
)
