package com.hitbosss.domain.model

data class CommunityStats(
    val memberCount: Int,
    val exerciseCount: Int,
)

data class GroupSummary(
    val id: Int,
    val name: String,
    val description: String?,
    val coverImageUrl: String?,
    val stats: CommunityStats,
)

data class EventSummary(
    val id: Int,
    val name: String,
    val description: String?,
    val coverImageUrl: String?,
    val sport: String,
    val hasOfficial: Boolean,
    val startTime: Long,
    val endTime: Long,
    val stats: CommunityStats,
)

data class GroupsResult(
    val myGroups: List<GroupSummary>,
    val communityGroups: List<GroupSummary>,
)

data class EventsResult(
    val myEvents: List<EventSummary>,
    val communityEvents: List<EventSummary>,
)

data class Creator(
    val id: String,
    val username: String,
    val profilePic: String?,
)

data class Member(
    val userId: String,
    val isAdmin: Boolean,
    val username: String,
    val profilePic: String?,
)

data class GroupDetail(
    val id: Int,
    val name: String,
    val motto: String?,
    val description: String?,
    val coverImageUrl: String?,
    val isPublic: Boolean,
    val stats: CommunityStats,
    val exercises: List<String>,
    val officialSports: List<String>,
    val members: List<Member>,
    val createdBy: Creator?,
    val ranking: Map<String, SportRanking> = emptyMap(),
)

data class EventDetail(
    val id: Int,
    val name: String,
    val description: String?,
    val sport: String,
    val hasOfficial: Boolean,
    val coverImageUrl: String?,
    val stats: CommunityStats,
    val exercises: List<String>,
    val members: List<Member>,
    val startTime: Long,
    val endTime: Long,
    val createdBy: Creator?,
    val ranking: Map<String, SportRanking> = emptyMap(),
)
