package com.hitbosss.data.repository

import com.hitbosss.data.mapper.toDomain
import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.data.remote.dto.HitVisibilityRequestDto
import com.hitbosss.data.remote.dto.ReportRequestDto
import com.hitbosss.domain.model.Participation
import com.hitbosss.domain.repository.HitRepository
import com.hitbosss.domain.repository.UploadHitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import okhttp3.RequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer

/** RequestBody que reporta el progreso de subida (bytes escritos / total) — para el % de "Subiendo HIT". */
private class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()
    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        val counting = object : ForwardingSink(sink as Sink) {
            private var written = 0L
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
            }
        }
        val buffered = counting.buffer()
        delegate.writeTo(buffered)
        buffered.flush()
    }
}

class HitRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : HitRepository {

    override suspend fun uploadHit(
        videoFile: File,
        fields: Map<String, String>,
        onProgress: ((Float) -> Unit)?,
    ): Result<UploadHitResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawBody = videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
                val videoBody = if (onProgress != null) CountingRequestBody(rawBody, onProgress) else rawBody
                val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, videoBody)
                val textType = "text/plain".toMediaTypeOrNull()
                val parts = fields.mapValues { (_, v) -> v.toRequestBody(textType) }

                val response = api.uploadHit(videoPart, parts)
                UploadHitResult(
                    hitId = response.hitId ?: 0,
                    videoUrl = response.videoUrl.orEmpty(),
                )
            }
        }

    override suspend fun reportHit(hitId: Int, comment: String?): Result<Unit> =
        runCatching { api.reportHit(hitId, ReportRequestDto(comment?.takeIf { it.isNotBlank() })); Unit }

    override suspend fun deleteHit(hitId: Int): Result<Unit> =
        runCatching { api.deleteHit(hitId); Unit }

    override suspend fun toggleHitVisibility(hitId: Int, hidden: Boolean): Result<Unit> =
        runCatching { api.toggleHitVisibility(hitId, HitVisibilityRequestDto(hidden)); Unit }

    override suspend fun getHiddenHits(unit: String): Result<List<Participation>> =
        runCatching { api.getHiddenHits(unit).hits.orEmpty().map { it.toDomain() } }

    override suspend fun editHit(
        hitId: Int,
        performedAt: Double,
        videoFile: File?,
        onProgress: ((Float) -> Unit)?,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val textType = "text/plain".toMediaTypeOrNull()
                // performedAt como entero de segundos (igual que iOS Int(performedAt)).
                val fields = mapOf("performedAt" to performedAt.toInt().toString().toRequestBody(textType))
                val videoPart = videoFile?.let { file ->
                    val rawBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                    val body = if (onProgress != null) CountingRequestBody(rawBody, onProgress) else rawBody
                    MultipartBody.Part.createFormData("video", file.name, body)
                }
                api.editHit(hitId, fields, videoPart)
                Unit
            }
        }
}
