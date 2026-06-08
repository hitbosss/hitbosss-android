package com.hitbosss.domain.usecase

import com.hitbosss.domain.model.AuthUser
import com.hitbosss.domain.model.LoginResult
import com.hitbosss.domain.repository.AuthRepository
import com.hitbosss.domain.repository.LoginRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

/** Autentica en Firebase con el ID token de Google. */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(googleIdToken: String): Result<AuthUser> =
        authRepository.signInWithGoogle(googleIdToken)
}

class SignInWithEmailPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> =
        authRepository.signInWithEmailPassword(email, password)
}

class CreateUserWithEmailPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> =
        authRepository.createUserWithEmailPassword(email, password)
}

class SendPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> = authRepository.sendPasswordReset(email)
}

/** Destino tras autenticar (equivale a Screen .mainTab / .completeProfile / .signIn de iOS). */
enum class PostLoginDestination { MAIN, COMPLETE_PROFILE, STAY }

/**
 * Resuelve a dónde ir tras el login: consulta /login y, si la primera respuesta no es MAIN,
 * reintenta una vez tras 1s (consistencia eventual). Si la API falla -> STAY (no atrapar al usuario).
 * Replica handlePostLogin() de SignInViewModel en iOS.
 */
class ResolvePostLoginUseCase @Inject constructor(
    private val checkUserExists: CheckUserExistsUseCase,
) {
    suspend operator fun invoke(): PostLoginDestination {
        val first = check()
        if (first == PostLoginDestination.MAIN) return first
        delay(1_000)
        return check()
    }

    private suspend fun check(): PostLoginDestination = checkUserExists().fold(
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

/** Comprueba si el usuario existe en la DB (GET /login). */
class CheckUserExistsUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
) {
    suspend operator fun invoke(): Result<LoginResult> = loginRepository.checkUserExists()
}

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() = authRepository.signOut()
}
