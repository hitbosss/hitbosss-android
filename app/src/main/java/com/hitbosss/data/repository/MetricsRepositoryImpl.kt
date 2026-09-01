package com.hitbosss.data.repository

import com.hitbosss.data.mapper.toBestsMap
import com.hitbosss.data.mapper.toDomain
import com.hitbosss.data.mapper.toStrengthDomain
import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.data.remote.dto.CreateGoalRequestDto
import com.hitbosss.data.remote.dto.CreateStrengthGoalRequestDto
import com.hitbosss.data.remote.dto.CreateTrainingRequestDto
import com.hitbosss.data.remote.dto.UpdateBodyCompositionDto
import com.hitbosss.data.remote.dto.UpdateBodyPointDto
import com.hitbosss.domain.model.BodyComposition
import com.hitbosss.domain.model.BodyHistoryPoint
import com.hitbosss.domain.model.BodyTrend
import com.hitbosss.domain.model.GoalHistoryEntry
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.model.StrengthStats
import com.hitbosss.domain.model.StrengthMark
import com.hitbosss.domain.repository.MetricsRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : MetricsRepository {

    override suspend fun getBodyComposition(unit: String): Result<BodyComposition?> =
        runCatching { api.getBodyComposition(unit).toDomain() }

    override suspend fun getBodyCompositionHistory(metric: String, range: String, unit: String): Result<List<BodyHistoryPoint>> =
        runCatching { api.getBodyCompositionHistory(metric, range, unit).mapNotNull { it.toDomain() } }

    override suspend fun getBodyTrend(period: String, unit: String, tzOffset: Int): Result<BodyTrend> =
        runCatching { api.getBodyTrend(period, unit, tzOffset).toDomain() }

    // Minutos al este de UTC del dispositivo (convención del backend para el "día local": upsert/gate de foto/goals).
    private fun tzOffsetMin() = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

    override suspend fun updateBodyComposition(
        weight: Double?, height: Double?, muscle: Double?, fat: Double?, unit: String,
    ): Result<Unit> =
        runCatching { api.updateBodyComposition(UpdateBodyCompositionDto(weight, height, muscle, fat, unit), tzOffsetMin()); Unit }

    override suspend fun updateBodyPoint(metric: String, pointId: Long, value: Double, unit: String): Result<Unit> =
        runCatching { api.updateBodyPoint(metric, pointId, UpdateBodyPointDto(value, unit)); Unit }

    override suspend fun deleteBodyPoint(metric: String, pointId: Long): Result<Unit> =
        runCatching { api.deleteBodyPoint(metric, pointId) }

    override suspend fun getGoal(metric: String, unit: String): Result<MetricGoal?> =
        runCatching { api.getGoal(metric, unit)?.toDomain() }

    override suspend fun createGoal(metric: String, target: Double, unit: String): Result<MetricGoal> =
        runCatching { api.createGoal(CreateGoalRequestDto(metric, target, unit), tzOffsetMin()).toDomain() ?: error("Invalid goal") }

    override suspend fun getGoalHistory(metric: String, unit: String): Result<List<GoalHistoryEntry>> =
        runCatching { api.getGoalHistory(metric, unit).mapNotNull { it.toDomain() } }

    override suspend fun deleteGoal(goalId: Long): Result<Unit> =
        runCatching { api.deleteGoal(goalId) }

    override suspend fun getProgressPhotos(unit: String): Result<List<ProgressPhoto>> =
        runCatching { api.getProgressPhotos(unit).mapNotNull { it.toDomain() } }

    override suspend fun uploadProgressPhoto(photo: File, unit: String): Result<Unit> =
        runCatching {
            val part = MultipartBody.Part.createFormData(
                "photo", "progress-${System.currentTimeMillis()}.jpg",
                photo.asRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            api.uploadProgressPhoto(part, tzOffsetMin()); Unit
        }

    override suspend fun deleteProgressPhoto(photoId: Long): Result<Unit> =
        runCatching { api.deleteProgressPhoto(photoId) }

    override suspend fun getStrengthStats(exercise: String, unit: String): Result<StrengthStats> =
        runCatching { api.getStrengthStats(exercise, unit).toDomain() }

    override suspend fun createTraining(exercise: String, weight: Double, performedAt: Long, unit: String): Result<Unit> =
        runCatching { api.createTraining(exercise, CreateTrainingRequestDto(weight, unit, performedAt)); Unit }

    override suspend fun updateTraining(trainingId: Long, weight: Double, unit: String): Result<Unit> =
        runCatching { api.updateTraining(trainingId, CreateTrainingRequestDto(weight, unit, null)); Unit }

    override suspend fun deleteTraining(trainingId: Long): Result<Unit> =
        runCatching { api.deleteTraining(trainingId) }

    override suspend fun getStrengthEvolution(exercise: String, range: String, unit: String): Result<List<StrengthMark>> =
        runCatching { api.getStrengthEvolution(exercise, range, unit).mapNotNull { it.toDomain() } }

    override suspend fun getStrengthBests(unit: String): Result<Map<String, Double>> =
        runCatching { api.getStrengthBests(unit).toBestsMap() }

    override suspend fun getStrengthGoal(exercise: String, unit: String): Result<MetricGoal?> =
        runCatching { api.getStrengthGoal(exercise, unit)?.toStrengthDomain() }

    override suspend fun createStrengthGoal(exercise: String, target: Double, unit: String): Result<MetricGoal> =
        runCatching { api.createStrengthGoal(exercise, CreateStrengthGoalRequestDto(target, unit), tzOffsetMin()).toStrengthDomain() ?: error("Invalid goal") }

    override suspend fun getStrengthGoalHistory(exercise: String, unit: String): Result<List<GoalHistoryEntry>> =
        runCatching { api.getStrengthGoalHistory(exercise, unit).mapNotNull { it.toDomain() } }

    override suspend fun deleteStrengthGoal(goalId: Long): Result<Unit> =
        runCatching { api.deleteStrengthGoal(goalId) }
}
