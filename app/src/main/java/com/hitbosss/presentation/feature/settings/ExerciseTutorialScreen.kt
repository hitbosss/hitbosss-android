package com.hitbosss.presentation.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Error100
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary100
import com.hitbosss.presentation.designsystem.theme.Success100
import com.hitbosss.presentation.designsystem.theme.Warning100
import com.hitbosss.presentation.feature.ranking.VideoPlayer

/** Detalle de tutorial de un ejercicio (1:1 con ExerciseTutorialDetailView.swift). */
@Composable
fun ExerciseTutorialScreen(
    apiKey: String,
    onBack: () -> Unit,
    viewModel: TutorialViewModel = hiltViewModel(),
) {
    val data = rememberTutorialData()
    val exercise = data.firstOrNull { it.apiKey == apiKey } ?: data.first()
    val isMetric by viewModel.isMetric.collectAsStateWithLifecycle()
    var showVideo by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Gray200)) {
        HitTopBar(title = exercise.title, onBack = onBack)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Información del ejercicio + miniatura del vídeo
            TutorialSectionView(R.drawable.ic_tutorial_info, Secondary100, stringResource(R.string.tutorial_section_info)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(boldMarkdown(exercise.info), style = HitbosssType.bodySmallRegular, color = Gray500)
                    val thumb = tutorialThumbDrawable(exercise.apiKey)
                    if (thumb != null && exercise.videoUrl != null) {
                        TutorialVideoThumbnail(exercise.title, thumb) { showVideo = true }
                    }
                }
            }

            exercise.techniqueText?.let { technique ->
                TutorialSectionView(R.drawable.ic_tutorial_technique, Success100, stringResource(R.string.tutorial_section_technique)) {
                    Text(boldMarkdown(technique), style = HitbosssType.bodySmallRegular, color = Gray500)
                }
            }

            exercise.recordingText?.let { recording ->
                TutorialSectionView(R.drawable.ic_tutorial_record, Error100, stringResource(R.string.tutorial_section_recording)) {
                    Text(boldMarkdown(recording), style = HitbosssType.bodySmallRegular, color = Gray500)
                }
            }

            exercise.thresholds?.let { table ->
                TutorialSectionView(R.drawable.ic_tutorial_thresholds, Warning100, stringResource(R.string.tutorial_section_thresholds)) {
                    TutorialThresholdTable(table, isMetric)
                }
            }
        }
    }

    // Reproductor a pantalla completa (superpuesto)
    if (showVideo && exercise.videoUrl != null) {
        Box(Modifier.fillMaxSize().background(Gray800)) {
            VideoPlayer(url = exercise.videoUrl, seekSeconds = 0.0, isActive = true)
            Box(
                Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopEnd).size(32.dp).clip(CircleShape)
                    .background(Gray100).border(1.dp, Gray300, CircleShape).clickable { showVideo = false },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Gray600, modifier = Modifier.size(18.dp))
            }
        }
    }
    }
}
