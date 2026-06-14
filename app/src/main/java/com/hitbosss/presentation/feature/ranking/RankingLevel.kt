package com.hitbosss.presentation.feature.ranking

import androidx.compose.ui.graphics.Color
import com.hitbosss.presentation.designsystem.theme.Aqua100
import com.hitbosss.presentation.designsystem.theme.Aqua500
import com.hitbosss.presentation.designsystem.theme.Brown100
import com.hitbosss.presentation.designsystem.theme.Brown500
import com.hitbosss.presentation.designsystem.theme.LemonGreen100
import com.hitbosss.presentation.designsystem.theme.LemonGreen500
import com.hitbosss.presentation.designsystem.theme.Red100
import com.hitbosss.presentation.designsystem.theme.Red500
import com.hitbosss.presentation.designsystem.theme.SkyBlue100
import com.hitbosss.presentation.designsystem.theme.SkyBlue500
import androidx.annotation.StringRes
import com.hitbosss.R

/** labelRes = nombre del nivel localizado (el badge lo muestra en MAYÚSCULAS, como iOS). */
data class LevelStyle(@StringRes val labelRes: Int, val text: Color, val bg: Color)

/** Mapea levelWilks/levelWeight a etiqueta + colores (LevelStyle.swift de iOS). */
fun levelStyle(level: String?): LevelStyle? = when (level?.lowercase()) {
    "elite" -> LevelStyle(R.string.level_elite, Aqua500, Aqua100)
    "advanced" -> LevelStyle(R.string.level_advanced, SkyBlue500, SkyBlue100)
    "intermediate" -> LevelStyle(R.string.level_intermediate, LemonGreen500, LemonGreen100)
    "noob" -> LevelStyle(R.string.level_noob, Brown500, Brown100)
    "beginner" -> LevelStyle(R.string.level_beginner, Red500, Red100)
    else -> null
}

/** Opciones de filtro (apiKey -> labelRes), igual que LevelOption.allCases de iOS. */
val levelFilterOptions = listOf(
    "elite" to R.string.level_elite,
    "advanced" to R.string.level_advanced,
    "intermediate" to R.string.level_intermediate,
    "noob" to R.string.level_noob,
    "beginner" to R.string.level_beginner,
)

/** Código ISO de país -> emoji bandera. */
fun countryFlag(code: String?): String {
    if (code == null || code.length != 2) return ""
    return code.uppercase().map { 0x1F1E6 + (it - 'A') }
        .joinToString("") { String(Character.toChars(it)) }
}
