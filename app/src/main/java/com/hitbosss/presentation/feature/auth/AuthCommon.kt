package com.hitbosss.presentation.feature.auth

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Destino de navegación tras autenticar (equivale a Screen .mainTab / .completeProfile de iOS). */
sealed interface AuthNavEvent {
    data object ToMain : AuthNavEvent
    data object ToCompleteProfile : AuthNavEvent
}

/** Traduce las excepciones de Firebase Auth a mensajes para el usuario. */
fun Throwable.toAuthMessage(): String = when (this) {
    is FirebaseAuthInvalidCredentialsException -> "Email o contraseña incorrectos."
    is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese email."
    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese email."
    is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil (mínimo 6 caracteres)."
    else -> localizedMessage ?: "Ha ocurrido un error. Inténtalo de nuevo."
}
