package com.hitbosss.presentation.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hitbosss.R
import androidx.compose.foundation.layout.size

/** Familia Inter, portada de la app iOS (Inter24pt Regular / SemiBold / Italic). */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
)

private fun inter(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    italic: Boolean = false,
    tracking: Float = 0f,
    decoration: TextDecoration? = null,
) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.em,
    textDecoration = decoration,
)

/**
 * Escala tipográfica equivalente a InterFont.swift (mismos tamaños, line-heights y tracking).
 * Uso: Text(text, style = HitbosssType.titleScreen)
 */
object HitbosssType {
    // Headings
    val titleScreen = inter(30, 36, FontWeight.SemiBold, tracking = -0.02f)
    val titleSection = inter(26, 32, FontWeight.SemiBold, tracking = -0.015f)
    val titleSubsection = inter(22, 28, FontWeight.SemiBold, tracking = -0.01f)
    val titleBody = inter(18, 24, FontWeight.SemiBold, tracking = -0.005f)
    val titleGroup = inter(14, 20, FontWeight.SemiBold)

    // Body Large (16)
    val bodyLargeEmphasis = inter(16, 24, FontWeight.SemiBold)
    val bodyLargeRegular = inter(16, 24, FontWeight.Normal)
    val bodyLargeItalic = inter(16, 24, FontWeight.Normal, italic = true)
    val bodyLargeStrikethrough = inter(16, 24, FontWeight.Normal, decoration = TextDecoration.LineThrough)
    val bodyLargeLink = inter(16, 24, FontWeight.SemiBold, decoration = TextDecoration.Underline)

    // Body Default (14)
    val bodyDefaultEmphasis = inter(14, 20, FontWeight.SemiBold)
    val bodyDefaultRegular = inter(14, 20, FontWeight.Normal)
    val bodyDefaultItalic = inter(14, 20, FontWeight.Normal, italic = true)
    val bodyDefaultStrikethrough = inter(14, 20, FontWeight.Normal, decoration = TextDecoration.LineThrough)
    val bodyDefaultLink = inter(14, 20, FontWeight.SemiBold, decoration = TextDecoration.Underline)

    // Body Small (12)
    val bodySmallEmphasis = inter(12, 16, FontWeight.SemiBold,)
    val bodySmallRegular = inter(12, 16, FontWeight.Normal)
    val bodySmallItalic = inter(12, 16, FontWeight.Normal, italic = true)
    val bodySmallStrikethrough = inter(12, 16, FontWeight.Normal, decoration = TextDecoration.LineThrough)
    val bodySmallLink = inter(12, 16, FontWeight.SemiBold, decoration = TextDecoration.Underline)
}
