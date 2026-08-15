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
import com.hitbosss.domain.model.TrainingEntry
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
    return BodyHistoryPoint(v, at)
}

fun GoalDto.toDomain(): MetricGoal? {
    val m = metric ?: return null
    val t = target.value() ?: return null
    return MetricGoal(metric = m, targetValue = t, reached = reached)
}

/** Objetivo de fuerza: la API devuelve {target, createdAt} sin metric → metric placeholder "strength". */
fun GoalDto.toStrengthDomain(): MetricGoal? {
    val t = target.value() ?: return null
    return MetricGoal(metric = "strength", targetValue = t)
}

fun GoalHistoryEntryDto.toDomain(): GoalHistoryEntry? {
    val t = target.value() ?: return null
    val at = createdAt ?: return null
    return GoalHistoryEntry(id = id, targetValue = t, createdAt = at, archivedAt = archivedAt, reached = reached)
}

private fun SeriesPointDto.toTrendPoint() = TrendPoint(date = date ?: weekStart ?: 0L, value = value ?: average)
private fun MetricSeriesDto?.toTrendPoints(): List<TrendPoint> = this?.points?.map { it.toTrendPoint() } ?: emptyList()

fun TrendDto.toDomain(): BodyTrend = BodyTrend(
    period = period ?: "week",
    weight = MetricTrend(current?.weight.toTrendPoints(), previous?.weight.toTrendPoints()),
    fat = MetricTrend(current?.fat.toTrendPoints(), previous?.fat.toTrendPoints()),
    muscle = MetricTrend(current?.muscle.toTrendPoints(), previous?.muscle.toTrendPoints()),
)

fun StrengthStatsDto.toDomain(): StrengthStats = StrengthStats(
    accumulatedImprovementKg = accumulatedImprovement.value(),
    progressRateKgPerMonth = progressRate.value(),
    communityAdvantageKg = communityAdvantage.value(),
    rankingPercentage = rankingPercentage,
)

fun TrainingEntryDto.toDomain(): TrainingEntry? {
    val w = weight.value() ?: return null
    val at = performedAt ?: return null
    return TrainingEntry(weightKg = w, performedAt = at)
}

fun EvolutionPointDto.toDomain(): StrengthMark? {
    val w = weight.value() ?: return null
    val at = performedAt ?: return null
    return StrengthMark(weightKg = w, performedAt = at, isHit = type == "hit")
}

/** [{exercise, best}] → mapa exercise(apiKey) → mejor marca (en la unidad pedida). */
fun List<StrengthBestDto>.toBestsMap(): Map<String, Double> =
    mapNotNull { dto -> val e = dto.exercise; val v = dto.best.value(); if (e != null && v != null) e to v else null }.toMap()

fun ProgressPhotoDto.toDomain(): ProgressPhoto? {
    val url = photoUrl ?: return null
    val at = takenAt ?: return null
    return ProgressPhoto(id = id, photoUrl = url, takenAt = at, weightKg = weight.value(), fatKg = fat.value(), muscleKg = muscle.value())
}
