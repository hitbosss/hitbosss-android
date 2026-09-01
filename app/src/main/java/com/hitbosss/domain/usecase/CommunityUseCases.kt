package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.model.EventsResult
import com.hitbosss.domain.model.GroupDetail
import com.hitbosss.domain.model.GroupsResult
import com.hitbosss.domain.repository.CommunityRepository
import javax.inject.Inject

class UpdateGroupUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(groupId: Int, params: CreateGroupParams): Result<Unit> = repository.updateGroup(
        groupId,
        mapOf(
            "name" to params.name,
            "motto" to params.motto,
            "description" to params.description,
            "exercises" to params.exercises.joinToString(","),
            "officialSports" to params.officialSports.joinToString(","),
        ),
        params.coverPic,
    )
}

class UpdateEventUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    /** El backend de PATCH evento usa startDate/endDate (no startTime/endTime) y no edita ejercicios. */
    suspend operator fun invoke(
        eventId: Int,
        name: String,
        description: String,
        startTime: Long,
        endTime: Long,
        coverPic: java.io.File? = null,
    ): Result<Unit> = repository.updateEvent(
        eventId,
        mapOf(
            "name" to name,
            "description" to description,
            "startDate" to startTime.toString(),
            "endDate" to endTime.toString(),
        ),
        coverPic,
    )
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
