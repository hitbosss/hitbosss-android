package com.hitbosss.domain.model

/**
 * Modelos de Métricas (contrato /api/metrics). El servidor devuelve cada medida en la unidad pedida
 * (kg/lbs, cm/in); el mapper la desenvuelve a Double (los campos `*Kg`/`*Cm` guardan el valor en la
 * unidad del usuario, no necesariamente métrico). Los DERIVADOS (%, "otras", IMC, progreso de objetivo)
 * los calcula mobile, no la API.
 */

/** Composición corporal: peso, altura, músculo y grasa (todo masa/longitud). "Otras" y % se derivan. */
data class BodyComposition(
    val weightKg: Double?,
    val heightCm: Double?,
    val muscleKg: Double?,
    val fatKg: Double?,
    val weightMeasuredAt: Long?,
    val heightMeasuredAt: Long?,
    val muscleMeasuredAt: Long?,
    val fatMeasuredAt: Long?,
) {
    // % sobre el peso total (ratio → independiente de la unidad). "Otras" = peso − músculo − grasa.
    val fatPercent: Double? get() = pctOf(fatKg)
    val musclePercent: Double? get() = pctOf(muscleKg)
    val otherKg: Double? get() =
        if (weightKg != null && muscleKg != null && fatKg != null) (weightKg - muscleKg - fatKg).coerceAtLeast(0.0) else null
    val otherPercent: Double? get() = pctOf(otherKg)

    private fun pctOf(v: Double?): Double? =
        if (v != null && weightKg != null && weightKg > 0) v / weightKg * 100 else null
}

/** Punto de la serie temporal de una métrica (weight/fat/muscle). */
data class BodyHistoryPoint(val value: Double, val measuredAt: Long)

/**
 * Objetivo de una body-metric (weight/fat/muscle). La API solo da `targetValue`; `currentValue` y
 * `progress` los rellena el ViewModel cruzando con la composición actual.
 */
data class MetricGoal(
    val metric: String,          // "weight" | "fat" | "muscle"
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val progress: Double = 0.0,  // 0..1
    val reached: Boolean? = null, // del servidor (con dirección real, según baseline). null = desconocido.
)

/** Entrada del historial de objetivos. `reached` se congela al archivar (null si sigue activo). */
data class GoalHistoryEntry(
    val id: Long?,
    val targetValue: Double,
    val createdAt: Long,
    val archivedAt: Long?,
    val reached: Boolean?,
)

/**
 * Tendencia (M5): punto unificado (semana → día+valor; mes → inicio-de-semana+promedio) y comparación
 * período actual vs anterior por métrica. Las medias las calcula mobile ignorando los huecos.
 */
data class TrendPoint(val date: Long, val value: Double?)

data class MetricTrend(val current: List<TrendPoint>, val previous: List<TrendPoint>) {
    private fun List<TrendPoint>.avg(): Double? =
        mapNotNull { it.value }.let { if (it.isEmpty()) null else it.average() }
    val currentAvg: Double? get() = current.avg()
    val previousAvg: Double? get() = previous.avg()
}

data class BodyTrend(val period: String, val weight: MetricTrend, val fat: MetricTrend, val muscle: MetricTrend) {
    fun forMetric(key: String): MetricTrend = when (key) {
        "fat" -> fat
        "muscle" -> muscle
        else -> weight
    }
}

/** Datos competitivos de fuerza, calculados en el servidor. Cualquier campo puede ser null. */
data class StrengthStats(
    val accumulatedImprovementKg: Double?,
    val progressRateKgPerMonth: Double?,
    val communityAdvantageKg: Double?,
    val rankingPercentage: Double?,
)

/** Entrenamiento manual (sin vídeo) para la evolución de fuerza. El ejercicio lo da el contexto (path). */
data class TrainingEntry(val weightKg: Double, val performedAt: Long)

/** Marca de evolución del ejercicio (del servidor): entreno manual (isHit=false) o HIT real (isHit=true). */
data class StrengthMark(val weightKg: Double, val performedAt: Long, val isHit: Boolean)

/** Foto de progreso con snapshot de medidas (masa). El % de grasa se deriva. */
data class ProgressPhoto(
    val id: Long?,
    val photoUrl: String,
    val takenAt: Long,
    val weightKg: Double?,
    val fatKg: Double?,
    val muscleKg: Double?,
) {
    val fatPercent: Double? get() =
        if (fatKg != null && weightKg != null && weightKg > 0) fatKg / weightKg * 100 else null
}
