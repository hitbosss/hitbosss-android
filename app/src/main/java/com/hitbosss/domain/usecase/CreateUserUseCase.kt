package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.CreateUserData
import com.hitbosss.domain.repository.UserRepository
import javax.inject.Inject

class CreateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(userId: String, data: CreateUserData): Result<Unit> =
        userRepository.createUser(userId, data)
}

class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(userId: String): Result<Unit> = userRepository.deleteAccount(userId)
}
