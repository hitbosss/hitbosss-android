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

    override suspend fun createGroup(fields: Map<String, String>, coverPic: java.io.File?): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.createGroup(fields.toParts(), coverPart(coverPic)); Unit } }

    override suspend fun createEvent(fields: Map<String, String>, coverPic: java.io.File?): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.createEvent(fields.toParts(), coverPart(coverPic)); Unit } }

    private fun coverPart(file: java.io.File?): okhttp3.MultipartBody.Part? = file?.let {
        okhttp3.MultipartBody.Part.createFormData(
            "coverPic", "cover.jpg",
            it.asRequestBody("image/jpeg".toMediaTypeOrNull()),
        )
    }

    private fun Map<String, String>.toParts() =
        mapValues { (_, v) -> v.toRequestBody("text/plain".toMediaTypeOrNull()) }
}
