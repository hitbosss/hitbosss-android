package com.hitbosss.presentation.feature.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hitbosss.presentation.designsystem.theme.Error400
import com.hitbosss.presentation.designsystem.theme.Red500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Orange300
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Success400
import com.hitbosss.presentation.designsystem.theme.Success500

/** Etiqueta de sección en mayúsculas (ESTADO FÍSICO, OBJETIVOS, ...). */
@Composable
fun MetricsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = HitbosssType.bodySmallEmphasis,
        color = Gray500,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Tarjeta blanca redondeada, contenedor estándar de los módulos de Métricas. */
@Composable
fun MetricsCard(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Gray100)
            .padding(16.dp),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** Segmented control (patrón VisibilitySegment/RecordSegment). */
@Composable
fun MetricsSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Gray400)
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (i == selectedIndex) Gray100 else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = if (i == selectedIndex) HitbosssType.bodyDefaultEmphasis else HitbosssType.bodyDefaultRegular,
                    color = if (i == selectedIndex) Gray800 else Gray500,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Icono ⓘ que abre el popup explicativo del módulo. */
@Composable
fun InfoIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Icon(
        Icons.Outlined.Info,
        contentDescription = null,
        tint = Gray400,
        modifier = modifier.size(16.dp).clickable(onClick = onClick),
    )
}

/** Tile de dato competitivo: icono + label + ⓘ arriba, valor grande + unidad debajo. */
@Composable
fun StatTile(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String,
    unit: String,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Gray100)
            .padding(12.dp),
    ) {
        // 1. Fila superior: Se mantiene intacta (Icono, Label con peso, InfoIcon)
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(iconBg),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(8.dp))
            Text(label, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.weight(1f))
            InfoIcon(onInfo)
        }

        Spacer(Modifier.height(8.dp))

        // 2. Fila inferior: Envuelta en Box para centrar solo este contenido
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = HitbosssType.titleSubsection, color = Gray800)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

/** Dots de pager (patrón WelcomeScreen). */
@Composable
fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i == current) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == current) Gray500 else Gray400),
            )
        }
    }
}

/**
 * Slider de IMC (16–35): barra con franjas de color (bajo azul / normal verde /
 * sobrepeso naranja / obesidad rojo) y marcador circular en el valor del usuario.
 */
@Composable
fun BmiSlider(bmi: Double?, modifier: Modifier = Modifier) {
    val minBmi = 16f
    val maxBmi = 35f
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            val barH = 9.dp.toPx()
            val y = size.height / 2f
            val bands = listOf(
                Triple(minBmi, 18.5f, Secondary500),
                Triple(18.5f, 25f, Success500),
                Triple(25f, 30f, Orange300),
                Triple(30f, maxBmi, Red500),
            )
            fun xOf(v: Float) = (v - minBmi) / (maxBmi - minBmi) * size.width
            bands.forEach { (from, to, color) ->
                drawRoundRect(
                    color,
                    topLeft = Offset(xOf(from), y - barH / 2),
                    size = Size(xOf(to) - xOf(from), barH),

                )
            }
            // Marcador del usuario: círculo blanco con borde del color de su franja. Sin datos → sin marcador.
            if (bmi != null) {
                val clamped = bmi.toFloat().coerceIn(minBmi, maxBmi)
                val bandColor = bands.first { clamped < it.second || it.second == maxBmi }.third
                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(xOf(clamped), y))
                drawCircle(bandColor, radius = 7.dp.toPx(), center = Offset(xOf(clamped), y), style = Stroke(2.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("16", "20", "25", "30", "35").forEach {
                Text(it, style = HitbosssType.bodySmallRegular, color = Gray500)
            }
        }
    }
}
