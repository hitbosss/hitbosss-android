package com.hitbosss.data.repository

import com.hitbosss.data.mapper.toDomain
import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.data.remote.dto.CreateBodyLogRequestDto
import com.hitbosss.data.remote.dto.CreateGoalRequestDto
import com.hitbosss.data.remote.dto.CreateStrengthLogRequestDto
import com.hitbosss.domain.model.BodyLog
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.MetricGoals
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.model.StrengthEntry
import com.hitbosss.domain.model.StrengthHistory
import com.hitbosss.domain.repository.MetricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : MetricsRepository {

    override suspend fun getBodyLogs(userId: String, from: Long?, to: Long?): Result<List<BodyLog>> =
        withContext(Dispatchers.IO) {
            runCatching { api.getBodyLogs(userId, from, to).mapNotNull { it.toDomain() } }
        }

    override suspend fun addBodyLog(userId: String, weight: Double, bodyFatPct: Double?, muscleMass: Double?, unit: String): Result<BodyLog> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.createBodyLog(userId, CreateBodyLogRequestDto(weight, bodyFatPct, muscleMass, unit = unit)).toDomain()
                    ?: error("Invalid body log response")
            }
        }

    override suspend fun deleteBodyLog(userId: String, logId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteBodyLog(userId, logId) } }

    override suspend fun getGoals(userId: String, type: String, exercise: String?): Result<MetricGoals> =
        withContext(Dispatchers.IO) {
            runCatching { api.getGoals(userId, type, exercise).toDomain() }
        }

    override suspend fun addGoal(userId: String, type: String, exercise: String?, target: Double, unit: String): Result<MetricGoal> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.createGoal(userId, CreateGoalRequestDto(type, exercise, target, unit)).toDomain()
                    ?: error("Invalid goal response")
            }
        }

    override suspend fun deleteGoal(userId: String, goalId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteGoal(userId, goalId) } }

    override suspend fun getStrengthHistory(userId: String, exercise: String, from: Long?, to: Long?): Result<StrengthHistory> =
        withContext(Dispatchers.IO) {
            runCatching { api.getStrengthHistory(userId, exercise, from, to).toDomain() }
        }

    override suspend fun addStrengthLog(userId: String, exercise: String, lift: Double, unit: String): Result<StrengthEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.createStrengthLog(userId, CreateStrengthLogRequestDto(exercise, lift, unit = unit)).toDomain()
                    ?: error("Invalid strength log response")
            }
        }

    override suspend fun deleteStrengthLog(userId: String, logId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteStrengthLog(userId, logId) } }

    override suspend fun getProgressPhotos(userId: String): Result<List<ProgressPhoto>> =
        withContext(Dispatchers.IO) {
            runCatching { api.getProgressPhotos(userId).mapNotNull { it.toDomain() } }
        }

    override suspend fun addProgressPhoto(
        userId: String,
        photo: File,
        takenAt: Long?,
        weight: Double?,
        bodyFatPct: Double?,
        muscleMass: Double?,
        unit: String,
    ): Result<ProgressPhoto> = withContext(Dispatchers.IO) {
        runCatching {
            val textType = "text/plain".toMediaTypeOrNull()
            val fields = buildMap {
                put("unit", unit.toRequestBody(textType))
                takenAt?.let { put("takenAt", it.toString().toRequestBody(textType)) }
                weight?.let { put("weight", it.toString().toRequestBody(textType)) }
                bodyFatPct?.let { put("bodyFatPct", it.toString().toRequestBody(textType)) }
                muscleMass?.let { put("muscleMass", it.toString().toRequestBody(textType)) }
            }
            val photoPart = MultipartBody.Part.createFormData(
                "photo", "progress-${System.currentTimeMillis()}.jpg",
                photo.asRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            api.uploadProgressPhoto(userId, photoPart, fields).toDomain() ?: error("Invalid photo response")
        }
    }

    override suspend fun deleteProgressPhoto(userId: String, photoId: Long): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteProgressPhoto(userId, photoId) } }
}
