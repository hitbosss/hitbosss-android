package com.hitbosss.data.repository

import com.hitbosss.data.mapper.toDomain
import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.model.EventsResult
import com.hitbosss.domain.model.GroupDetail
import com.hitbosss.domain.model.GroupsResult
import com.hitbosss.domain.repository.CommunityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class CommunityRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : CommunityRepository {

    override suspend fun getUserGroups(userId: String): Result<GroupsResult> =
        withContext(Dispatchers.IO) { runCatching { api.getUserGroups(userId).toDomain() } }

    override suspend fun getUserEvents(userId: String): Result<EventsResult> =
        withContext(Dispatchers.IO) { runCatching { api.getUserEvents(userId).toDomain() } }

    override suspend fun getGroup(groupId: Int): Result<GroupDetail> =
        withContext(Dispatchers.IO) { runCatching { api.getGroup(groupId).toDomain() } }

    override suspend fun getEvent(eventId: Int): Result<EventDetail> =
        withContext(Dispatchers.IO) { runCatching { api.getEvent(eventId).toDomain() } }

    override suspend fun leaveGroup(groupId: Int): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.leaveGroup(groupId); Unit } }

    override suspend fun deleteGroup(groupId: Int): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteGroup(groupId); Unit } }

    override suspend fun leaveEvent(eventId: Int): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.leaveEvent(eventId); Unit } }

    override suspend fun deleteEvent(eventId: Int): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteEvent(eventId); Unit } }

    override suspend fun reportGroup(groupId: Int, comment: String?): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.reportGroup(groupId, com.hitbosss.data.remote.dto.ReportRequestDto(comment?.ifBlank { null })); Unit } }

    override suspend fun reportEvent(eventId: Int, comment: String?): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.reportEvent(eventId, com.hitbosss.data.remote.dto.ReportRequestDto(comment?.ifBlank { null })); Unit } }

    override suspend fun resetEventHit(eventId: Int, hitId: Int): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.resetEventHit(eventId, hitId); Unit } }

    override suspend fun makeGroupAdmin(groupId: Int, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.makeGroupAdmin(groupId, userId); Unit } }

    override suspend fun removeGroupMember(groupId: Int, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.removeGroupMember(groupId, userId); Unit } }

    override suspend fun makeEventAdmin(eventId: Int, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.makeEventAdmin(eventId, userId); Unit } }

    override suspend fun removeEventMember(eventId: Int, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.removeEventMember(eventId, userId); Unit } }

    override suspend fun createGroup(fields: Map<String, String>, coverPic: java.io.File?): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.createGroup(fields.toParts(), coverPart(coverPic)); Unit } }

    override suspend fun createEvent(fields: Map<String, String>, coverPic: java.io.File?): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.createEvent(fields.toParts(), coverPart(coverPic)); Unit } }

    override suspend fun updateGroup(groupId: Int, fields: Map<String, String>, coverPic: java.io.File?): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.updateGroup(groupId, fields.toParts(), coverPart(coverPic)); Unit } }

    override suspend fun updateEvent(eventId: Int, fields: Map<String, String>, coverPic: java.io.File?): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.updateEvent(eventId, fields.toParts(), coverPart(coverPic)); Unit } }

    private fun coverPart(file: java.io.File?): okhttp3.MultipartBody.Part? = file?.let {
        okhttp3.MultipartBody.Part.createFormData(
            "coverPic", "cover.jpg",
            it.asRequestBody("image/jpeg".toMediaTypeOrNull()),
        )
    }

    private fun Map<String, String>.toParts() =
        mapValues { (_, v) -> v.toRequestBody("text/plain".toMediaTypeOrNull()) }
}
