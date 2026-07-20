package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.UserProfile
import com.hitbosss.domain.repository.UserRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userId: String): Result<UserProfile> = repository.getUserProfile(userId)
}

/** Denuncia el perfil de otro usuario (comment opcional, máx. 1000). */
class ReportUserUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userId: String, comment: String?): Result<Unit> =
        repository.reportUser(userId, comment)
}
