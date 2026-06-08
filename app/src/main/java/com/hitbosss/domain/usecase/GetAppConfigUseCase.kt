package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.AppConfig
import com.hitbosss.domain.repository.ConfigRepository
import javax.inject.Inject

class GetAppConfigUseCase @Inject constructor(
    private val repository: ConfigRepository,
) {
    suspend operator fun invoke(): Result<AppConfig> = repository.getConfig()
}
