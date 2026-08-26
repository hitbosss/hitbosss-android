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
import com.hitbosss.presentation.designsystem.theme.Success500

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
    yLabelCount: Int = 15,
    yFormatter: (Double) -> String = { v -> if (v % 1.0 == 0.0) "${v.toInt()}" else "%.1f".format(v) },
    selectedIndex: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
    pointLabel: ((Int) -> String)? = null,
    // Si se indica, el tap busca el punto MÁS CERCANO (2D) de ESA serie y, si cae dentro del radio de toque,
    // llama a onSelect(index) con su índice (para "abrir" al pulsar un marcador concreto, p.ej. un HIT).
    tapSeriesIndex: Int? = null,
    // Línea de objetivo: se dibuja como línea discontinua en su color y añade su valor como etiqueta en el eje Y.
    goalLine: Double? = null,
    goalColor: Color = Success500,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = HitbosssType.bodySmallRegular.copy(color = Gray500)

    // Escala Y y gutter se calculan también fuera del Canvas para poder hacer hit-testing del tap.
    val allY = series.flatMap { s -> s.points.map { it.second } }
    val primary = series.firstOrNull { it.points.isNotEmpty() }
    // Eje Y centrado en los PUNTOS de datos: rango simétrico alrededor de su centro para que queden
    // lo más centrados posible. Las líneas de referencia (objetivo/comunidad, dashed) no descentran;
    // solo amplían el rango —de forma simétrica— si caen razonablemente cerca (si no, se ignoran).
    val dataY = series.filterNot { it.dashed }.flatMap { s -> s.points.map { it.second } }.ifEmpty { allY }
    var lo = dataY.minOrNull() ?: 0.0
    var hi = dataY.maxOrNull() ?: 1.0
    // Rango degenerado (1 punto o todos iguales): abre una ventana proporcional al valor para que no quede pegado.
    if (hi == lo) { val d = (kotlin.math.abs(hi) * 0.08).coerceAtLeast(1.0); hi += d; lo -= d }
    val center = (lo + hi) / 2.0
    var half = (hi - lo) / 2.0 * 1.6   // margen: los puntos ocupan ~el 60% central de la altura
    series.filter { it.dashed }.flatMap { it.points }.forEach { (_, r) ->
        val dist = kotlin.math.abs(r - center)
        if (dist <= (hi - lo) + half) half = maxOf(half, dist)
    }
    // El objetivo siempre visible, con algo de margen por encima/debajo.
    goalLine?.let { g -> half = maxOf(half, kotlin.math.abs(g - center) * 1.15) }
    val yMin = center - half
    val yMax = center + half
    val yLabelTexts = (0 until yLabelCount).map { i -> yFormatter(yMin + (yMax - yMin) * i / (yLabelCount - 1)) }
    val yGutter = with(density) { yLabelTexts.maxOf { textMeasurer.measure(it, labelStyle).size.width }.toFloat() + 8.dp.toPx() }

    val xLabelHeightPx = with(density) { 18.dp.toPx() }
    val touchRadiusPx = with(density) { 28.dp.toPx() }
    val tapTarget = tapSeriesIndex?.let { series.getOrNull(it) }
    val tapModifier = when {
        // Modo "abrir al pulsar un marcador" (p.ej. HITs): punto más cercano en 2D de esa serie, con radio.
        onSelect != null && tapTarget != null && tapTarget.points.isNotEmpty() -> {
            Modifier.pointerInput(tapTarget.points) {
                detectTapGestures { offset ->
                    val plotW = (size.width - yGutter).coerceAtLeast(1f)
                    val plotBottom = size.height - xLabelHeightPx
                    val pts = tapTarget.points
                    var best = -1; var bestD = Double.MAX_VALUE
                    pts.forEachIndexed { i, (fx, v) ->
                        val sx = yGutter + fx * plotW
                        val sy = plotBottom - ((v - yMin) / (yMax - yMin)).toFloat() * plotBottom
                        val d = kotlin.math.hypot((sx - offset.x).toDouble(), (sy - offset.y).toDouble())
                        if (d < bestD) { bestD = d; best = i }
                    }
                    if (best >= 0 && bestD <= touchRadiusPx) onSelect(best)
                }
            }
        }
        // Modo tooltip (Físico): punto más cercano en X de la serie principal, alterna selección.
        onSelect != null && primary != null -> {
            Modifier.pointerInput(primary.points) {
                detectTapGestures { offset ->
                    val plotWidth = (size.width - yGutter).coerceAtLeast(1f)
                    val frac = (offset.x - yGutter) / plotWidth
                    val pts = primary.points
                    val nearest = pts.indices.minByOrNull { kotlin.math.abs(pts[it].first - frac) } ?: return@detectTapGestures
                    onSelect(if (nearest == selectedIndex) null else nearest)
                }
            }
        }
        else -> Modifier
    }

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

        // Línea de objetivo (discontinua) + su valor como etiqueta en el eje Y (en su color).
        goalLine?.let { g ->
            if (g in yMin..yMax) {
                val gy = py(g)
                drawLine(
                    goalColor, Offset(plot.left, gy), Offset(plot.right, gy),
                    strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                )
                val gl = textMeasurer.measure(yFormatter(g), labelStyle.copy(color = goalColor))
                drawText(gl, topLeft = Offset(0f, (gy - gl.size.height / 2f).coerceIn(0f, size.height - gl.size.height)))
            }
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
