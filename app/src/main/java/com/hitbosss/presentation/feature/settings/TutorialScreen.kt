package com.hitbosss.presentation.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.designsystem.theme.Warning100

/** Lista de tutoriales agrupada por deporte (1:1 con SettingsTutorialsView.swift). */
@Composable
fun TutorialScreen(
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    viewModel: TutorialViewModel = hiltViewModel(),
) {
    val data = rememberTutorialData()
    val isMetric by viewModel.isMetric.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.tutorials_title), onBack = onBack)
        Column(
            Modifier.fillMaxSize().background(Gray200).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            listOf("powerlifting", "crossfit").forEach { sport ->
                val exercises = data.filter { it.sport == sport && !it.isOfficial }
                SportSection(sport, exercises, isMetric, onOpenExercise)
            }
        }
    }
}

@Composable
private fun SportSection(sport: String, exercises: List<TutorialExercise>, isMetric: Boolean, onOpenExercise: (String) -> Unit) {
    val isPl = sport == "powerlifting"
    val sportColor = if (isPl) Secondary500 else Error500
    val sportIcon = if (isPl) R.drawable.ic_sport_powerlifting else R.drawable.ic_sport_crossfit
    val sportTitle = if (isPl) "Powerlifting" else "CrossHIT"
    var showThresholds by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Cabecera del deporte
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(painterResource(sportIcon), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(sportTitle.uppercase(), style = HitbosssType.bodyDefaultRegular, color = sportColor, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.tutorial_section_thresholds), tint = Gray800, modifier = Modifier.size(24.dp).clickable { showThresholds = true })
        }
        // Tarjetas de ejercicio
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            exercises.forEach { ex -> ExerciseTutorialCard(ex, sportColor) { onOpenExercise(ex.apiKey) } }
        }
    }

    // Popup de umbrales oficiales del deporte (fullScreenCover en iOS)
    if (showThresholds) {
        val officialKey = if (isPl) "officialPowerlifting" else "officialCrossfit"
        val table = tutorialThresholds[officialKey]
        Dialog(onDismissRequest = { showThresholds = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier.fillMaxSize().background(Secondary800.copy(alpha = 0.8f)).clickable { showThresholds = false },
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.End) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(Gray100).border(1.dp, Gray300, CircleShape).clickable { showThresholds = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Gray600, modifier = Modifier.size(18.dp))
                    }
                    if (table != null) {
                        val title = if (isPl) stringResource(R.string.tutorial_thresholds_power) else stringResource(R.string.tutorial_thresholds_cross)
                        TutorialSectionView(R.drawable.ic_tutorial_thresholds, Warning100, title) {
                            TutorialThresholdTable(table, isMetric)
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta de ejercicio: imagen a la derecha + nombre a la izquierda (1:1 con ExerciseTutorialCardView). */
@Composable
private fun ExerciseTutorialCard(exercise: TutorialExercise, sportColor: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(120.dp).clip(RoundedCornerShape(16.dp))
            .background(Gray100).clickable { onClick() },
    ) {
        tutorialCardDrawable(exercise.apiKey)?.let { img ->
            // La imagen (3:1, atleta a la derecha) se muestra completa anclada a la derecha sin recortar.
            Image(
                painterResource(img), contentDescription = null,
                contentScale = ContentScale.Fit, alignment = Alignment.CenterEnd,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
        Text(
            exercise.title.uppercase(), style = HitbosssType.bodyLargeEmphasis, color = sportColor,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).widthIn(max = 150.dp),
        )
    }
}
