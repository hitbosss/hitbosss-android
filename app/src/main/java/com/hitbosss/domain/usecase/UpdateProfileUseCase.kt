package com.hitbosss.domain.usecase

import com.hitbosss.domain.repository.UserRepository
import java.io.File
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(
        userId: String,
        fields: Map<String, String>,
        profilePic: File?,
        coverPic: File?,
    ): Result<Unit> = repository.updateProfile(userId, fields, profilePic, coverPic)
}
