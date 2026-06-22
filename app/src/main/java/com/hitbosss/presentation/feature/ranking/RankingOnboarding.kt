package com.hitbosss.presentation.feature.ranking

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray700
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Primary600
import com.hitbosss.presentation.designsystem.theme.Secondary300
import com.hitbosss.presentation.designsystem.theme.Secondary800
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

/** Forma del recorte que resalta el elemento (1:1 con iOS OnboardingHighlightShape). */
enum class HighlightShape { Rectangle, Circle }

/**
 * Pasos del tutorial coach-marks del ranking (1:1 con iOS PowerliftingOnboardingStep).
 * Textos del Localizable.xcstrings (es).
 */
enum class RankingOnboardingStep(
    val titleHighlight: String,
    val titleRest: String,
    val description: String,
    val shape: HighlightShape,
) {
    Exercises(
        "Compite", " en distintos rankings.",
        "Selecciona el ejercicio y muestra tus habilidades. Aquí podrás demostrar tu fuerza y competir contra otros.",
        HighlightShape.Rectangle,
    ),
    Filters(
        "Ajusta el ranking", " a tu medida.",
        "Configura el ranking para una competición personalizada. Selecciona ubicación, género, edad o nivel.",
        HighlightShape.Circle,
    ),
    OrderBy(
        "Cambia los criterios", " del ranking.",
        "Ordena el ranking por peso levantado o por Points, que considera tu peso y el levantado.",
        HighlightShape.Circle,
    ),
    UploadHit(
        "Sube tus HITS", " y compite.",
        "Sube tus hits al ranking y compárate con otros. Asegúrate de que el video corresponda al ejercicio.",
        HighlightShape.Circle,
    ),
}

/** Persistencia del flag "visto" (1:1 con iOS TutorialTracker, clave powerlifting_onboarding). */
object TutorialTracker {
    private const val PREFS = "hitbosss_tutorial"
    private const val KEY_RANKING = "powerlifting_onboarding"

    fun hasSeenRankingOnboarding(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_RANKING, false)

    fun markRankingOnboardingSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_RANKING, true).apply()
    }

    /** Tutorial "Cómo grabar tu HIT" visto por ejercicio (1:1 con iOS hasSeenTutorial(for:)). */
    fun hasSeenExerciseTutorial(context: Context, apiKey: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("tutorial_seen_$apiKey", false)

    fun markExerciseTutorialSeen(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("tutorial_seen_$apiKey", true).apply()
    }
}

/**
 * Overlay del tutorial: scrim oscurecido con recorte alrededor del elemento resaltado + tarjeta.
 * Swipe horizontal para avanzar/retroceder (1:1 con iOS GlobalRankingView onboarding).
 */
@Composable
fun RankingOnboardingOverlay(
    step: RankingOnboardingStep,
    highlightFrame: Rect,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    // Los frames vienen en coordenadas del root (boundsInRoot). El Canvas de abajo dibuja en su
    // espacio local (su origen está desplazado del root por el padding superior, Scaffold, etc.), así
    // que restamos la posición del overlay en el root para que el recorte caiga sobre el elemento.
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() }
            .pointerInput(step) {
                var dragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onDragEnd = {
                        if (dragX < -50f) onNext() else if (dragX > 50f) onBack()
                    },
                ) { _, dragAmount -> dragX += dragAmount }
            },
    ) {
        OnboardingDimmedOverlay(highlightFrame.translate(-overlayOrigin.x, -overlayOrigin.y), step.shape)

        Box(Modifier.fillMaxSize().padding(horizontal = 47.dp), contentAlignment = Alignment.Center) {
            OnboardingCard(
                step = step,
                onNext = onNext,
                onBack = onBack,
                onSkip = onSkip,
            )
        }
    }
}

@Composable
private fun OnboardingDimmedOverlay(frame: Rect, shape: HighlightShape) {
    if (frame == Rect.Zero) return
    val padPx = with(androidx.compose.ui.platform.LocalDensity.current) { 6.dp.toPx() }
    val borderPx = with(androidx.compose.ui.platform.LocalDensity.current) { 3.dp.toPx() }
    val cornerPx = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
    Canvas(Modifier.fillMaxSize()) {
        val canvasRect = Rect(Offset.Zero, size)
        drawContext.canvas.saveLayer(canvasRect, Paint())
        drawRect(Secondary800.copy(alpha = 0.8f))
        when (shape) {
            HighlightShape.Circle -> {
                val d = maxOf(frame.width, frame.height)
                drawCircle(Color.Transparent, radius = d / 2f, center = frame.center, blendMode = BlendMode.Clear)
            }
            HighlightShape.Rectangle -> {
                drawRoundRect(
                    Color.Transparent,
                    topLeft = frame.topLeft,
                    size = frame.size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    blendMode = BlendMode.Clear,
                )
            }
        }
        drawContext.canvas.restore()

        // Borde resaltado en rosa (Primary500, 3dp).
        when (shape) {
            HighlightShape.Circle -> {
                val d = maxOf(frame.width, frame.height)
                drawCircle(Primary500, radius = d / 2f + padPx, center = frame.center, style = Stroke(borderPx))
            }
            HighlightShape.Rectangle -> {
                drawRoundRect(
                    Primary500,
                    topLeft = Offset(frame.left - padPx, frame.top - padPx),
                    size = Size(frame.width + padPx * 2, frame.height + padPx * 2),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(borderPx),
                )
            }
        }
    }
}

@Composable
private fun OnboardingCard(
    step: RankingOnboardingStep,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    val steps = RankingOnboardingStep.entries
    val index = step.ordinal
    val total = steps.size
    val isLast = index == total - 1

    Column(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Gray100).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Paso ${index + 1} de $total", style = HitbosssType.bodyDefaultRegular, color = Gray800)
            Spacer(Modifier.weight(1f))
            Text(
                "Saltar",
                style = HitbosssType.bodyDefaultRegular,
                color = Gray600,
                modifier = Modifier.clickable { onSkip() }.padding(vertical = 8.dp),
            )
        }

        // Indicador de pasos (LinearPageControl: solo el actual en rosa).
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(total) { i ->
                Box(Modifier.weight(1f).height(4.dp).background(if (i == index) Primary500 else Secondary300))
            }
        }

        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Primary500)) { append(step.titleHighlight) }
                withStyle(SpanStyle(color = Gray800)) { append(step.titleRest) }
            },
            style = HitbosssType.titleBody,
        )

        Text(step.description, style = HitbosssType.bodyDefaultRegular, color = Gray800)

        Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (index > 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Gray100)
                        .border(1.dp, Gray400, RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Gray600, modifier = Modifier.size(16.dp))
                    Text("Atrás", style = HitbosssType.bodySmallRegular, color = Gray600)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isLast) Primary500 else Gray700)
                    .border(1.dp, if (isLast) Primary600 else Gray500, RoundedCornerShape(8.dp))
                    .clickable { onNext() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (isLast) "Terminar" else "Continuar", style = HitbosssType.bodySmallRegular, color = Gray100)
                Icon(
                    if (isLast) Icons.Filled.Check else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Gray100,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
