package com.hitbosss.domain.repository

import com.hitbosss.domain.model.CreateUserData
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.model.UserProfile
import java.io.File

interface UserRepository {
    suspend fun getUserProfile(userId: String): Result<UserProfile>
    suspend fun getPersonalInfo(userId: String): Result<PersonalInfo>
    suspend fun createUser(userId: String, data: CreateUserData): Result<Unit>
    suspend fun deleteAccount(userId: String): Result<Unit>

    /** Actualiza el perfil (campos editados + fotos opcionales). */
    suspend fun updateProfile(
        userId: String,
        fields: Map<String, String>,
        profilePic: File?,
        coverPic: File?,
    ): Result<Unit>
}
