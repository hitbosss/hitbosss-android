package com.hitbosss.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Añade a cada petición:
 *  - X-App-Version: 2   (igual que iOS, activa el formato de respuesta v2)
 *  - Authorization: Bearer <firebase_token>  (si hay sesión)
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("X-App-Version", Environment.API_VERSION)

        tokenProvider.currentToken()?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
