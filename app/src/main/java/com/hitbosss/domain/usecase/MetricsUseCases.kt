package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.BodyLog
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.MetricGoals
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.model.StrengthEntry
import com.hitbosss.domain.model.StrengthHistory
import com.hitbosss.domain.repository.MetricsRepository
import java.io.File
import javax.inject.Inject

class GetBodyLogsUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, from: Long? = null, to: Long? = null): Result<List<BodyLog>> =
        repo.getBodyLogs(userId, from, to)
}

class AddBodyLogUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, weight: Double, bodyFatPct: Double?, muscleMass: Double?, unit: String): Result<BodyLog> =
        repo.addBodyLog(userId, weight, bodyFatPct, muscleMass, unit)
}

class DeleteBodyLogUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, logId: Long): Result<Unit> = repo.deleteBodyLog(userId, logId)
}

class GetMetricGoalsUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, type: String, exercise: String? = null): Result<MetricGoals> =
        repo.getGoals(userId, type, exercise)
}

class AddMetricGoalUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, type: String, exercise: String?, target: Double, unit: String): Result<MetricGoal> =
        repo.addGoal(userId, type, exercise, target, unit)
}

class DeleteMetricGoalUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, goalId: Long): Result<Unit> = repo.deleteGoal(userId, goalId)
}

class GetStrengthHistoryUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, exercise: String, from: Long? = null, to: Long? = null): Result<StrengthHistory> =
        repo.getStrengthHistory(userId, exercise, from, to)
}

class AddStrengthLogUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, exercise: String, lift: Double, unit: String): Result<StrengthEntry> =
        repo.addStrengthLog(userId, exercise, lift, unit)
}

class DeleteStrengthLogUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, logId: Long): Result<Unit> = repo.deleteStrengthLog(userId, logId)
}

class GetProgressPhotosUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String): Result<List<ProgressPhoto>> = repo.getProgressPhotos(userId)
}

class AddProgressPhotoUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(
        userId: String,
        photo: File,
        takenAt: Long? = null,
        weight: Double? = null,
        bodyFatPct: Double? = null,
        muscleMass: Double? = null,
        unit: String,
    ): Result<ProgressPhoto> = repo.addProgressPhoto(userId, photo, takenAt, weight, bodyFatPct, muscleMass, unit)
}

class DeleteProgressPhotoUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(userId: String, photoId: Long): Result<Unit> = repo.deleteProgressPhoto(userId, photoId)
}
