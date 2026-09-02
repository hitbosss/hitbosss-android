package com.hitbosss.core.upload

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.hitbosss.R

/**
 * Notificación custom de subida de HIT (RemoteViews) 1:1 con el diseño de Figma: fila ejercicio + peso,
 * fila icono/estado/%, barra de progreso rosa o verde, mensaje y acción. Un único builder para los 5
 * estados (subiendo, buscando conexión, subido, cancelado, error) para no duplicar el poblado de la vista.
 */
object UploadNotification {

    enum class Bar { PinkProgress, Green, None }

    @Suppress("LongParameterList")
    fun build(
        context: Context,
        smallIcon: Int,
        title: String,
        weight: String?,
        icon: Int,
        iconTint: Int,
        status: String,
        percent: String?,
        bar: Bar,
        progress: Int = 0,
        message: String? = null,
        actionLabel: String? = null,
        actionIntent: PendingIntent? = null,
        contentIntent: PendingIntent? = null,
        ongoing: Boolean,
    ): Notification {
        val rv = RemoteViews(context.packageName, R.layout.notification_upload).apply {
            setTextViewText(R.id.np_title, title)
            setText(R.id.np_weight, weight)
            setImageViewResource(R.id.np_icon, icon)
            setInt(R.id.np_icon, "setColorFilter", iconTint)
            setTextViewText(R.id.np_status, status)
            setText(R.id.np_percent, percent)
            when (bar) {
                Bar.PinkProgress -> {
                    setViewVisibility(R.id.np_bar_pink, View.VISIBLE)
                    setProgressBar(R.id.np_bar_pink, 100, progress, false)
                    setViewVisibility(R.id.np_bar_green, View.GONE)
                }
                Bar.Green -> {
                    setViewVisibility(R.id.np_bar_green, View.VISIBLE)
                    setViewVisibility(R.id.np_bar_pink, View.GONE)
                }
                Bar.None -> {
                    setViewVisibility(R.id.np_bar_pink, View.GONE)
                    setViewVisibility(R.id.np_bar_green, View.GONE)
                }
            }
            setText(R.id.np_message, message)
            setText(R.id.np_action, actionLabel)
            if (actionLabel != null && actionIntent != null) setOnClickPendingIntent(R.id.np_action, actionIntent)
        }
        return NotificationCompat.Builder(context, HitUploadService.CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setColor(HitUploadManager.BRAND_COLOR)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(rv)
            .setCustomBigContentView(rv)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(if (ongoing) NotificationCompat.CATEGORY_PROGRESS else NotificationCompat.CATEGORY_STATUS)
            .apply { contentIntent?.let(::setContentIntent) }
            .build()
    }

    /** Rellena un TextView y lo oculta si el texto es null (evita repetir el visibility en cada campo). */
    private fun RemoteViews.setText(viewId: Int, text: CharSequence?) {
        setViewVisibility(viewId, if (text == null) View.GONE else View.VISIBLE)
        if (text != null) setTextViewText(viewId, text)
    }
}
