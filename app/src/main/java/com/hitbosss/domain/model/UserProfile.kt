package com.hitbosss.domain.model

data class UserProfile(
    val id: String,
    val fullName: String,
    val username: String,
    val description: String?,
    val profilePicUrl: String?,
    val coverPicUrl: String?,
    val countryCode: String?,
    val measurementSystem: String?,
    val socialNetworks: List<SocialNetwork>,
    val participations: List<Participation>,
    val groups: List<ProfileGroup>,
    val events: List<ProfileEvent>,
)

data class ProfileGroup(
    val groupId: Int,
    val name: String,
    val coverImageUrl: String?,
    val officialSports: List<String>,
    val userHits: List<ProfileHit>,
)

data class ProfileEvent(
    val eventId: Int,
    val name: String,
    val sport: String,
    val finalizedAt: Long,
    val finalPosition: Int?,
    val totalLevel: String?,
    val userHits: List<ProfileHit>,
)

data class ProfileHit(
    val hitId: Int?,
    val exercise: String,
    val sport: String,
    val maxLift: Measurement?,
    val videoUrl: String?,
    val levelWeight: String?,
    val performedAt: Double,
    val createdAt: Long,
    val position: Int?,
)

data class SocialNetwork(
    val name: String,
    val url: String,
    val username: String,
)

data class Participation(
    val exercise: String,
    val sport: String,
    val context: String,
    val hitId: Int?,
    val maxLift: Measurement?,
    val performedAt: Double, // segundo del vídeo a buscar (seek)
    val createdAt: Long,     // timestamp del hit -> fecha mostrada
    val videoUrl: String?,
    val wilksScore: Double?,
    val levelWeight: String?,
    val levelWilks: String?,
    val position: Int?,
)
