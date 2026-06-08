package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.model.EventsResult
import com.hitbosss.domain.model.GroupDetail
import com.hitbosss.domain.model.GroupsResult
import com.hitbosss.domain.repository.CommunityRepository
import javax.inject.Inject

class GetUserGroupsUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(userId: String): Result<GroupsResult> = repository.getUserGroups(userId)
}

class GetUserEventsUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(userId: String): Result<EventsResult> = repository.getUserEvents(userId)
}

class GetGroupUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(groupId: Int): Result<GroupDetail> = repository.getGroup(groupId)
}

class GetEventUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(eventId: Int): Result<EventDetail> = repository.getEvent(eventId)
}

class LeaveGroupUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(groupId: Int): Result<Unit> = repository.leaveGroup(groupId)
}

class DeleteGroupUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(groupId: Int): Result<Unit> = repository.deleteGroup(groupId)
}

class LeaveEventUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(eventId: Int): Result<Unit> = repository.leaveEvent(eventId)
}

data class CreateGroupParams(
    val name: String,
    val motto: String,
    val description: String,
    val isPublic: Boolean,
    val exercises: List<String>,
    val officialSports: List<String>,
    val coverPic: java.io.File? = null,
)

class CreateGroupUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(params: CreateGroupParams): Result<Unit> = repository.createGroup(
        mapOf(
            "name" to params.name,
            "motto" to params.motto,
            "description" to params.description,
            "isPublic" to params.isPublic.toString(),
            "exercises" to params.exercises.joinToString(","),
            "officialSports" to params.officialSports.joinToString(","),
        ),
        params.coverPic,
    )
}

data class CreateEventParams(
    val name: String,
    val description: String,
    val isPublic: Boolean,
    val sport: String,
    val exercises: List<String>,
    val hasOfficial: Boolean,
    val startTime: Long,
    val endTime: Long,
    val coverPic: java.io.File? = null,
)

class CreateEventUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(params: CreateEventParams): Result<Unit> = repository.createEvent(
        mapOf(
            "name" to params.name,
            "description" to params.description,
            "isPublic" to params.isPublic.toString(),
            "sport" to params.sport,
            "exercises" to params.exercises.joinToString(","),
            "hasOfficial" to params.hasOfficial.toString(),
            "startTime" to params.startTime.toString(),
            "endTime" to params.endTime.toString(),
        ),
        params.coverPic,
    )
}
