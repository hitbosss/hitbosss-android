package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreatorDto(
    val id: String? = null,
    val username: String? = null,
    val profilePic: String? = null,
)

@Serializable
data class MemberDto(
    val userId: String? = null,
    val isAdmin: Boolean? = null,
    val joinedAt: Long? = null,
    val username: String? = null,
    val profilePic: String? = null,
)

@Serializable
data class GroupDetailDto(
    val id: Int? = null,
    val name: String? = null,
    val motto: String? = null,
    val description: String? = null,
    val createdBy: CreatorDto? = null,
    val isPublic: Boolean? = null,
    val coverImageUrl: String? = null,
    val stats: CommunityStatsDto? = null,
    val exercises: List<String> = emptyList(),
    val officialSports: List<String> = emptyList(),
    val members: List<MemberDto> = emptyList(),
    val powerlifting: SportRankingDto? = null,
    val crossfit: SportRankingDto? = null,
)

@Serializable
data class EventDetailDto(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val sport: String? = null,
    val hasOfficial: Boolean? = null,
    val createdBy: CreatorDto? = null,
    val isPublic: Boolean? = null,
    val coverImageUrl: String? = null,
    val stats: CommunityStatsDto? = null,
    val exercises: List<String> = emptyList(),
    val members: List<MemberDto> = emptyList(),
    val startTime: Long? = null,
    val endTime: Long? = null,
    val powerlifting: SportRankingDto? = null,
    val crossfit: SportRankingDto? = null,
)
