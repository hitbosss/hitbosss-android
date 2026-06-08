package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.SportRanking
import com.hitbosss.domain.repository.RankingRepository
import javax.inject.Inject

/** Equivalente a GetRankingUseCase de iOS. */
class GetRankingUseCase @Inject constructor(
    private val repository: RankingRepository,
) {
    suspend operator fun invoke(sport: String, unit: String? = null): Result<SportRanking> =
        repository.getRanking(sport, unit)
}
