package com.hitbosss.domain.repository

import com.hitbosss.domain.model.AuthUser

interface AuthRepository {
    fun currentUser(): AuthUser?

    suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthUser>

    suspend fun createUserWithEmailPassword(email: String, password: String): Result<AuthUser>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Autentica en Firebase con el ID token de Google obtenido por Credentials Manager. */
    suspend fun signInWithGoogle(googleIdToken: String): Result<AuthUser>

    fun signOut()
}
