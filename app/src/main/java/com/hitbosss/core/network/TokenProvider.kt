package com.hitbosss.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth

/** Proporciona el ID token de Firebase para autenticar peticiones (equivale al token de iOS). */
interface TokenProvider {
    fun currentToken(): String?
}

/**
 * Implementación con Firebase Auth. Devuelve null si no hay usuario o si Firebase
 * aún no está configurado (sin google-services.json) — así los endpoints públicos
 * (config, ranking, deeplink) funcionan antes de cablear el login.
 */
class FirebaseTokenProvider : TokenProvider {
    override fun currentToken(): String? = try {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        Tasks.await(user.getIdToken(false)).token
    } catch (e: Exception) {
        null
    }
}
