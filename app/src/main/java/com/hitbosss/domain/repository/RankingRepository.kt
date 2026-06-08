package com.hitbosss.domain.repository

import com.hitbosss.domain.model.SportRanking

interface RankingRepository {
    /** unit: "metric" | "imperial" | null (usa el del usuario). */
    suspend fun getRanking(sport: String, unit: String? = null): Result<SportRanking>
}
