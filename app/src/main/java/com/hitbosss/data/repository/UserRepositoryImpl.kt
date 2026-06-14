package com.hitbosss.data.repository

import com.hitbosss.data.mapper.toDomain
import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.data.remote.dto.CreateUserRequestDto
import com.hitbosss.domain.model.CreateUserData
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.model.UserProfile
import com.hitbosss.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import androidx.compose.foundation.layout.height

class UserRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : UserRepository {
    override suspend fun getUserProfile(userId: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching { api.getUserProfile(userId).toDomain() }
        }

    override suspend fun getPersonalInfo(userId: String): Result<PersonalInfo> =
        withContext(Dispatchers.IO) {
            runCatching { api.getPersonalInfo(userId).toDomain() }
        }

    override suspend fun createUser(userId: String, data: CreateUserData): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.createUser(
                    userId,
                    CreateUserRequestDto(
                        username = data.username,
                        gender = data.gender,
                        countryCode = data.countryCode,
                        height = data.height,
                        weight = data.weight,
                        birthDate = data.birthDate,
                        unit = data.unit,
                    ),
                )
                Unit
            }
        }

    override suspend fun deleteAccount(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { api.deleteAccount(userId); Unit }
        }

    override suspend fun updateProfile(
        userId: String,
        fields: Map<String, String>,
        profilePic: java.io.File?,
        coverPic: java.io.File?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val textType = "text/plain".toMediaTypeOrNull()
            val parts = fields.mapValues { (_, v) -> v.toRequestBody(textType) }
            val imageType = "image/jpeg".toMediaTypeOrNull()
            val profilePart = profilePic?.let {
                MultipartBody.Part.createFormData("profilePic", "$userId-profile.jpg", it.asRequestBody(imageType))
            }
            val coverPart = coverPic?.let {
                MultipartBody.Part.createFormData("coverPic", "$userId-cover.jpg", it.asRequestBody(imageType))
            }
            api.updateProfile(userId, parts, profilePart, coverPart)
            Unit
        }
    }
}
