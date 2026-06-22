package com.hitbosss.core.deeplink

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cola del deeplink entrante (hitbosss://group/{id} | hitbosss://event/{id}).
 * MainActivity publica el Uri del intent; el destino MAIN lo consume y navega cuando el usuario
 * ya está autenticado (equivale a procesar el universal link tras el launch en iOS).
 */
object DeepLinkBus {
    private val _pending = MutableStateFlow<Uri?>(null)
    val pending: StateFlow<Uri?> = _pending.asStateFlow()

    fun post(uri: Uri?) {
        if (uri != null && uri.scheme == "hitbosss") _pending.value = uri
    }

    fun consume() {
        _pending.value = null
    }
}
