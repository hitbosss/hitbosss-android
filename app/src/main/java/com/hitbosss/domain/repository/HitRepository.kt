package com.hitbosss.domain.repository

import java.io.File

data class UploadHitResult(
    val hitId: Int,
    val videoUrl: String,
)

interface HitRepository {
    /** Sube un hit: vídeo (fichero) + campos del formulario (multipart). */
    suspend fun uploadHit(videoFile: File, fields: Map<String, String>): Result<UploadHitResult>

    /** Denuncia un hit (comment opcional). */
    suspend fun reportHit(hitId: Int, comment: String?): Result<Unit>
}
