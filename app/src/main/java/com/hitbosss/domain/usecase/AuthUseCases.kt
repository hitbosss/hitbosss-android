package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.AuthUser
import com.hitbosss.domain.repository.AuthRepository
import com.hitbosss.domain.repository.LoginRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

/** Destino tras autenticar (equivale a Screen .mainTab / .completeProfile / .signIn de iOS). */
enum class PostLoginDestination { MAIN, COMPLETE_PROFILE, STAY }

/**
 * Resuelve a dónde ir tras el login: consulta /login y, si la primera respuesta no es MAIN,
 * reintenta una vez tras 1s (consistencia eventual). Si la API falla -> STAY (no atrapar al usuario).
 * Replica handlePostLogin() de SignInViewModel en iOS.
 */
class ResolvePostLoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
) {
    suspend operator fun invoke(): PostLoginDestination {
        val first = check()
        if (first == PostLoginDestination.MAIN) return first
        delay(1_000)
        return check()
    }

    private suspend fun check(): PostLoginDestination = loginRepository.checkUserExists().fold(
        onSuccess = { if (it.isUserInApi) PostLoginDestination.MAIN else PostLoginDestination.COMPLETE_PROFILE },
        onFailure = { PostLoginDestination.STAY },
    )
}

/** ¿Hay sesión Firebase activa? */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): AuthUser? = authRepository.currentUser()
}

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() = authRepository.signOut()
}
