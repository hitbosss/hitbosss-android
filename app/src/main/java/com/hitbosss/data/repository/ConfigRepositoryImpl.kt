package com.hitbosss.data.repository

import com.hitbosss.data.remote.HitbosssApi
import com.hitbosss.domain.model.AppConfig
import com.hitbosss.domain.repository.ConfigRepository
import javax.inject.Inject

class ConfigRepositoryImpl @Inject constructor(
    private val api: HitbosssApi,
) : ConfigRepository {
    override suspend fun getConfig(): Result<AppConfig> =
        runCatching { AppConfig(minimumVersion = api.getConfig().minimumVersion ?: "1.0.0") }
}
