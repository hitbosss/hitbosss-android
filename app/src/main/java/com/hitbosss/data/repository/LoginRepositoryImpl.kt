package com.hitbosss.data.repository

import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.domain.model.LoginResult
import com.hitbosss.domain.repository.LoginRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : LoginRepository {
    override suspend fun checkUserExists(): Result<LoginResult> = withContext(Dispatchers.IO) {
        runCatching {
            val dto = api.checkUserExists()
            LoginResult(
                isUserInApi = dto.isUserInApi ?: false,
                measurementSystem = dto.measurementSystem,
            )
        }
    }
}
