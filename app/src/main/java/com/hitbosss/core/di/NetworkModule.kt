package com.hitbosss.core.di

import com.hitbosss.BuildConfig
import com.hitbosss.core.network.AuthInterceptor
import com.hitbosss.core.network.Environment
import com.hitbosss.core.network.FirebaseTokenProvider
import com.hitbosss.core.network.TokenProvider
import com.hitbosss.data.remote.HitbosssApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = FirebaseTokenProvider()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // HEADERS (no BODY): loguear el cuerpo lee el RequestBody entero, lo que en la subida del
            // vídeo provocaba que se "escribiera" dos veces (log + envío real) → el % saltaba 100→0→sube.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            // Timeouts pensados para subir vídeos por redes lentas: los 10s por defecto de
            // write/read abortaban la subida entera al primer parón (SocketTimeoutException)
            // aunque la conexión siguiera viva. Son límites por operación de I/O (no totales):
            // una subida sana de minutos no se corta; una red muerta falla en ≤60s → HITS guardados.
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .writeTimeout(java.time.Duration.ofSeconds(60))
            .readTimeout(java.time.Duration.ofSeconds(60))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(Environment.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideHitbosssApi(retrofit: Retrofit): HitbosssApi = retrofit.create(HitbosssApi::class.java)
}
