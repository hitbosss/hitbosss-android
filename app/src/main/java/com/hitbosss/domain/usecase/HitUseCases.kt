package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.repository.HitRepository
import com.hitbosss.domain.repository.UploadHitResult
import com.hitbosss.domain.repository.UserRepository
import com.hitbosss.domain.util.WilksCalculator
import java.io.File
import javax.inject.Inject

class GetPersonalInfoUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(userId: String): Result<PersonalInfo> =
        userRepository.getPersonalInfo(userId)
}

data class UploadHitParams(
    val userId: String,
    val sport: String,
    val exercise: String,
    val lift: Double,
    val unit: String,        // "kg" | "lbs"
    val bodyWeightKg: Double,
    val gender: String,
    val videoFile: File,
    val performedAt: Double = 0.0,  // segundo DENTRO del vídeo para el frame de preview (no epoch)
)

class UploadHitUseCase @Inject constructor(
    private val hitRepository: HitRepository,
) {
    suspend operator fun invoke(params: UploadHitParams): Result<UploadHitResult> {
        val liftKg = if (params.unit.equals("lbs", true)) params.lift / WilksCalculator.KG_TO_LBS else params.lift
        val wilks = WilksCalculator.calculate(params.bodyWeightKg, liftKg, params.gender)

        // El backend espera "metric"/"imperial" (no "kg"/"lbs") y convierte el lift según unit.
        val apiUnit = if (params.unit.equals("lbs", true) || params.unit.equals("lb", true)) "imperial" else "metric"
        val fields = mapOf(
            "userId" to params.userId,
            "sport" to params.sport,
            "exercise" to params.exercise,
            "lift" to params.lift.toString(),
            "unit" to apiUnit,
            "wilksScore" to wilks.toString(),
            // performedAt es el segundo del vídeo para el frame de preview (igual que iOS pinnedTime),
            // NO un timestamp epoch (provocaba que iOS hiciese seek fuera del vídeo y no se viera).
            "performedAt" to params.performedAt.toString(),
            "contextType" to "global",
        )
        return hitRepository.uploadHit(params.videoFile, fields)
    }
}

class ReportHitUseCase @Inject constructor(
    private val hitRepository: HitRepository,
) {
    suspend operator fun invoke(hitId: Int, comment: String?): Result<Unit> =
        hitRepository.reportHit(hitId, comment)
}
