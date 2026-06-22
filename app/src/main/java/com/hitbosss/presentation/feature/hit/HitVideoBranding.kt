package com.hitbosss.presentation.feature.hit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.feature.ranking.levelStyle

/**
 * Renderiza, sobre un lienzo del tamaño del frame, los gráficos que iOS quema en el vídeo al
 * exportar un HIT (ExerciseVideoExporter): la tarjeta de datos arriba y el outro de marca al final.
 */
object HitVideoBranding {

    private const val NAVY = 0xFF182A40.toInt()

    private fun interBold(c: Context) = ResourcesCompat.getFont(c, R.font.inter_semibold) ?: Typeface.DEFAULT_BOLD
    private fun interRegular(c: Context) = ResourcesCompat.getFont(c, R.font.inter_regular) ?: Typeface.DEFAULT

    /**
     * Tarjeta superior (1:1 con la del visor): nivel + rank arriba; ejercicio/fecha + peso debajo.
     * Devuelve un bitmap del tamaño completo del frame, transparente salvo la tarjeta.
     */
    fun renderHeaderOverlay(context: Context, hit: HitVideoData, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = w / 360f // escala respecto al ancho de referencia (pantalla 360dp)

        val bold = interBold(context)
        val regular = interRegular(context)
        fun paint(sizeDp: Float, tf: Typeface, colorArgb: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizeDp * s; typeface = tf; color = colorArgb
        }

        val gray800 = Gray800.toArgb()
        val gray500 = Gray500.toArgb()

        val outer = 8f * s
        val pad = 12f * s
        val left = outer
        val right = w - outer
        val contentLeft = left + pad
        val contentRight = right - pad

        // Medidas de texto
        val pExercise = paint(18f, bold, gray800)
        val pDate = paint(14f, regular, gray800)
        val pPoints = paint(14f, bold, gray500)
        val pWeight = paint(22f, bold, gray800)
        val pRank = paint(18f, bold, gray800)
        val pLevel = paint(14f, bold, 0)

        fun textH(p: Paint) = p.fontMetrics.let { it.descent - it.ascent }

        val lvl = levelStyle(hit.levelWeight)
        val levelLabel = lvl?.let { context.getString(it.labelRes).uppercase() }
        val pillVPad = 5f * s
        val pillHPad = 10f * s
        val row1H = maxOf(if (levelLabel != null) textH(pLevel) + pillVPad * 2 else 0f, textH(pRank))
        val rowGap = 8f * s
        val row2H = maxOf(textH(pExercise), textH(pWeight))
        // Fila 3: fecha (izq) + POINTS (der), 1:1 con iOS.
        val hasRow3 = hit.dateText.isNotEmpty() || hit.pointsText.isNotEmpty()
        val row3H = if (hasRow3) maxOf(textH(pDate), textH(pPoints)) else 0f
        val row23Gap = 6f * s

        val hasRow1 = levelLabel != null || hit.rankText.isNotEmpty()
        val cardTop = outer
        val cardHeight = pad * 2 + (if (hasRow1) row1H + rowGap else 0f) + row2H + (if (hasRow3) row23Gap + row3H else 0f)
        val cardBottom = cardTop + cardHeight

        // Fondo de tarjeta semitransparente (~72% blanco) para que se vea más el vídeo por debajo.
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB8FFFFFF.toInt() }
        canvas.drawRoundRect(RectF(left, cardTop, right, cardBottom), 12f * s, 12f * s, bgPaint)

        var y = cardTop + pad
        // Fila 1: nivel (izq) + rank (der)
        if (hasRow1) {
            if (levelLabel != null) {
                val tw = pLevel.apply { color = lvl.text.toArgb() }.measureText(levelLabel)
                val pillRect = RectF(contentLeft, y, contentLeft + tw + pillHPad * 2, y + textH(pLevel) + pillVPad * 2)
                canvas.drawRoundRect(pillRect, 6f * s, 6f * s, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lvl.bg.toArgb() })
                canvas.drawText(levelLabel, pillRect.left + pillHPad, pillRect.top + pillVPad - pLevel.fontMetrics.ascent, pLevel)
            }
            if (hit.rankText.isNotEmpty()) {
                val rw = pRank.measureText(hit.rankText)
                canvas.drawText(hit.rankText, contentRight - rw, y + (row1H - textH(pRank)) / 2 - pRank.fontMetrics.ascent, pRank)
            }
            y += row1H + rowGap
        }
        // Fila 2: ejercicio (izq) / peso (der)
        canvas.drawText(hit.exerciseTitle, contentLeft, y + (row2H - textH(pExercise)) / 2 - pExercise.fontMetrics.ascent, pExercise)
        val ww = pWeight.measureText(hit.weightText)
        canvas.drawText(hit.weightText, contentRight - ww, y + (row2H - textH(pWeight)) / 2 - pWeight.fontMetrics.ascent, pWeight)
        // Fila 3: fecha (izq) / POINTS (der)
        if (hasRow3) {
            y += row2H + row23Gap
            if (hit.dateText.isNotEmpty()) {
                canvas.drawText(hit.dateText, contentLeft, y + (row3H - textH(pDate)) / 2 - pDate.fontMetrics.ascent, pDate)
            }
            if (hit.pointsText.isNotEmpty()) {
                val pw = pPoints.measureText(hit.pointsText)
                canvas.drawText(hit.pointsText, contentRight - pw, y + (row3H - textH(pPoints)) / 2 - pPoints.fontMetrics.ascent, pPoints)
            }
        }

        return bmp
    }

    /**
     * Outro de marca (1:1 con iOS): fondo manAndWoman (cover) + logo + eslogan + badge de Google Play,
     * grupo centrado vertical con el orden y paddings de iOS (logo → 20pt → eslogan → 65pt → badge).
     */
    fun renderOutro(context: Context, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fs = w / 390f // figmaScale, igual que iOS

        // Fondo: manAndWoman recortado a cover; si falla, azul marino (fallback de iOS).
        val bg = decode(context, R.drawable.im_outro_bg)
        if (bg != null) drawCover(canvas, bg, w, h) else canvas.drawColor(NAVY)

        val logo = decode(context, R.drawable.im_logo_color_white)
        val logoW = 130f * fs
        val logoH = logo?.let { logoW * it.height / it.width } ?: 0f

        // Dos badges (Google Play + App Store) en fila, a la misma altura. "Casi promo gratis": que
        // se note que está en ambas tiendas.
        val play = decode(context, R.drawable.im_badge_google_play)
        val appStore = decode(context, R.drawable.im_badge_app_store)
        val badgeH = 50f * fs
        fun badgeW(b: Bitmap?) = b?.let { badgeH * it.width / it.height } ?: 0f
        val playW = badgeW(play)
        val appW = badgeW(appStore)
        val badgeGap = 14f * fs
        val badgesW = playW + badgeGap + appW

        val slogan = "DEMUESTRA   |   COMPITE   |   MEJORA"
        val pSlogan = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f * fs; typeface = interBold(context); color = 0xFFFFFFFF.toInt()
        }
        val sloganH = pSlogan.fontMetrics.let { it.descent - it.ascent }

        val padLogoSlogan = 20f * fs
        val padSloganBadge = 55f * fs
        val totalH = logoH + padLogoSlogan + sloganH + padSloganBadge + badgeH
        var y = (h - totalH) / 2f

        val filter = Paint(Paint.FILTER_BITMAP_FLAG)
        if (logo != null) {
            canvas.drawBitmap(logo, null, RectF((w - logoW) / 2f, y, (w + logoW) / 2f, y + logoH), filter)
        }
        y += logoH + padLogoSlogan
        canvas.drawText(slogan, (w - pSlogan.measureText(slogan)) / 2f, y - pSlogan.fontMetrics.ascent, pSlogan)
        y += sloganH + padSloganBadge
        var bx = (w - badgesW) / 2f
        if (play != null) {
            canvas.drawBitmap(play, null, RectF(bx, y, bx + playW, y + badgeH), filter); bx += playW + badgeGap
        }
        if (appStore != null) {
            canvas.drawBitmap(appStore, null, RectF(bx, y, bx + appW, y + badgeH), filter)
        }

        bg?.recycle(); logo?.recycle(); play?.recycle(); appStore?.recycle()
        return bmp
    }

    private fun decode(context: Context, resId: Int): Bitmap? =
        runCatching { BitmapFactory.decodeResource(context.resources, resId) }.getOrNull()

    /** Escala la imagen para cubrir w×h (center-crop). */
    private fun drawCover(canvas: Canvas, src: Bitmap, w: Int, h: Int) {
        val scale = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
        val dw = src.width * scale
        val dh = src.height * scale
        val left = (w - dw) / 2f
        val top = (h - dh) / 2f
        canvas.drawBitmap(src, null, RectF(left, top, left + dw, top + dh), Paint(Paint.FILTER_BITMAP_FLAG))
    }
}
