package com.hitbosss.domain.repository

import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.model.EventsResult
import com.hitbosss.domain.model.GroupDetail
import com.hitbosss.domain.model.GroupsResult

interface CommunityRepository {
    suspend fun getUserGroups(userId: String): Result<GroupsResult>
    suspend fun getUserEvents(userId: String): Result<EventsResult>
    suspend fun getGroup(groupId: Int): Result<GroupDetail>
    suspend fun getEvent(eventId: Int): Result<EventDetail>

    suspend fun leaveGroup(groupId: Int): Result<Unit>
    suspend fun deleteGroup(groupId: Int): Result<Unit>
    suspend fun leaveEvent(eventId: Int): Result<Unit>

    suspend fun createGroup(fields: Map<String, String>, coverPic: java.io.File? = null): Result<Unit>
    suspend fun createEvent(fields: Map<String, String>, coverPic: java.io.File? = null): Result<Unit>
}
