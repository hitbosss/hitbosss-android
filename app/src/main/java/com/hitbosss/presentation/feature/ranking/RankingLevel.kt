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

data class LevelStyle(val label: String, val text: Color, val bg: Color)

/** Mapea levelWilks/levelWeight a etiqueta + colores (LevelStyle.swift de iOS). */
fun levelStyle(level: String?): LevelStyle? = when (level?.lowercase()) {
    "elite" -> LevelStyle("ÉLITE", Aqua500, Aqua100)
    "advanced" -> LevelStyle("AVANZADO", SkyBlue500, SkyBlue100)
    "intermediate" -> LevelStyle("INTERMEDIO", LemonGreen500, LemonGreen100)
    "noob" -> LevelStyle("NOVATO", Brown500, Brown100)
    "beginner" -> LevelStyle("PRINCIPIANTE", Red500, Red100)
    else -> null
}

/** Opciones de filtro (apiKey -> nombre), igual que LevelOption.allCases de iOS. */
val levelFilterOptions = listOf(
    "elite" to "Élite",
    "advanced" to "Avanzado",
    "intermediate" to "Intermedio",
    "noob" to "Novato",
    "beginner" to "Principiante",
)

/** Código ISO de país -> emoji bandera. */
fun countryFlag(code: String?): String {
    if (code == null || code.length != 2) return ""
    return code.uppercase().map { 0x1F1E6 + (it - 'A') }
        .joinToString("") { String(Character.toChars(it)) }
}
