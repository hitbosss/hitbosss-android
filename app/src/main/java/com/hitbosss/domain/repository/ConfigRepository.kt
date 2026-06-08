package com.hitbosss.domain.repository

import com.hitbosss.domain.model.AppConfig

interface ConfigRepository {
    suspend fun getConfig(): Result<AppConfig>
}
