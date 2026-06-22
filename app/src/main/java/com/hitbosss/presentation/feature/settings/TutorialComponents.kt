package com.hitbosss.presentation.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.feature.ranking.levelStyle

/** Imagen de tarjeta del ejercicio (lista), o null para los oficiales. */
@DrawableRes
fun tutorialCardDrawable(apiKey: String): Int? = when (apiKey) {
    "squat" -> R.drawable.tutorial_card_squat
    "benchpress" -> R.drawable.tutorial_card_benchpress
    "deadlift" -> R.drawable.tutorial_card_deadlift
    "sumoDeadlift" -> R.drawable.tutorial_card_sumo_deadlift
    "snatch" -> R.drawable.tutorial_card_snatch
    "clean" -> R.drawable.tutorial_card_clean
    "cleanAndJerk" -> R.drawable.tutorial_card_clean_and_jerk
    else -> null
}

/** Miniatura del vídeo del ejercicio (detalle), o null para los oficiales. */
@DrawableRes
fun tutorialThumbDrawable(apiKey: String): Int? = when (apiKey) {
    "squat" -> R.drawable.tutorial_thumb_squat
    "benchpress" -> R.drawable.tutorial_thumb_benchpress
    "deadlift" -> R.drawable.tutorial_thumb_deadlift
    "sumoDeadlift" -> R.drawable.tutorial_thumb_sumo_deadlift
    "snatch" -> R.drawable.tutorial_thumb_snatch
    "clean" -> R.drawable.tutorial_thumb_clean
    "cleanAndJerk" -> R.drawable.tutorial_thumb_clean_and_jerk
    else -> null
}

/** Sección con icono en caja de color + título + contenido (1:1 con TutorialSectionView.swift). */
@Composable
fun TutorialSectionView(
    @DrawableRes icon: Int,
    iconBackground: Color,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Gray100)
            .border(1.dp, Gray300, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(title, style = HitbosssType.bodyLargeEmphasis, color = Gray800)
        }
        content()
    }
}

/** Miniatura clicable del vídeo con botón de play y etiqueta (1:1 con TutorialVideoThumbnail.swift). */
@Composable
fun TutorialVideoThumbnail(exercise: String, @DrawableRes thumb: Int, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(thumb), contentDescription = null,
            contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth(),
        )
        // Botón de play centrado
        Box(Modifier.size(48.dp).clip(CircleShape).background(Gray200), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Primary500, modifier = Modifier.size(24.dp))
        }
        // Etiqueta arriba-izquierda
        Column(Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopStart)) {
            Text(stringResource(R.string.tutorial_video_label), style = HitbosssType.bodySmallEmphasis, color = Gray100)
            Text(exercise, style = HitbosssType.bodyLargeEmphasis, color = Gray100)
        }
    }
}

/** Tabla de umbrales male/female (1:1 con TutorialThresholdView.swift). */
@Composable
fun TutorialThresholdTable(table: ThresholdTable, isMetric: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ThresholdGenderTable(R.drawable.ic_tutorial_male, stringResource(R.string.threshold_male), table.maleRows, isMetric)
        ThresholdGenderTable(R.drawable.ic_tutorial_female, stringResource(R.string.threshold_female), table.femaleRows, isMetric)
    }
}

@Composable
private fun ThresholdGenderTable(@DrawableRes icon: Int, title: String, rows: List<ThresholdRow>, isMetric: Boolean) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gray100)
            .border(1.dp, Gray300, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(title, style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
        }
        Spacer(Modifier.height(12.dp))
        // Cabecera de columnas
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.threshold_col_levels), style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
            Text(if (isMetric) "KG" else "LBS", style = HitbosssType.bodyDefaultEmphasis, color = Gray800, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.threshold_col_points), style = HitbosssType.bodyDefaultEmphasis, color = Gray800, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray400))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { ThresholdRowItem(it, isMetric) }
        }
    }
}

@Composable
private fun ThresholdRowItem(row: ThresholdRow, isMetric: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            val lvl = levelStyle(row.level.name.lowercase())
            if (lvl != null) {
                Text(
                    stringResource(thresholdBadgeRes(row.level)), style = HitbosssType.bodySmallRegular, color = lvl.text,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(lvl.bg).padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Text(if (isMetric) row.weightKg else row.weightLbs, style = HitbosssType.bodySmallRegular, color = Gray800, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text(row.points, style = HitbosssType.bodySmallRegular, color = Gray800, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

/** Código corto del nivel para el badge de la tabla de umbrales (1:1 con badgeText de iOS). */
@androidx.annotation.StringRes
private fun thresholdBadgeRes(level: ThresholdLevel): Int = when (level) {
    ThresholdLevel.Beginner -> R.string.level_badge_beginner
    ThresholdLevel.Noob -> R.string.level_badge_noob
    ThresholdLevel.Intermediate -> R.string.level_badge_intermediate
    ThresholdLevel.Advanced -> R.string.level_badge_advanced
    ThresholdLevel.Elite -> R.string.level_badge_elite
}

/** Convierte **negrita** de markdown a texto con estilo. */
fun boldMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val regex = Regex("""\*\*(.+?)\*\*""")
    var last = 0
    regex.findAll(text).forEach { m ->
        append(text.substring(last, m.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(m.groupValues[1]) }
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}
