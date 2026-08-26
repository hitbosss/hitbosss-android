package com.hitbosss.domain.repository

import com.hitbosss.domain.model.BodyComposition
import com.hitbosss.domain.model.BodyHistoryPoint
import com.hitbosss.domain.model.BodyTrend
import com.hitbosss.domain.model.GoalHistoryEntry
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.model.StrengthStats
import com.hitbosss.domain.model.StrengthMark
import com.hitbosss.domain.model.TrainingEntry
import java.io.File

/** Métricas (contrato /api/metrics). Usuario = token; unidad = measurementSystem del usuario. */
interface MetricsRepository {
    suspend fun getBodyComposition(unit: String): Result<BodyComposition?>
    suspend fun getBodyCompositionHistory(metric: String, range: String, unit: String): Result<List<BodyHistoryPoint>>
    suspend fun getBodyTrend(period: String, unit: String, tzOffset: Int): Result<BodyTrend>
    suspend fun updateBodyComposition(
        weight: Double? = null, height: Double? = null,
        muscle: Double? = null, fat: Double? = null, unit: String,
    ): Result<Unit>
    suspend fun updateBodyPoint(metric: String, pointId: Long, value: Double, unit: String): Result<Unit>
    suspend fun deleteBodyPoint(metric: String, pointId: Long): Result<Unit>

    suspend fun getGoal(metric: String, unit: String): Result<MetricGoal?>
    suspend fun createGoal(metric: String, target: Double, unit: String): Result<MetricGoal>
    suspend fun getGoalHistory(metric: String, unit: String): Result<List<GoalHistoryEntry>>
    suspend fun deleteGoal(goalId: Long): Result<Unit>

    suspend fun getProgressPhotos(unit: String): Result<List<ProgressPhoto>>
    suspend fun uploadProgressPhoto(photo: File, unit: String): Result<Unit>
    suspend fun deleteProgressPhoto(photoId: Long): Result<Unit>

    suspend fun getStrengthStats(exercise: String, unit: String): Result<StrengthStats>
    suspend fun createTraining(exercise: String, weight: Double, performedAt: Long, unit: String): Result<Unit>
    suspend fun getTrainings(exercise: String, range: String, unit: String): Result<List<TrainingEntry>>
    suspend fun updateTraining(trainingId: Long, weight: Double, unit: String): Result<Unit>
    suspend fun deleteTraining(trainingId: Long): Result<Unit>
    suspend fun getStrengthEvolution(exercise: String, range: String, unit: String): Result<List<StrengthMark>>
    suspend fun getStrengthBests(unit: String): Result<Map<String, Double>>

    suspend fun getStrengthGoal(exercise: String, unit: String): Result<MetricGoal?>
    suspend fun createStrengthGoal(exercise: String, target: Double, unit: String): Result<MetricGoal>
    suspend fun getStrengthGoalHistory(exercise: String, unit: String): Result<List<GoalHistoryEntry>>
    suspend fun deleteStrengthGoal(goalId: Long): Result<Unit>
}
