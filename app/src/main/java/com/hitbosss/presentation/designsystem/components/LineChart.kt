package com.hitbosss.presentation.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType

enum class ChartMarker { None, Circle, Diamond }

/** Por encima de esta densidad de puntos se ocultan los marcadores y se deja solo la línea
 * (al hacer zoom in y bajar el nº de puntos, reaparecen). Diseño Físico M9, opción 2. */
private const val MARKER_DENSITY_LIMIT = 16

/**
 * Serie de la gráfica. `points` = (x en fracción 0..1 del ancho, valor y en unidades de datos).
 * Los puntos con el mismo eje Y de todas las series comparten escala (rango automático común).
 */
data class ChartSeries(
    val points: List<Pair<Float, Double>>,
    val color: Color,
    val dashed: Boolean = false,
    val marker: ChartMarker = ChartMarker.None,
    val showLine: Boolean = true,
    val fill: Boolean = false,
)

/**
 * Gráfica de líneas con Canvas (no existe otra en la app; reutilizable Físico/Fuerza).
 * Eje Y con rango automático + etiquetas, grid horizontal ligero, etiquetas X equiespaciadas.
 * Interacción opcional: al tocar un punto de la serie principal se resalta y muestra una burbuja
 * con `pointLabel(index)` (fecha + valor). El caller gestiona `selectedIndex`/`onSelect`.
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    yLabelCount: Int = 4,
    yFormatter: (Double) -> String = { v -> if (v % 1.0 == 0.0) "${v.toInt()}" else "%.1f".format(v) },
    selectedIndex: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
    pointLabel: ((Int) -> String)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = HitbosssType.bodySmallRegular.copy(color = Gray500)

    // Escala Y y gutter se calculan también fuera del Canvas para poder hacer hit-testing del tap.
    val allY = series.flatMap { s -> s.points.map { it.second } }
    val primary = series.firstOrNull { it.points.isNotEmpty() }
    var yMin = allY.minOrNull() ?: 0.0
    var yMax = allY.maxOrNull() ?: 1.0
    // Rango degenerado (1 punto o todos iguales): abre una ventana proporcional al valor para que no quede pegado.
    if (yMax == yMin) { val d = (kotlin.math.abs(yMax) * 0.05).coerceAtLeast(1.0); yMax += d; yMin -= d }
    // Margen vertical amplio (antes 0.15, quedaba "apretado": el eje empezaba casi pegado al mínimo).
    val padY = (yMax - yMin) * 0.25
    yMin -= padY; yMax += padY
    val yLabelTexts = (0 until yLabelCount).map { i -> yFormatter(yMin + (yMax - yMin) * i / (yLabelCount - 1)) }
    val yGutter = with(density) { yLabelTexts.maxOf { textMeasurer.measure(it, labelStyle).size.width }.toFloat() + 8.dp.toPx() }

    val tapModifier = if (onSelect != null && primary != null) {
        Modifier.pointerInput(primary.points) {
            detectTapGestures { offset ->
                val plotWidth = (size.width - yGutter).coerceAtLeast(1f)
                val frac = (offset.x - yGutter) / plotWidth
                val pts = primary.points
                val nearest = pts.indices.minByOrNull { kotlin.math.abs(pts[it].first - frac) } ?: return@detectTapGestures
                onSelect(if (nearest == selectedIndex) null else nearest)
            }
        }
    } else Modifier

    Canvas(modifier.then(tapModifier)) {
        if (allY.isEmpty()) return@Canvas
        val xLabelHeight = with(density) { 18.dp.toPx() }
        val plot = Rect(yGutter, 0f, size.width, size.height - xLabelHeight)
        fun px(x: Float) = plot.left + x * plot.width
        fun py(y: Double) = plot.bottom - ((y - yMin) / (yMax - yMin)).toFloat() * plot.height

        // Grid horizontal + etiquetas Y
        for (i in 0 until yLabelCount) {
            val yVal = yMin + (yMax - yMin) * i / (yLabelCount - 1)
            val y = py(yVal)
            drawLine(Gray400, Offset(plot.left, y), Offset(plot.right, y), strokeWidth = 1f)
            val layout = textMeasurer.measure(yLabelTexts[i], labelStyle)
            drawText(layout, topLeft = Offset(0f, (y - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height)))
        }

        // Etiquetas X equiespaciadas
        if (xLabels.isNotEmpty()) {
            xLabels.forEachIndexed { i, label ->
                val x = if (xLabels.size == 1) plot.center.x
                else plot.left + plot.width * i / (xLabels.size - 1f)
                val layout = textMeasurer.measure(label, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        (x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width),
                        plot.bottom + with(density) { 4.dp.toPx() },
                    ),
                )
            }
        }

        // Series
        series.forEach { s ->
            if (s.points.isEmpty()) return@forEach
            val offsets = s.points.sortedBy { it.first }.map { Offset(px(it.first), py(it.second)) }

            if (s.fill && offsets.size > 1) {
                val fillPath = Path().apply {
                    moveTo(offsets.first().x, plot.bottom)
                    offsets.forEach { lineTo(it.x, it.y) }
                    lineTo(offsets.last().x, plot.bottom)
                    close()
                }
                drawPath(fillPath, s.color.copy(alpha = 0.12f))
            }

            if (s.showLine && offsets.size > 1) {
                val path = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path, s.color,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = if (s.dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 10f)) else null,
                    ),
                )
            }

            // Muchos puntos → solo línea (los marcadores se solaparían); pocos → se pintan.
            if (s.points.size <= MARKER_DENSITY_LIMIT) offsets.forEach { drawMarker(s.marker, it, s.color) }
        }

        // Tooltip del punto seleccionado (serie principal): guía vertical + marcador + burbuja.
        if (selectedIndex != null && primary != null && pointLabel != null) {
            primary.points.getOrNull(selectedIndex)?.let { (fx, fy) ->
                val cx = px(fx)
                val cy = py(fy)
                drawLine(primary.color.copy(alpha = 0.4f), Offset(cx, plot.top), Offset(cx, plot.bottom), strokeWidth = 1.dp.toPx())
                drawCircle(primary.color, radius = 6.dp.toPx(), center = Offset(cx, cy))
                drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
                val layout = textMeasurer.measure(pointLabel(selectedIndex), labelStyle.copy(color = Color.White))
                val padH = 8.dp.toPx()
                val padV = 5.dp.toPx()
                val bw = layout.size.width + padH * 2
                val bh = layout.size.height + padV * 2
                val bx = (cx - bw / 2).coerceIn(plot.left, (plot.right - bw).coerceAtLeast(plot.left))
                val by = if (cy - bh - 8.dp.toPx() < plot.top) cy + 8.dp.toPx() else cy - bh - 8.dp.toPx()
                drawRoundRect(Gray800, topLeft = Offset(bx, by), size = Size(bw, bh), cornerRadius = CornerRadius(6.dp.toPx()))
                drawText(layout, topLeft = Offset(bx + padH, by + padV))
            }
        }
    }
}

private fun DrawScope.drawMarker(marker: ChartMarker, center: Offset, color: Color) {
    when (marker) {
        ChartMarker.None -> Unit
        ChartMarker.Circle -> {
            // Círculo hueco (entrenamiento): relleno blanco + borde de color
            drawCircle(Color.White, radius = 4.dp.toPx(), center = center)
            drawCircle(color, radius = 4.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
        }
        ChartMarker.Diamond -> {
            // Rombo relleno (HIT oficial)
            val r = 5.dp.toPx()
            val path = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + r, center.y)
                lineTo(center.x, center.y + r)
                lineTo(center.x - r, center.y)
                close()
            }
            drawPath(path, color)
        }
    }
}
