package com.hitbosss.data.remote.dto

import kotlinx.serialization.Serializable

/** value/unit, equivalente al Measurement de la API (v2). */
@Serializable
data class MeasurementDto(
    val value: Double? = null,
    val unit: String? = null,
)

@Serializable
data class RankingEntryDto(
    val id: String? = null,
    val hitId: Int? = null,
    val username: String? = null,
    val gender: String? = null,
    // Oficial:
    val totalWilks: Double? = null,
    val totalLift: MeasurementDto? = null,
    // Por ejercicio:
    val maxLift: MeasurementDto? = null,
    val wilksScore: Double? = null,
    val levelWeight: String? = null,
    val levelWilks: String? = null,
    val profilePic: String? = null,
    val countryCode: String? = null,
    val address: AddressDto? = null,
    val videoUrl: String? = null,
    // performedAt = segundo del vídeo donde está el mejor instante del lift (seek), NO una fecha.
    val performedAt: Double? = null,
    // createdAt = timestamp unix de cuándo se hizo el hit (lo que se muestra como fecha).
    val createdAt: Long? = null,
)

@Serializable
data class AddressDto(val country: CountryDto? = null)

@Serializable
data class CountryDto(val code: String? = null)

/** Cada deporte trae el ranking oficial + uno por ejercicio (mapas uid -> entry). */
@Serializable
data class SportRankingDto(
    val officialWilksRanking: Map<String, RankingEntryDto>? = null,
    val officialPointsRanking: Map<String, RankingEntryDto>? = null,  // grupos/eventos
    val squat: Map<String, RankingEntryDto>? = null,
    val benchpress: Map<String, RankingEntryDto>? = null,
    val deadlift: Map<String, RankingEntryDto>? = null,
    val sumoDeadlift: Map<String, RankingEntryDto>? = null,
    val snatch: Map<String, RankingEntryDto>? = null,
    val clean: Map<String, RankingEntryDto>? = null,
    val cleanAndJerk: Map<String, RankingEntryDto>? = null,
) {
    fun mapFor(apiKey: String): Map<String, RankingEntryDto> = when (apiKey) {
        "officialWilksRanking" -> officialWilksRanking ?: officialPointsRanking
        "squat" -> squat
        "benchpress" -> benchpress
        "deadlift" -> deadlift
        "sumoDeadlift" -> sumoDeadlift
        "snatch" -> snatch
        "clean" -> clean
        "cleanAndJerk" -> cleanAndJerk
        else -> null
    }.orEmpty()
}
