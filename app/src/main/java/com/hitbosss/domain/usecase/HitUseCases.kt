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
    val onProgress: ((Float) -> Unit)? = null,  // 0..1 de bytes subidos (para el % de "Subiendo HIT")
    val contextType: String = "global",  // "global" | "group" | "event"
    val groupId: Int? = null,
    val eventId: Int? = null,
    // Clave de idempotencia (UUID por intento lógico de subida; los reintentos reutilizan la misma).
    val clientRequestId: String? = null,
)

class UploadHitUseCase @Inject constructor(
    private val hitRepository: HitRepository,
) {
    suspend operator fun invoke(params: UploadHitParams): Result<UploadHitResult> {
        val liftKg = if (params.unit.equals("lbs", true)) params.lift / WilksCalculator.KG_TO_LBS else params.lift
        val wilks = WilksCalculator.calculate(params.bodyWeightKg, liftKg, params.gender)

        // El backend espera "metric"/"imperial" (no "kg"/"lbs") y convierte el lift según unit.
        val apiUnit = if (params.unit.equals("lbs", true) || params.unit.equals("lb", true)) "imperial" else "metric"
        val fields = buildMap {
            put("userId", params.userId)
            put("sport", params.sport)
            put("exercise", params.exercise)
            put("lift", params.lift.toString())
            put("unit", apiUnit)
            put("wilksScore", wilks.toString())
            // performedAt es el segundo del vídeo para el frame de preview (igual que iOS pinnedTime),
            // NO un timestamp epoch (provocaba que iOS hiciese seek fuera del vídeo y no se viera).
            put("performedAt", params.performedAt.toString())
            put("contextType", params.contextType)
            // Solo se envían en su contexto (el backend valida que estén presentes).
            params.groupId?.takeIf { params.contextType == "group" }?.let { put("groupId", it.toString()) }
            params.eventId?.takeIf { params.contextType == "event" }?.let { put("eventId", it.toString()) }
            params.clientRequestId?.let { put("clientRequestId", it) }
        }
        return hitRepository.uploadHit(params.videoFile, fields, params.onProgress)
    }
}

class ReportHitUseCase @Inject constructor(
    private val hitRepository: HitRepository,
) {
    suspend operator fun invoke(hitId: Int, comment: String?): Result<Unit> =
        hitRepository.reportHit(hitId, comment)
}

class DeleteHitUseCase @Inject constructor(
    private val hitRepository: HitRepository,
) {
    suspend operator fun invoke(hitId: Int): Result<Unit> = hitRepository.deleteHit(hitId)
}

class ToggleHitVisibilityUseCase @Inject constructor(
    private val hitRepository: HitRepository,
) {
    suspend operator fun invoke(hitId: Int, hidden: Boolean): Result<Unit> =
        hitRepository.toggleHitVisibility(hitId, hidden)
}

class GetHiddenHitsUseCase @Inject constructor(
    private val hitRepository: HitRepository,
) {
    suspend operator fun invoke(unit: String): Result<List<com.hitbosss.domain.model.Participation>> =
        hitRepository.getHiddenHits(unit)
}

class EditHitUseCase @Inject constructor(
    private val hitRepository: HitRepository,
) {
    /** performedAt = segundo del vídeo (pin). videoFile null => solo se actualiza performedAt. */
    suspend operator fun invoke(
        hitId: Int,
        performedAt: Double,
        videoFile: File?,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<Unit> = hitRepository.editHit(hitId, performedAt, videoFile, onProgress)
}
