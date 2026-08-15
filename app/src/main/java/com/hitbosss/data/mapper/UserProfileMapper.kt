package com.hitbosss.data.mapper

import com.hitbosss.data.remote.dto.ParticipationDto
import com.hitbosss.data.remote.dto.PersonalInfoDto
import com.hitbosss.data.remote.dto.ProfileEventDto
import com.hitbosss.data.remote.dto.ProfileGroupDto
import com.hitbosss.data.remote.dto.ProfileHitDto
import com.hitbosss.data.remote.dto.SocialNetworkDto
import com.hitbosss.data.remote.dto.UserProfileDto
import com.hitbosss.domain.model.Measurement
import com.hitbosss.domain.model.Participation
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.model.SocialNetwork
import com.hitbosss.domain.model.UserProfile
import androidx.compose.foundation.layout.height

fun UserProfileDto.toDomain() = UserProfile(
    id = id.orEmpty(),
    fullName = fullName.orEmpty(),
    username = username.orEmpty(),
    description = description,
    profilePicUrl = profilePic,
    coverPicUrl = coverPic,
    countryCode = countryCode,
    measurementSystem = measurementSystem,
    socialNetworks = socialNetworks.mapNotNull { it.toDomain() },
    // Orden cronológico (más reciente primero) como iOS; el backend los devuelve por performed_at
    // (el segundo del vídeo), que no es una fecha.
    participations = participations.map { it.toDomain() }.sortedByDescending { it.createdAt },
    groups = groups.map { it.toDomain() },
    events = events.map { it.toDomain() },
)

private fun ProfileGroupDto.toDomain() = com.hitbosss.domain.model.ProfileGroup(
    groupId = groupId ?: 0,
    name = name.orEmpty(),
    coverImageUrl = coverImageUrl,
    officialSports = officialSports,
    userHits = userHits.map { it.toDomain() },
)

private fun ProfileEventDto.toDomain() = com.hitbosss.domain.model.ProfileEvent(
    eventId = eventId ?: 0,
    name = name.orEmpty(),
    sport = sport.orEmpty(),
    finalizedAt = finalizedAt ?: 0L,
    finalPosition = finalPosition,
    totalLevel = totalLevel,
    userHits = userHits.map { it.toDomain() },
)

private fun ProfileHitDto.toDomain() = com.hitbosss.domain.model.ProfileHit(
    hitId = hitId,
    exercise = exercise.orEmpty(),
    sport = sport.orEmpty(),
    maxLift = maxLift?.let { m -> if (m.value != null && m.unit != null) Measurement(m.value, m.unit) else null },
    videoUrl = videoUrl,
    levelWeight = levelWeight,
    performedAt = performedAt ?: 0.0,
    createdAt = createdAt ?: 0L,
    position = position,
    wilksScore = wilksScore,
)

fun PersonalInfoDto.toDomain() = PersonalInfo(
    gender = gender.orEmpty(),
    weight = weight?.takeIf { it.value != null && it.unit != null }
        ?.let { Measurement(it.value!!, it.unit!!) }
        ?: Measurement(0.0, "kg"),
    measurementSystem = measurementSystem.orEmpty(),
    fullName = fullName.orEmpty(),
    username = username.orEmpty(),
    description = description.orEmpty(),
    profilePicUrl = profilePic,
    coverPicUrl = coverPic,
    birthDate = birthDate ?: 0L,
    countryCode = countryCode.orEmpty(),
    height = height?.takeIf { it.value != null && it.unit != null }
        ?.let { Measurement(it.value!!, it.unit!!) }
        ?: Measurement(0.0, "cm"),
    socialNetworks = socialNetworks.mapNotNull { it.toDomain() },
)

private fun SocialNetworkDto.toDomain(): SocialNetwork? {
    if (name == null || url == null) return null
    return SocialNetwork(name = name, url = url, username = username.orEmpty())
}

fun ParticipationDto.toDomain() = Participation(
    exercise = exercise.orEmpty(),
    sport = sport.orEmpty(),
    context = context.orEmpty(),
    hitId = hitId,
    maxLift = maxLift?.let { m ->
        if (m.value != null && m.unit != null) Measurement(m.value, m.unit) else null
    },
    performedAt = performedAt ?: 0.0,
    createdAt = createdAt ?: 0L,
    videoUrl = videoUrl,
    wilksScore = wilksScore,
    levelWeight = levelWeight,
    levelWilks = levelWilks,
    position = position,
)
