package com.hitbosss.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Añade a cada petición:
 *  - X-App-Version: 2   (igual que iOS, activa el formato de respuesta v2)
 *  - Authorization: Bearer <firebase_token>  (si hay sesión)
 *
 * El token se pide a Firebase aquí mismo: devuelve null si no hay usuario o si Firebase aún no
 * está configurado, así los endpoints públicos (config, ranking, deeplink) siguen funcionando.
 */
class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("X-App-Version", Environment.API_VERSION)

        firebaseToken()?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }

    private fun firebaseToken(): String? = try {
        FirebaseAuth.getInstance().currentUser?.let { Tasks.await(it.getIdToken(false)).token }
    } catch (_: Exception) {
        null
    }
}
