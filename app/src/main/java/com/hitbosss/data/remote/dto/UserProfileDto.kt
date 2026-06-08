package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String? = null,
    val fullName: String? = null,
    val username: String? = null,
    val description: String? = null,
    val profilePic: String? = null,
    val coverPic: String? = null,
    val measurementSystem: String? = null,
    val countryCode: String? = null,
    val photos: List<UserPhotoDto> = emptyList(),
    val socialNetworks: List<SocialNetworkDto> = emptyList(),
    val participations: List<ParticipationDto> = emptyList(),
    val groups: List<ProfileGroupDto> = emptyList(),
    val events: List<ProfileEventDto> = emptyList(),
)

@Serializable
data class ProfileGroupDto(
    val groupId: Int? = null,
    val name: String? = null,
    val coverImageUrl: String? = null,
    val officialSports: List<String> = emptyList(),
    val userHits: List<ProfileHitDto> = emptyList(),
)

@Serializable
data class ProfileEventDto(
    val eventId: Int? = null,
    val name: String? = null,
    val sport: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val finalizedAt: Long? = null,
    val finalPosition: Int? = null,
    val totalLevel: String? = null,
    val userHits: List<ProfileHitDto> = emptyList(),
)

/** Hit dentro de un grupo/evento del perfil. */
@Serializable
data class ProfileHitDto(
    val hitId: Int? = null,
    val exercise: String? = null,
    val sport: String? = null,
    val maxLift: MeasurementDto? = null,
    val videoUrl: String? = null,
    val wilksScore: Double? = null,
    val levelWeight: String? = null,
    val levelWilks: String? = null,
    val performedAt: Double? = null,
    val createdAt: Long? = null,
    val position: Int? = null,
)

@Serializable
data class UserPhotoDto(
    val id: Int? = null,
    val photoUrl: String? = null,
    val uploadedAt: Long? = null,
)

@Serializable
data class SocialNetworkDto(
    val name: String? = null,
    val url: String? = null,
    val username: String? = null,
)

@Serializable
data class ParticipationDto(
    val exercise: String? = null,
    val sport: String? = null,
    val context: String? = null,
    val hitId: Int? = null,
    val maxLift: MeasurementDto? = null,
    val performedAt: Double? = null, // segundo del vídeo (seek), no fecha
    val createdAt: Long? = null,     // timestamp del hit -> fecha
    val videoUrl: String? = null,
    val wilksScore: Double? = null,
    val levelWeight: String? = null,
    val levelWilks: String? = null,
    val position: Int? = null,
)
