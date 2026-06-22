package com.hitbosss.data.mapper

import com.hitbosss.data.remote.dto.RankingEntryDto
import com.hitbosss.data.remote.dto.SportRankingDto
import com.hitbosss.domain.model.Measurement
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.RankingEntry
import com.hitbosss.domain.model.Sport
import com.hitbosss.domain.model.SportRanking

/**
 * Convierte la respuesta de /ranking/{sport} a dominio: oficial + cada ejercicio.
 * Cada categoría es un mapa (sin orden); ordenamos por score (wilks) desc y asignamos posición.
 */
fun Map<String, SportRankingDto>.toDomain(sportKey: String): SportRanking {
    val sport = Sport.entries.firstOrNull { it.apiValue == sportKey } ?: Sport.Powerlifting
    val dto = this[sportKey] ?: this.values.firstOrNull()

    val byCategory = RankingCategory.forSport(sport).associateWith { category ->
        dto?.mapFor(category.apiKey).orEmpty()
            .values
            .map { it.toEntry() }
            // iOS ordena por peso levantado (orderBy = .lift), wilks como desempate.
            .sortedWith(compareByDescending<RankingEntry> { it.lift?.value ?: 0.0 }.thenByDescending { it.score })
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }
    return SportRanking(sport = sportKey, byCategory = byCategory)
}

private fun RankingEntryDto.toEntry() = RankingEntry(
    rank = 0,
    hitId = hitId,
    userId = id.orEmpty(),
    username = username.orEmpty(),
    gender = gender,
    isDeleted = isDeleted ?: false,
    score = totalWilks ?: wilksScore ?: 0.0,
    lift = (totalLift ?: maxLift)?.let { m ->
        if (m.value != null && m.unit != null) Measurement(m.value, m.unit) else null
    },
    levelWeight = levelWeight,
    levelWilks = levelWilks,
    birthDate = birthDate ?: 0L,
    profilePicUrl = profilePic,
    countryCode = countryCode ?: address?.country?.code,
    videoUrl = videoUrl,
    performedAt = performedAt ?: 0.0,
    createdAt = createdAt ?: 0L,
    latitude = address?.currentLocation?.latitude,
    longitude = address?.currentLocation?.longitude,
)
