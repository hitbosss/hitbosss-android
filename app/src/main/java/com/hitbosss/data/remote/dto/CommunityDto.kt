package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommunityStatsDto(
    val memberCount: Int? = null,
    val exerciseCount: Int? = null,
)

@Serializable
data class GroupListDto(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val stats: CommunityStatsDto? = null,
)

@Serializable
data class EventListDto(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val sport: String? = null,
    val hasOfficial: Boolean? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val exercises: List<String> = emptyList(),
    val stats: CommunityStatsDto? = null,
)

@Serializable
data class GroupsResponseDto(
    val myGroups: List<GroupListDto> = emptyList(),
    val community: GroupsCommunityDto? = null,
)

@Serializable
data class GroupsCommunityDto(
    val groups: List<GroupListDto> = emptyList(),
)

@Serializable
data class EventsResponseDto(
    val myEvents: List<EventListDto> = emptyList(),
    val community: EventsCommunityDto? = null,
)

@Serializable
data class EventsCommunityDto(
    val events: List<EventListDto> = emptyList(),
)
