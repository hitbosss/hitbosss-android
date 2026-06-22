package com.hitbosss.presentation.feature.auth

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import android.content.Context
import com.google.firebase.auth.FirebaseAuthException
import com.hitbosss.R

/** Destino de navegación tras autenticar (equivale a Screen .mainTab / .completeProfile de iOS). */
sealed interface AuthNavEvent {
    data object ToMain : AuthNavEvent
    data object ToCompleteProfile : AuthNavEvent
}

/**
 * Traduce las excepciones de Firebase Auth a mensajes para el usuario (login/registro).
 * 1:1 con iOS AuthRepositoryLive (signIn/signUp); textos localizados (es/en) vía recursos.
 */
fun Throwable.toAuthMessage(context: Context): String = when ((this as? FirebaseAuthException)?.errorCode) {
    "ERROR_USER_NOT_FOUND" -> context.getString(R.string.err_user_not_found)
    "ERROR_WRONG_PASSWORD" -> context.getString(R.string.err_wrong_password)
    "ERROR_INVALID_CREDENTIAL" -> context.getString(R.string.err_invalid_credential)
    "ERROR_INVALID_EMAIL" -> context.getString(R.string.err_invalid_email)
    "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.err_email_in_use)
    "ERROR_WEAK_PASSWORD" -> context.getString(R.string.err_weak_password)
    // Cualquier otro error (incl. errores de Firebase no mapeados): mensaje amigable, sin el texto crudo.
    else -> context.getString(R.string.common_unexpected_error_msg)
}

/**
 * Mensajes de error específicos del flujo de recuperar contraseña.
 * 1:1 con iOS AuthRepositoryLive.sendPasswordReset.
 */
fun Throwable.toRecoverMessage(context: Context): String = when ((this as? FirebaseAuthException)?.errorCode) {
    "ERROR_INVALID_EMAIL" -> context.getString(R.string.err_invalid_email)
    "ERROR_USER_NOT_FOUND" -> context.getString(R.string.err_recover_no_account)
    "ERROR_MISSING_EMAIL" -> context.getString(R.string.err_recover_missing_email)
    else -> context.getString(R.string.err_recover_send_failed)
}
