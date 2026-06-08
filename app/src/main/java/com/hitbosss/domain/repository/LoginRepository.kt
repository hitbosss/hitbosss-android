package com.hitbosss.domain.repository

import com.hitbosss.domain.model.LoginResult

interface LoginRepository {
    /** GET /login con el token Firebase: ¿existe el usuario en la DB? */
    suspend fun checkUserExists(): Result<LoginResult>
}
