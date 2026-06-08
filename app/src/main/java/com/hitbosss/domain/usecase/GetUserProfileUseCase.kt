package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.UserProfile
import com.hitbosss.domain.repository.UserRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userId: String): Result<UserProfile> = repository.getUserProfile(userId)
}
