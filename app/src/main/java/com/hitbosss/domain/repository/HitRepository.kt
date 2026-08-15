package com.hitbosss.domain.repository

import com.hitbosss.domain.model.Participation
import java.io.File

data class UploadHitResult(
    val hitId: Int,
    val videoUrl: String,
)

interface HitRepository {
    /** Sube un hit: vídeo (fichero) + campos del formulario (multipart). onProgress: 0..1 de bytes subidos. */
    suspend fun uploadHit(
        videoFile: File,
        fields: Map<String, String>,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<UploadHitResult>

    /** Denuncia un hit (comment opcional). */
    suspend fun reportHit(hitId: Int, comment: String?): Result<Unit>

    /** Elimina un hit propio. */
    suspend fun deleteHit(hitId: Int): Result<Unit>

    /** Oculta/muestra un hit propio (reversible; desaparece de ranking/perfil, sigue en su grupo/evento). */
    suspend fun toggleHitVisibility(hitId: Int, hidden: Boolean): Result<Unit>

    /** Lista los hits ocultos del usuario (misma forma que participations). */
    suspend fun getHiddenHits(unit: String): Result<List<Participation>>

    /**
     * Edita un hit propio: nuevo performedAt y, opcionalmente, un vídeo recortado
     * (si videoFile es null solo se actualiza performedAt). onProgress: 0..1 de bytes subidos.
     */
    suspend fun editHit(
        hitId: Int,
        performedAt: Double,
        videoFile: File?,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<Unit>
}
