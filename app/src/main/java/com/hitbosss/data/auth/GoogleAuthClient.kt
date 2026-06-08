package com.hitbosss.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Obtiene el ID token de Google con la API moderna (Credentials Manager),
 * que luego se canjea por una sesión Firebase. Requiere `webClientId`
 * (el Web client ID del proyecto Firebase — ver BuildConfig.WEB_CLIENT_ID).
 */
class GoogleAuthClient(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun getGoogleIdToken(webClientId: String): Result<String> = runCatching {
        require(webClientId.isNotBlank()) {
            "WEB_CLIENT_ID vacío: añade google-services.json y el Web client ID (ver MIGRATION.md)"
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(context, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            error("Credencial inesperada de Google")
        }
    }
}
