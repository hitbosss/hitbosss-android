package com.hitbosss.core.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
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
        val title = listOfNotNull(st.exerciseTitle, st.liftLabel).joinToString(" · ")
            .ifEmpty { getString(R.string.upload_notif_title) }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(
                if (waiting) getString(R.string.upload_notif_waiting)
                else getString(R.string.upload_notif_title) + "  ·  $pct%",
            )
            .setProgress(100, pct, false)
            .setColor(HitUploadManager.BRAND_COLOR)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.common_cancel), cancelIntent)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.upload_channel_name), NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        const val CHANNEL_ID = "hit_upload"
        const val NOTIFICATION_ID = 2001
        const val ACTION_CANCEL = "com.hitbosss.upload.CANCEL"
    }
}
