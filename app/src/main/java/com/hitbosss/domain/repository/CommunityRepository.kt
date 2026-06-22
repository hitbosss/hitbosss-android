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
    suspend fun deleteEvent(eventId: Int): Result<Unit>

    suspend fun reportGroup(groupId: Int, comment: String?): Result<Unit>
    suspend fun reportEvent(eventId: Int, comment: String?): Result<Unit>
    suspend fun resetEventHit(eventId: Int, hitId: Int): Result<Unit>

    suspend fun makeGroupAdmin(groupId: Int, userId: String): Result<Unit>
    suspend fun removeGroupMember(groupId: Int, userId: String): Result<Unit>
    suspend fun makeEventAdmin(eventId: Int, userId: String): Result<Unit>
    suspend fun removeEventMember(eventId: Int, userId: String): Result<Unit>

    suspend fun createGroup(fields: Map<String, String>, coverPic: java.io.File? = null): Result<Unit>
    suspend fun createEvent(fields: Map<String, String>, coverPic: java.io.File? = null): Result<Unit>
    suspend fun updateGroup(groupId: Int, fields: Map<String, String>, coverPic: java.io.File? = null): Result<Unit>
    suspend fun updateEvent(eventId: Int, fields: Map<String, String>, coverPic: java.io.File? = null): Result<Unit>
}
