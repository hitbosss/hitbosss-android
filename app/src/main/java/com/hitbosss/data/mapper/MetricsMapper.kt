package com.hitbosss.data.mapper

import com.hitbosss.data.remote.dto.BodyLogDto
import com.hitbosss.data.remote.dto.GoalsResponseDto
import com.hitbosss.data.remote.dto.MeasurementDto
import com.hitbosss.data.remote.dto.MetricGoalDto
import com.hitbosss.data.remote.dto.ProgressPhotoDto
import com.hitbosss.data.remote.dto.StrengthEntryDto
import com.hitbosss.data.remote.dto.StrengthHistoryDto
import com.hitbosss.domain.model.BodyLog
import com.hitbosss.domain.model.Measurement
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.MetricGoals
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.model.StrengthEntry
import com.hitbosss.domain.model.StrengthHistory

private fun MeasurementDto?.toMeasurementOrNull(): Measurement? =
    this?.value?.let { Measurement(it, unit ?: "kg") }

fun BodyLogDto.toDomain(): BodyLog? {
    val w = weight.toMeasurementOrNull() ?: return null
    return BodyLog(
        id = id ?: return null,
        weight = w,
        bodyFatPct = bodyFatPct,
        muscleMass = muscleMass.toMeasurementOrNull(),
        loggedAt = loggedAt ?: return null,
    )
}

fun MetricGoalDto.toDomain(): MetricGoal? {
    val t = target.toMeasurementOrNull() ?: return null
    return MetricGoal(
        id = id ?: return null,
        type = type ?: "weight",
        target = t,
        status = status ?: "active",
        createdAt = createdAt ?: 0,
        archivedAt = archivedAt,
    )
}

fun GoalsResponseDto.toDomain(): MetricGoals = MetricGoals(
    active = active?.toDomain(),
    history = history.mapNotNull { it.toDomain() },
)

fun StrengthEntryDto.toDomain(): StrengthEntry? {
    val l = lift.toMeasurementOrNull() ?: return null
    return StrengthEntry(id = id ?: return null, lift = l, performedAt = performedAt ?: return null)
}

fun StrengthHistoryDto.toDomain(): StrengthHistory = StrengthHistory(
    trainings = trainings.mapNotNull { it.toDomain() },
    hits = hits.mapNotNull { it.toDomain() },
)

fun ProgressPhotoDto.toDomain(): ProgressPhoto? {
    return ProgressPhoto(
        id = id ?: return null,
        photoUrl = photoUrl ?: return null,
        takenAt = takenAt ?: return null,
        weight = weight.toMeasurementOrNull(),
        bodyFatPct = bodyFatPct,
        muscleMass = muscleMass.toMeasurementOrNull(),
    )
}
