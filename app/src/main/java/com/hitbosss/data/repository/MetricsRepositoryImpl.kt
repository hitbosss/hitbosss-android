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
import com.hitbosss.domain.model.TrainingEntry
import com.hitbosss.domain.repository.MetricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : MetricsRepository {

    override suspend fun getBodyComposition(unit: String): Result<BodyComposition?> =
        withContext(Dispatchers.IO) { runCatching { api.getBodyComposition(unit).toDomain() } }

    override suspend fun getBodyCompositionHistory(metric: String, range: String, unit: String): Result<List<BodyHistoryPoint>> =
        withContext(Dispatchers.IO) {
            runCatching { api.getBodyCompositionHistory(metric, range, unit).mapNotNull { it.toDomain() } }
        }

    override suspend fun getBodyTrend(period: String, unit: String, tzOffset: Int): Result<BodyTrend> =
        withContext(Dispatchers.IO) { runCatching { api.getBodyTrend(period, unit, tzOffset).toDomain() } }

    // Minutos al este de UTC del dispositivo (convención del backend para el "día local": upsert/gate de foto/goals).
    private fun tzOffsetMin() = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

    override suspend fun updateBodyComposition(
        weight: Double?, height: Double?, muscle: Double?, fat: Double?, unit: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            api.updateBodyComposition(UpdateBodyCompositionDto(weight, height, muscle, fat, unit), tzOffsetMin()); Unit
        }
    }

    override suspend fun updateBodyPoint(metric: String, pointId: Long, value: Double, unit: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { api.updateBodyPoint(metric, pointId, UpdateBodyPointDto(value, unit)); Unit }
        }

    override suspend fun deleteBodyPoint(metric: String, pointId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteBodyPoint(metric, pointId) } }

    override suspend fun getGoal(metric: String, unit: String): Result<MetricGoal?> =
        withContext(Dispatchers.IO) { runCatching { api.getGoal(metric, unit)?.toDomain() } }

    override suspend fun createGoal(metric: String, target: Double, unit: String): Result<MetricGoal> =
        withContext(Dispatchers.IO) {
            runCatching { api.createGoal(CreateGoalRequestDto(metric, target, unit), tzOffsetMin()).toDomain() ?: error("Invalid goal") }
        }

    override suspend fun getGoalHistory(metric: String, unit: String): Result<List<GoalHistoryEntry>> =
        withContext(Dispatchers.IO) {
            runCatching { api.getGoalHistory(metric, unit).mapNotNull { it.toDomain() } }
        }

    override suspend fun deleteGoal(goalId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteGoal(goalId) } }

    override suspend fun getProgressPhotos(unit: String): Result<List<ProgressPhoto>> =
        withContext(Dispatchers.IO) { runCatching { api.getProgressPhotos(unit).mapNotNull { it.toDomain() } } }

    override suspend fun uploadProgressPhoto(photo: File, unit: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val part = MultipartBody.Part.createFormData(
                "photo", "progress-${System.currentTimeMillis()}.jpg",
                photo.asRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            api.uploadProgressPhoto(part, tzOffsetMin()); Unit
        }
    }

    override suspend fun deleteProgressPhoto(photoId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteProgressPhoto(photoId) } }

    override suspend fun getStrengthStats(exercise: String, unit: String): Result<StrengthStats> =
        withContext(Dispatchers.IO) { runCatching { api.getStrengthStats(exercise, unit).toDomain() } }

    override suspend fun createTraining(exercise: String, weight: Double, performedAt: Long, unit: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { api.createTraining(exercise, CreateTrainingRequestDto(weight, unit, performedAt)); Unit }
        }

    override suspend fun updateTraining(trainingId: Long, weight: Double, unit: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { api.updateTraining(trainingId, CreateTrainingRequestDto(weight, unit, null)); Unit }
        }

    override suspend fun deleteTraining(trainingId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteTraining(trainingId) } }

    override suspend fun getTrainings(exercise: String, range: String, unit: String): Result<List<TrainingEntry>> =
        withContext(Dispatchers.IO) { runCatching { api.getTrainings(exercise, range, unit).mapNotNull { it.toDomain() } } }

    override suspend fun getStrengthEvolution(exercise: String, range: String, unit: String): Result<List<StrengthMark>> =
        withContext(Dispatchers.IO) { runCatching { api.getStrengthEvolution(exercise, range, unit).mapNotNull { it.toDomain() } } }

    override suspend fun getStrengthBests(unit: String): Result<Map<String, Double>> =
        withContext(Dispatchers.IO) { runCatching { api.getStrengthBests(unit).toBestsMap() } }

    override suspend fun getStrengthGoal(exercise: String, unit: String): Result<MetricGoal?> =
        withContext(Dispatchers.IO) { runCatching { api.getStrengthGoal(exercise, unit)?.toStrengthDomain() } }

    override suspend fun createStrengthGoal(exercise: String, target: Double, unit: String): Result<MetricGoal> =
        withContext(Dispatchers.IO) {
            runCatching { api.createStrengthGoal(exercise, CreateStrengthGoalRequestDto(target, unit), tzOffsetMin()).toStrengthDomain() ?: error("Invalid goal") }
        }

    override suspend fun getStrengthGoalHistory(exercise: String, unit: String): Result<List<GoalHistoryEntry>> =
        withContext(Dispatchers.IO) { runCatching { api.getStrengthGoalHistory(exercise, unit).mapNotNull { it.toDomain() } } }

    override suspend fun deleteStrengthGoal(goalId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteStrengthGoal(goalId) } }
}
