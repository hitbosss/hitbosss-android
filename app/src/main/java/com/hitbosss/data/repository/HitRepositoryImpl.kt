package com.hitbosss.data.repository

import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.data.remote.dto.ReportRequestDto
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

class HitRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : HitRepository {

    override suspend fun uploadHit(videoFile: File, fields: Map<String, String>): Result<UploadHitResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val videoBody = videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
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
        withContext(Dispatchers.IO) {
            runCatching { api.reportHit(hitId, ReportRequestDto(comment?.takeIf { it.isNotBlank() })); Unit }
        }
}
