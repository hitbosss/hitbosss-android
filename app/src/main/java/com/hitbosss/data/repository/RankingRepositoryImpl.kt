package com.hitbosss.data.repository

import com.hitbosss.data.mapper.toDomain
import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.domain.model.SportRanking
import com.hitbosss.domain.repository.RankingRepository
import javax.inject.Inject

class RankingRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : RankingRepository {

    override suspend fun getRanking(sport: String, unit: String?): Result<SportRanking> =
        runCatching { api.getRanking(sport, unit).toDomain(sport) }
}
