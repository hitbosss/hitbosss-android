package com.hitbosss.data.mapper

import com.hitbosss.data.remote.dto.CommunityStatsDto
import com.hitbosss.data.remote.dto.CreatorDto
import com.hitbosss.data.remote.dto.EventDetailDto
import com.hitbosss.data.remote.dto.EventListDto
import com.hitbosss.data.remote.dto.EventsResponseDto
import com.hitbosss.data.remote.dto.GroupDetailDto
import com.hitbosss.data.remote.dto.GroupListDto
import com.hitbosss.data.remote.dto.GroupsResponseDto
import com.hitbosss.data.remote.dto.MemberDto
import com.hitbosss.domain.model.CommunityStats
import com.hitbosss.domain.model.Creator
import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.model.EventSummary
import com.hitbosss.domain.model.EventsResult
import com.hitbosss.domain.model.GroupDetail
import com.hitbosss.domain.model.GroupSummary
import com.hitbosss.domain.model.GroupsResult
import com.hitbosss.domain.model.Member

private fun CommunityStatsDto?.toDomain() =
    CommunityStats(memberCount = this?.memberCount ?: 0, exerciseCount = this?.exerciseCount ?: 0)

private fun GroupListDto.toDomain() = GroupSummary(
    id = id ?: 0,
    name = name.orEmpty(),
    description = description,
    coverImageUrl = coverImageUrl,
    stats = stats.toDomain(),
)

private fun EventListDto.toDomain() = EventSummary(
    id = id ?: 0,
    name = name.orEmpty(),
    description = description,
    coverImageUrl = coverImageUrl,
    sport = sport.orEmpty(),
    hasOfficial = hasOfficial ?: false,
    startTime = startTime ?: 0L,
    endTime = endTime ?: 0L,
    stats = stats.toDomain(),
)

fun GroupsResponseDto.toDomain() = GroupsResult(
    myGroups = myGroups.map { it.toDomain() },
    communityGroups = community?.groups.orEmpty().map { it.toDomain() },
)

fun EventsResponseDto.toDomain() = EventsResult(
    myEvents = myEvents.map { it.toDomain() },
    communityEvents = community?.events.orEmpty().map { it.toDomain() },
)

private fun CreatorDto.toDomain() = Creator(id.orEmpty(), username.orEmpty(), profilePic)
private fun MemberDto.toDomain() = Member(userId.orEmpty(), isAdmin ?: false, username.orEmpty(), profilePic)

fun GroupDetailDto.toDomain() = GroupDetail(
    id = id ?: 0,
    name = name.orEmpty(),
    motto = motto,
    description = description,
    coverImageUrl = coverImageUrl,
    isPublic = isPublic ?: false,
    stats = stats.toDomain(),
    exercises = exercises,
    officialSports = officialSports,
    members = members.map { it.toDomain() },
    createdBy = createdBy?.toDomain(),
    ranking = buildMap {
        powerlifting?.let { put("powerlifting", mapOf("powerlifting" to it).toDomain("powerlifting")) }
        crossfit?.let { put("crossfit", mapOf("crossfit" to it).toDomain("crossfit")) }
    },
)

fun EventDetailDto.toDomain() = EventDetail(
    id = id ?: 0,
    name = name.orEmpty(),
    description = description,
    sport = sport.orEmpty(),
    hasOfficial = hasOfficial ?: false,
    coverImageUrl = coverImageUrl,
    stats = stats.toDomain(),
    exercises = exercises,
    members = members.map { it.toDomain() },
    startTime = startTime ?: 0L,
    endTime = endTime ?: 0L,
    createdBy = createdBy?.toDomain(),
    ranking = buildMap {
        powerlifting?.let { put("powerlifting", mapOf("powerlifting" to it).toDomain("powerlifting")) }
        crossfit?.let { put("crossfit", mapOf("crossfit" to it).toDomain("crossfit")) }
    },
)
