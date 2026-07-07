package com.hitbosss.domain.repository

import com.hitbosss.domain.model.BodyLog
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.MetricGoals
import com.hitbosss.domain.model.ProgressPhoto
import com.hitbosss.domain.model.StrengthEntry
import com.hitbosss.domain.model.StrengthHistory
import java.io.File

interface MetricsRepository {
    suspend fun getBodyLogs(userId: String, from: Long? = null, to: Long? = null): Result<List<BodyLog>>
    suspend fun addBodyLog(userId: String, weight: Double, bodyFatPct: Double?, muscleMass: Double?, unit: String): Result<BodyLog>
    suspend fun deleteBodyLog(userId: String, logId: Long): Result<Unit>

    suspend fun getGoals(userId: String, type: String, exercise: String? = null): Result<MetricGoals>
    suspend fun addGoal(userId: String, type: String, exercise: String?, target: Double, unit: String): Result<MetricGoal>
    suspend fun deleteGoal(userId: String, goalId: Long): Result<Unit>

    suspend fun getStrengthHistory(userId: String, exercise: String, from: Long? = null, to: Long? = null): Result<StrengthHistory>
    suspend fun addStrengthLog(userId: String, exercise: String, lift: Double, unit: String): Result<StrengthEntry>
    suspend fun deleteStrengthLog(userId: String, logId: Long): Result<Unit>

    suspend fun getProgressPhotos(userId: String): Result<List<ProgressPhoto>>
    suspend fun addProgressPhoto(
        userId: String,
        photo: File,
        takenAt: Long?,
        weight: Double?,
        bodyFatPct: Double?,
        muscleMass: Double?,
        unit: String,
    ): Result<ProgressPhoto>
    suspend fun deleteProgressPhoto(userId: String, photoId: Long): Result<Unit>
}
