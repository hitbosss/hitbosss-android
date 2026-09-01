package com.hitbosss.data.mapper

import com.hitbosss.data.remote.dto.BodyCompositionDto
import com.hitbosss.data.remote.dto.BodyHistoryPointDto
import com.hitbosss.data.remote.dto.GoalDto
import com.hitbosss.data.remote.dto.GoalHistoryEntryDto
import com.hitbosss.data.remote.dto.MeasurementDto
import com.hitbosss.data.remote.dto.MetricSeriesDto
import com.hitbosss.data.remote.dto.ProgressPhotoDto
import com.hitbosss.data.remote.dto.SeriesPointDto
import com.hitbosss.data.remote.dto.StrengthStatsDto
import com.hitbosss.data.remote.dto.EvolutionPointDto
import com.hitbosss.data.remote.dto.StrengthBestDto
import com.hitbosss.data.remote.dto.TrainingEntryDto
import com.hitbosss.data.remote.dto.TrendDto
import com.hitbosss.domain.model.BodyComposition
import com.hitbosss.domain.model.BodyHistoryPoint
import com.hitbosss.domain.model.BodyTrend
import com.hitbosss.domain.model.GoalHistoryEntry
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.MetricTrend
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.model.StrengthStats
import com.hitbosss.domain.model.StrengthMark
import com.hitbosss.domain.model.TrendPoint

// El valor ya viene en la unidad pedida; nos quedamos con el número (la etiqueta la pone la UI).
private fun MeasurementDto?.value(): Double? = this?.value

fun BodyCompositionDto.toDomain(): BodyComposition = BodyComposition(
    weightKg = weight.value(),
    heightCm = height.value(),
    muscleKg = muscle.value(),
    fatKg = fat.value(),
    weightMeasuredAt = weightMeasuredAt,
    heightMeasuredAt = heightMeasuredAt,
    muscleMeasuredAt = muscleMeasuredAt,
    fatMeasuredAt = fatMeasuredAt,
)

fun BodyHistoryPointDto.toDomain(): BodyHistoryPoint? {
    val v = value ?: return null
    val at = measuredAt ?: return null
    return BodyHistoryPoint(id ?: return null, v, at)
}

fun GoalDto.toDomain(): MetricGoal? {
    val m = metric ?: return null
    val t = target.value() ?: return null
    return MetricGoal(metric = m, targetValue = t, reached = reached)
}

/**
 * Objetivo de fuerza: metric placeholder "strength". `current`/`reached` los da el SERVIDOR contando
 * entrenos + HITs (getBestLift) → no se recalcula en cliente (fuente única, sin divergencia iOS/Android).
 */
fun GoalDto.toStrengthDomain(): MetricGoal? {
    val t = target.value() ?: return null
    val cur = current.value() ?: 0.0
    val prog = if (t > 0.0 && cur > 0.0) (cur / t).coerceIn(0.0, 1.0) else 0.0
    return MetricGoal(metric = "strength", targetValue = t, currentValue = cur, progress = prog, reached = reached)
}

fun GoalHistoryEntryDto.toDomain(): GoalHistoryEntry? {
    val t = target.value() ?: return null
    val at = createdAt ?: return null
    return GoalHistoryEntry(id = id, targetValue = t, createdAt = at, archivedAt = archivedAt, reached = reached)
}

private fun SeriesPointDto.toTrendPoint(): TrendPoint = TrendPoint(
    date = date ?: weekStart ?: 0L,
    value = value ?: average,
    days = days?.map { it.toTrendPoint() } ?: emptyList(),
)
private fun MetricSeriesDto?.toTrendPoints(): List<TrendPoint> = this?.points?.map { it.toTrendPoint() } ?: emptyList()

fun TrendDto.toDomain(): BodyTrend = BodyTrend(
    period = period ?: "week",
    weight = MetricTrend(current?.weight.toTrendPoints(), previous?.weight.toTrendPoints(), current?.weight?.average, previous?.weight?.average),
    fat = MetricTrend(current?.fat.toTrendPoints(), previous?.fat.toTrendPoints(), current?.fat?.average, previous?.fat?.average),
    muscle = MetricTrend(current?.muscle.toTrendPoints(), previous?.muscle.toTrendPoints(), current?.muscle?.average, previous?.muscle?.average),
)

fun StrengthStatsDto.toDomain(): StrengthStats = StrengthStats(
    accumulatedImprovementKg = accumulatedImprovement.value(),
    progressRateKgPerMonth = progressRate.value(),
    communityAdvantageKg = communityAdvantage.value(),
    rankingPercentage = rankingPercentage,
)

fun EvolutionPointDto.toDomain(): StrengthMark? {
    val w = weight.value() ?: return null
    val at = createdAt ?: return null              // fecha (eje X) — convención de la API
    return StrengthMark(
        weightKg = w, performedAt = at, isHit = type == "hit",
        trainingId = trainingId,
        hitId = hitId, videoUrl = videoUrl,
        videoSecond = performedAt,                 // seek = performedAt de la API (segundo del vídeo)
        wilksScore = wilksScore, levelWeight = levelWeight, levelWilks = levelWilks,
    )
}

/** [{exercise, best}] → mapa exercise(apiKey) → mejor marca (en la unidad pedida). */
fun List<StrengthBestDto>.toBestsMap(): Map<String, Double> =
    mapNotNull { dto -> val e = dto.exercise; val v = dto.best.value(); if (e != null && v != null) e to v else null }.toMap()

fun ProgressPhotoDto.toDomain(): ProgressPhoto? {
    val url = photoUrl ?: return null
    val at = takenAt ?: return null
    return ProgressPhoto(id = id, photoUrl = url, takenAt = at, weightKg = weight.value(), fatKg = fat.value(), muscleKg = muscle.value())
}
