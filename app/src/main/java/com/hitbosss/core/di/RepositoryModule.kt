package com.hitbosss.core.di

import com.hitbosss.data.repository.AuthRepositoryImpl
import com.hitbosss.data.repository.CommunityRepositoryImpl
import com.hitbosss.data.repository.ConfigRepositoryImpl
import com.hitbosss.data.repository.HitRepositoryImpl
import com.hitbosss.data.repository.LoginRepositoryImpl
import com.hitbosss.data.repository.MetricsRepositoryImpl
import com.hitbosss.data.repository.RankingRepositoryImpl
import com.hitbosss.data.repository.UserRepositoryImpl
import com.hitbosss.domain.repository.AuthRepository
import com.hitbosss.domain.repository.CommunityRepository
import com.hitbosss.domain.repository.ConfigRepository
import com.hitbosss.domain.repository.HitRepository
import com.hitbosss.domain.repository.LoginRepository
import com.hitbosss.domain.repository.MetricsRepository
import com.hitbosss.domain.repository.RankingRepository
import com.hitbosss.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRankingRepository(impl: RankingRepositoryImpl): RankingRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLoginRepository(impl: LoginRepositoryImpl): LoginRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(impl: CommunityRepositoryImpl): CommunityRepository

    @Binds
    @Singleton
    abstract fun bindHitRepository(impl: HitRepositoryImpl): HitRepository

    @Binds
    @Singleton
    abstract fun bindMetricsRepository(impl: MetricsRepositoryImpl): MetricsRepository
}
