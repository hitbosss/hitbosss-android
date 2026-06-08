package com.hitbosss.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.hitbosss.domain.model.AuthUser
import com.hitbosss.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    override fun currentUser(): AuthUser? = runCatching { auth.currentUser?.toAuthUser() }.getOrNull()

    override suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = Tasks.await(auth.signInWithEmailAndPassword(email.trim(), password))
                result.user?.toAuthUser() ?: error("Firebase no devolvió usuario")
            }
        }

    override suspend fun createUserWithEmailPassword(email: String, password: String): Result<AuthUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = Tasks.await(auth.createUserWithEmailAndPassword(email.trim(), password))
                result.user?.toAuthUser() ?: error("Firebase no devolvió usuario")
            }
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(auth.sendPasswordResetEmail(email.trim()))
                Unit
            }
        }

    override suspend fun signInWithGoogle(googleIdToken: String): Result<AuthUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val result = Tasks.await(auth.signInWithCredential(credential))
                result.user?.toAuthUser() ?: error("Firebase no devolvió usuario")
            }
        }

    override fun signOut() {
        runCatching { auth.signOut() }
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
)
