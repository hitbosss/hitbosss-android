package com.hitbosss.domain.model

/** Datos de registro de perfil (POST /users/{id}). height en cm, weight en kg cuando unit=metric. */
data class CreateUserData(
    val username: String,
    val gender: String,
    val countryCode: String,
    val height: Double,
    val weight: Double,
    val birthDate: Long,
    val unit: String,
)
