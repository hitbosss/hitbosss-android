package com.hitbosss.core.upload

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.app.NotificationCompat
import com.hitbosss.R
import com.hitbosss.core.RefreshCoordinator
import com.hitbosss.data.local.SavedHitStore
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.SavedHit
import com.hitbosss.domain.usecase.UploadHitParams
import com.hitbosss.domain.usecase.UploadHitUseCase
import com.hitbosss.presentation.feature.ranking.titleRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subida de HIT global, fuera del ciclo de vida de las pantallas (equivale al
 * HitUploadActivityManager + URLSession.background de iOS #649): foreground service con
 * notificación de progreso, cancelación y — como en el diseño de la "notificación contraída" —
 * fase "Buscando conexión" con auto-reintento cuando vuelve la red (seguro gracias a la clave
 * de idempotencia). El vídeo se persiste como SavedHit ANTES de tocar la red.
 * Solo 1 subida a la vez (igual que iOS).
 */
@Singleton
class HitUploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadHitUseCase: UploadHitUseCase,
    private val store: SavedHitStore,
    private val refreshCoordinator: RefreshCoordinator,
) {
    enum class Phase { Uploading, WaitingConnection }

    data class UploadUi(
        val inProgress: Boolean = false,
        val progress: Float = 0f,
        val phase: Phase = Phase.Uploading,
        val exerciseTitle: String? = null,  // "Press banca" (localizado)
        val liftLabel: String? = null,      // "150 KG" / "330 LBS"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var canceled = false
    private var currentHit: SavedHit? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _state = MutableStateFlow(UploadUi())
    val state: StateFlow<UploadUi> = _state.asStateFlow()

    /**
     * Sube un SavedHit ya persistido (status=uploading, vídeo copiado al store).
     * Devuelve false si ya hay una subida en curso (se bloquea, no se encola — igual que iOS).
     */
    fun start(hit: SavedHit): Boolean {
        if (_state.value.inProgress) return false
        canceled = false
        currentHit = hit
        _state.value = UploadUi(
            inProgress = true,
            progress = 0f,
            exerciseTitle = exerciseTitle(hit),
            liftLabel = liftLabel(hit),
        )
        runCatching { context.startForegroundService(Intent(context, HitUploadService::class.java)) }
        launchUpload(hit)
        return true
    }

    private fun launchUpload(hit: SavedHit) {
        job = scope.launch {
            var lastPct = -1
            uploadHitUseCase(
                UploadHitParams(
                    userId = hit.userId,
                    sport = hit.sport,
                    exercise = hit.exercise,
                    lift = hit.lift,
                    unit = hit.unit,
                    bodyWeightKg = hit.bodyWeightKg,
                    gender = hit.gender,
                    videoFile = store.videoFile(hit),
                    performedAt = hit.performedAt,
                    contextType = hit.contextType,
                    groupId = hit.groupId,
                    eventId = hit.eventId,
                    clientRequestId = hit.clientRequestId ?: hit.id,
                    onProgress = { p ->
                        val pct = (p * 100).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            _state.update { it.copy(progress = p, phase = Phase.Uploading) }
                        }
                    },
                ),
            ).onSuccess {
                store.delete(hit.id)
                when (hit.contextType) {
                    "group" -> { hit.groupId?.let(refreshCoordinator::invalidateGroup); refreshCoordinator.invalidateProfile() }
                    "event" -> { hit.eventId?.let(refreshCoordinator::invalidateEvent); refreshCoordinator.invalidateProfile() }
                    else -> refreshCoordinator.onHitUploaded()
                }
                notifySuccess(hit)
                finish()
            }.onFailure { e ->
                when {
                    canceled -> Unit // cancel() ya notificó y dejó el estado consistente
                    // Sin red validada: fase "Buscando conexión" + auto-reintento al volver
                    // (la clave de idempotencia hace el reintento seguro aunque el servidor
                    // hubiera llegado a procesar la subida).
                    e is IOException && !isOnline() -> waitForConnection(hit)
                    else -> {
                        store.setStatus(hit.id, SavedHit.STATUS_PENDING)
                        notifyError(hit)
                        finish()
                    }
                }
            }
        }
    }

    /** Cancela la subida en curso (notificación o app). El hit queda en HITS guardados. */
    fun cancel() {
        val hit = currentHit
        canceled = true
        unregisterNetworkCallback()
        job?.cancel()
        scope.launch {
            hit?.let { store.setStatus(it.id, SavedHit.STATUS_PENDING) }
            hit?.let { notifyCanceled(it) }
            finish()
        }
    }

    // --- Fase "Buscando conexión" (diseño: la barra se congela y el texto cambia) ---

    private fun waitForConnection(hit: SavedHit) {
        _state.update { it.copy(phase = Phase.WaitingConnection) }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                unregisterNetworkCallback()
                if (!canceled) launchUpload(hit)
            }
        }
        networkCallback = callback
        runCatching { cm.registerDefaultNetworkCallback(callback) }
            .onFailure {
                // No se pudo registrar el callback: degradar a error normal.
                scope.launch {
                    store.setStatus(hit.id, SavedHit.STATUS_PENDING)
                    notifyError(hit)
                    finish()
                }
            }
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let { cb ->
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            runCatching { cm.unregisterNetworkCallback(cb) }
        }
        networkCallback = null
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun finish() {
        currentHit = null
        _state.value = UploadUi() // idle → el service se para solo
    }

    // --- Notificaciones terminales (la de progreso la gestiona el service) ---

    private fun exerciseTitle(hit: SavedHit): String =
        Exercise.entries.firstOrNull { it.apiValue.equals(hit.exercise, true) }
            ?.let { context.getString(it.titleRes()) } ?: hit.exercise

    private fun liftLabel(hit: SavedHit): String {
        val v = if (hit.lift % 1.0 == 0.0) hit.lift.toInt().toString() else hit.lift.toString()
        return "$v ${hit.unit.uppercase()}"
    }

    private fun notifySuccess(hit: SavedHit) {
        // "Ver HIT" abre la app (el HIT recién subido está el primero en el perfil).
        val openApp = PendingIntent.getActivity(
            context, 1,
            Intent(context, com.hitbosss.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        notify(
            NotificationCompat.Builder(context, HitUploadService.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("${exerciseTitle(hit)} · ${liftLabel(hit)}")
                .setContentText(context.getString(R.string.upload_notif_done))
                .setColor(SUCCESS_COLOR)
                .setContentIntent(openApp)
                .addAction(0, context.getString(R.string.upload_notif_view_hit), openApp)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun notifyCanceled(hit: SavedHit) {
        notify(
            NotificationCompat.Builder(context, HitUploadService.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle(context.getString(R.string.upload_notif_canceled))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.upload_notif_saved_msg)))
                .setColor(BRAND_COLOR)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun notifyError(hit: SavedHit) {
        // "Ir a 'HIT Guardados'": deeplink interno que navega directo a la pantalla.
        val toSaved = PendingIntent.getActivity(
            context, 2,
            Intent(context, com.hitbosss.MainActivity::class.java)
                .setData(android.net.Uri.parse("hitbosss://savedhits")),
            PendingIntent.FLAG_IMMUTABLE,
        )
        notify(
            NotificationCompat.Builder(context, HitUploadService.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.upload_notif_failed))
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.upload_notif_saved_msg)))
                .setColor(BRAND_COLOR)
                .setContentIntent(toSaved)
                .addAction(0, context.getString(R.string.upload_notif_go_saved), toSaved)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun notify(notification: android.app.Notification) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.notify(RESULT_NOTIFICATION_ID, notification) }
    }

    companion object {
        const val RESULT_NOTIFICATION_ID = 2002
        const val BRAND_COLOR = 0xFFD41568.toInt()   // Primary500
        const val SUCCESS_COLOR = 0xFF569628.toInt() // Success500
    }
}
