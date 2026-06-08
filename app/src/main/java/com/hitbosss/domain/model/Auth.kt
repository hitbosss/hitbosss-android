package com.hitbosss.domain.model

/** Usuario autenticado en Firebase (independiente del registro en la DB). */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

/** Resultado de GET /login: si el usuario existe en la DB y su sistema de medición. */
data class LoginResult(
    val isUserInApi: Boolean,
    val measurementSystem: String?,
)
