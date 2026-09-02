package com.hitbosss.core.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.hitbosss.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service que mantiene viva la subida del HIT y muestra su progreso en una
 * notificación con botón Cancelar (equivalente Android de la Live Activity de iOS #649).
 * Se para solo cuando la subida termina (éxito, fallo o cancelación).
 */
@AndroidEntryPoint
class HitUploadService : Service() {

    @Inject lateinit var manager: HitUploadManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            manager.cancel()
            return START_NOT_STICKY
        }
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        scope.launch {
            var last = ""
            manager.state.collect { st ->
                if (!st.inProgress) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val key = "${(st.progress * 100).toInt()}-${st.phase}"
                    if (key != last) {
                        last = key
                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIFICATION_ID, buildNotification(st))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Notificación de progreso 1:1 con el diseño: ejercicio + peso, estado y barra con %. */
    private fun buildNotification(st: HitUploadManager.UploadUi = manager.state.value): Notification {
        val cancelIntent = PendingIntent.getService(
            this, 0,
            Intent(this, HitUploadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pct = (st.progress * 100).toInt()
        val waiting = st.phase == HitUploadManager.Phase.WaitingConnection
        return UploadNotification.build(
            context = this,
            smallIcon = android.R.drawable.stat_sys_upload,
            title = st.exerciseTitle ?: getString(R.string.upload_notif_title),
            weight = st.liftLabel,
            icon = if (waiting) R.drawable.np_ic_wifi else R.drawable.np_ic_upload,
            iconTint = HitUploadManager.BRAND_COLOR,
            status = getString(if (waiting) R.string.upload_notif_waiting else R.string.upload_notif_title),
            percent = if (waiting) null else "$pct%",
            bar = UploadNotification.Bar.PinkProgress,
            progress = pct,
            actionLabel = getString(R.string.common_cancel),
            actionIntent = cancelIntent,
            ongoing = true,
        )
    }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // La config de un canal (importancia, lockscreenVisibility) no se puede cambiar tras crearlo → se
        // versiona el id y se retiran los anteriores. IMPORTANCE_DEFAULT: LOW lo ocultan del bloqueo algunos
        // OEMs (MIUI/HyperOS); DEFAULT sí se muestra. Sin sonido/vibración para no molestar en cada progreso.
        listOf("hit_upload", "hit_upload_v2").forEach { nm.deleteNotificationChannel(it) }
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.upload_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "hit_upload_v3"
        const val NOTIFICATION_ID = 2001
        const val ACTION_CANCEL = "com.hitbosss.upload.CANCEL"
    }
}
