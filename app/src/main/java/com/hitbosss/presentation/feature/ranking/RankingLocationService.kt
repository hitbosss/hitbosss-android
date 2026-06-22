package com.hitbosss.presentation.feature.ranking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Lectura puntual de la ubicación del usuario (equivalente al CLLocationManager.location de iOS),
 * para el filtro de ranking "Current". El permiso se comprueba/solicita en la pantalla antes de llamar.
 */
@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? =
    suspendCancellableCoroutine { cont ->
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location -> cont.resume(location) } // puede ser null
            .addOnFailureListener { cont.resume(null) }
    }
