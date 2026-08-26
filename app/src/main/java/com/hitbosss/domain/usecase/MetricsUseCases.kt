package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.BodyComposition
import com.hitbosss.domain.model.BodyHistoryPoint
import com.hitbosss.domain.model.BodyTrend
import com.hitbosss.domain.model.GoalHistoryEntry
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.model.StrengthStats
import com.hitbosss.domain.model.StrengthMark
import com.hitbosss.domain.model.TrainingEntry
import com.hitbosss.domain.repository.MetricsRepository
import java.io.File
import javax.inject.Inject

class GetBodyCompositionUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(unit: String): Result<BodyComposition?> = repo.getBodyComposition(unit)
}

class GetBodyHistoryUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(metric: String, range: String, unit: String): Result<List<BodyHistoryPoint>> =
        repo.getBodyCompositionHistory(metric, range, unit)
}

class GetBodyTrendUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(period: String, unit: String, tzOffset: Int): Result<BodyTrend> =
        repo.getBodyTrend(period, unit, tzOffset)
}

class UpdateBodyCompositionUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(
        weight: Double? = null, height: Double? = null,
        muscle: Double? = null, fat: Double? = null, unit: String,
    ): Result<Unit> = repo.updateBodyComposition(weight, height, muscle, fat, unit)
}

class GetMetricGoalUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(metric: String, unit: String): Result<MetricGoal?> = repo.getGoal(metric, unit)
}

class CreateMetricGoalUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(metric: String, target: Double, unit: String): Result<MetricGoal> =
        repo.createGoal(metric, target, unit)
}

class GetGoalHistoryUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(metric: String, unit: String): Result<List<GoalHistoryEntry>> = repo.getGoalHistory(metric, unit)
}

class DeleteMetricGoalUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(goalId: Long): Result<Unit> = repo.deleteGoal(goalId)
}

class GetProgressPhotosUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(unit: String): Result<List<ProgressPhoto>> = repo.getProgressPhotos(unit)
}

class UploadProgressPhotoUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(photo: File, unit: String): Result<Unit> = repo.uploadProgressPhoto(photo, unit)
}

class DeleteProgressPhotoUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(photoId: Long): Result<Unit> = repo.deleteProgressPhoto(photoId)
}

class GetStrengthStatsUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(exercise: String, unit: String): Result<StrengthStats> = repo.getStrengthStats(exercise, unit)
}

class CreateTrainingUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(exercise: String, weight: Double, performedAt: Long, unit: String): Result<Unit> =
        repo.createTraining(exercise, weight, performedAt, unit)
}

class UpdateTrainingUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(trainingId: Long, weight: Double, unit: String): Result<Unit> =
        repo.updateTraining(trainingId, weight, unit)
}

class UpdateBodyPointUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(metric: String, pointId: Long, value: Double, unit: String): Result<Unit> =
        repo.updateBodyPoint(metric, pointId, value, unit)
}

class DeleteBodyPointUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(metric: String, pointId: Long): Result<Unit> = repo.deleteBodyPoint(metric, pointId)
}

class DeleteTrainingUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(trainingId: Long): Result<Unit> = repo.deleteTraining(trainingId)
}

class GetTrainingsUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(exercise: String, range: String, unit: String): Result<List<TrainingEntry>> =
        repo.getTrainings(exercise, range, unit)
}

class GetStrengthEvolutionUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(exercise: String, range: String, unit: String): Result<List<StrengthMark>> =
        repo.getStrengthEvolution(exercise, range, unit)
}

class GetStrengthBestsUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(unit: String): Result<Map<String, Double>> = repo.getStrengthBests(unit)
}

// --- Objetivo de fuerza por ejercicio ---

class GetStrengthGoalUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(exercise: String, unit: String): Result<MetricGoal?> = repo.getStrengthGoal(exercise, unit)
}

class CreateStrengthGoalUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(exercise: String, target: Double, unit: String): Result<MetricGoal> =
        repo.createStrengthGoal(exercise, target, unit)
}

class GetStrengthGoalHistoryUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(exercise: String, unit: String): Result<List<GoalHistoryEntry>> =
        repo.getStrengthGoalHistory(exercise, unit)
}

class DeleteStrengthGoalUseCase @Inject constructor(private val repo: MetricsRepository) {
    suspend operator fun invoke(goalId: Long): Result<Unit> = repo.deleteStrengthGoal(goalId)
}
