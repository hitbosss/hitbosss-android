package com.hitbosss.presentation.feature.hit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hitbosss.presentation.feature.ranking.VideoPlayer
import com.hitbosss.presentation.feature.settings.rememberTutorialData
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.Sport
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.theme.Error100
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Orange100
import com.hitbosss.presentation.designsystem.theme.Orange200
import com.hitbosss.presentation.designsystem.theme.Orange300
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Purple100
import com.hitbosss.presentation.designsystem.theme.Purple200
import com.hitbosss.presentation.designsystem.theme.Purple400
import com.hitbosss.presentation.designsystem.theme.Secondary100
import com.hitbosss.presentation.designsystem.theme.Secondary200
import com.hitbosss.presentation.designsystem.theme.Secondary500
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.feature.ranking.TutorialTracker
import com.hitbosss.presentation.feature.ranking.titleRes

/**
 * FAB de subir HIT del ranking. En pestañas oficiales muestra el aviso "no es un ejercicio"; en un
 * ejercicio concreto abre el modal "Registra tu HIT" (bottom sheet, igual que iOS).
 */
@Composable
fun UploadHitFab(
    sport: Sport,
    isOfficial: Boolean,
    exerciseKey: String,
    onRecordHit: (exercise: String, weight: Double) -> Unit,
    modifier: Modifier = Modifier,
    onTutorial: (apiKey: String) -> Unit = {},
    onSavedHits: () -> Unit = {},
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    // Al pulsar "Ver tutorial" se cierra la hoja y se navega; esta bandera (persistida) hace que al
    // VOLVER de la página de tutoriales se reabra la hoja sola, sin tener que pulsar "subir hit" otra vez.
    var reopenSheetOnReturn by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (reopenSheetOnReturn) { reopenSheetOnReturn = false; showSheet = true }
    }

    FloatingActionButton(
        onClick = { if (isOfficial) showAlert = true else showSheet = true },
        containerColor = Primary500,
        contentColor = Gray100,
        modifier = modifier,
    ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.hit_upload)) }

    if (showAlert) OfficialNotExerciseAlert(sport) { showAlert = false }

    if (showSheet) {
        val exercise = Exercise.entries.firstOrNull { it.apiValue.equals(exerciseKey, true) } ?: Exercise.Squat
        UploadHitSheet(
            exercise = exercise,
            onDismiss = { showSheet = false },
            onRecord = { weight -> showSheet = false; onRecordHit(exerciseKey, weight) },
            // "Ver tutorial": cierra la hoja y navega; marca para reabrirla al volver.
            onTutorial = { apiKey -> reopenSheetOnReturn = true; showSheet = false; onTutorial(apiKey) },
            onSavedHits = { showSheet = false; onSavedHits() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadHitSheet(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onRecord: (Double) -> Unit,
    onTutorial: (apiKey: String) -> Unit,
    onSavedHits: () -> Unit,
    viewModel: HitUploadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Modal de vídeo del tutorial (se muestra la PRIMERA vez que se va a grabar este ejercicio).
    var showVideoModal by remember { mutableStateOf(false) }
    val tutorialVideoUrl = rememberTutorialData().firstOrNull { it.apiKey == exercise.apiValue }?.videoUrl

    val isPl = exercise.sport == Sport.Powerlifting
    val sportColor = if (isPl) Secondary500 else Error500
    val sportChipBg = if (isPl) Secondary100 else Error100

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Gray100,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            // Cabecera: título + chip del ejercicio.
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.hit_register), style = HitbosssType.titleSection, color = Gray800, textAlign = TextAlign.Center)
                Text(
                    stringResource(exercise.titleRes()),
                    style = HitbosssType.bodySmallEmphasis,
                    color = sportColor,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(sportChipBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Text(
                stringResource(R.string.hit_total_weight),
                style = HitbosssType.bodyDefaultEmphasis,
                color = Gray500,
                modifier = Modifier.padding(top = 26.dp, start = 16.dp, end = 16.dp),
            )

            // Campo de peso + insignia de unidad.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(68.dp).clip(RoundedCornerShape(8.dp))
                        .background(Gray200).border(1.dp, Gray300, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = state.lift,
                        onValueChange = viewModel::onLiftChange,
                        textStyle = HitbosssType.bodyLargeRegular.copy(color = Gray800),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        // Llena toda la caja para que el área de tap sea el recuadro entero (68dp), no solo el texto.
                        modifier = Modifier.fillMaxSize(),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                if (state.lift.isEmpty()) Text("0", style = HitbosssType.bodyLargeRegular, color = Gray500)
                                inner()
                            }
                        },
                    )
                }
                Box(
                    modifier = Modifier.height(68.dp).clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.unitLabel, style = HitbosssType.bodyLargeEmphasis, color = Gray500)
                }
            }

            // Aviso "Importante".
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Secondary100)
                    .border(1.dp, Secondary200, RoundedCornerShape(12.dp))
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Secondary500, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.hit_important), style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                }
                Text(
                    stringResource(R.string.hit_important_desc),
                    style = HitbosssType.bodyDefaultRegular,
                    color = Gray500,
                )
            }

            ActionRow(
                icon = { Icon(Icons.Filled.SaveAlt, contentDescription = null, tint = Orange300, modifier = Modifier.size(18.dp)) },
                iconBg = Orange100,
                iconBorder = Orange200,
                title = stringResource(R.string.hit_saved_list),
                subtitle = stringResource(R.string.hit_none_pending),
                onClick = onSavedHits,
                badgeCount = state.pendingCount,
            )
            ActionRow(
                icon = { Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Purple400, modifier = Modifier.size(18.dp)) },
                iconBg = Purple100,
                iconBorder = Purple200,
                title = stringResource(R.string.hit_see_tutorial),
                subtitle = stringResource(R.string.hit_how_record),
                onClick = { onTutorial(exercise.apiValue) },
            )

            HitButton(
                text = stringResource(R.string.hit_record),
                onClick = {
                    val w = state.lift.toDoubleOrNull() ?: return@HitButton
                    // La PRIMERA vez que se va a grabar este ejercicio se muestra el vídeo en una modal
                    // (no se navega a la página de tutoriales). Las siguientes veces graba directo.
                    if (!TutorialTracker.hasSeenExerciseTutorial(context, exercise.apiValue) && tutorialVideoUrl != null) {
                        TutorialTracker.markExerciseTutorialSeen(context, exercise.apiValue)
                        showVideoModal = true
                    } else {
                        onRecord(w)
                    }
                },
                enabled = state.canRecord,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            )
        }
    }

    // Modal con el vídeo del tutorial (primera vez): sobre la hoja; al cerrarlo, la hoja sigue abierta.
    if (showVideoModal && tutorialVideoUrl != null) {
        TutorialVideoDialog(url = tutorialVideoUrl, onDismiss = { showVideoModal = false })
    }
}

/** Modal que reproduce el vídeo del tutorial del ejercicio (9:16, igual que la página de tutoriales). */
@Composable
private fun TutorialVideoDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp)).background(Color.Black),
            ) {
                VideoPlayer(url = url, seekSeconds = 0.0, isActive = true)
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp)
                        .clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.4f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: @Composable () -> Unit,
    iconBg: Color,
    iconBorder: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badgeCount: Int = 0,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(iconBg)
                .border(1.dp, iconBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(Modifier.weight(1f)) {
            Text(title, style = HitbosssType.bodyLargeEmphasis, color = Gray500)
            Text(subtitle, style = HitbosssType.bodySmallRegular, color = Gray500)
        }
        // Badge con el nº de HITs sin subir (igual que iOS).
        if (badgeCount > 0) {
            Box(
                Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Primary500),
                contentAlignment = Alignment.Center,
            ) {
                Text("$badgeCount", style = HitbosssType.bodySmallEmphasis, color = Gray100)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Gray500)
    }
}

/** Aviso al pulsar subir en una pestaña oficial (Powerlifting/CrossHIT). */
@Composable
private fun OfficialNotExerciseAlert(sport: Sport, onDismiss: () -> Unit) {
    val isPl = sport == Sport.Powerlifting
    val name = if (isPl) "Powerlifting" else "CrossHIT"
    val exercisesText = if (isPl) stringResource(R.string.hit_pl_exercises) else stringResource(R.string.hit_cf_exercises)

    HitPopup(
        title = stringResource(R.string.hit_not_exercise_title, name),
        message = buildAnnotatedString {
            append(stringResource(R.string.hit_official_explain_1, name, exercisesText))
            append("\n\n")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(R.string.hit_official_improve_lead)) }
            append(stringResource(R.string.hit_official_improve_rest))
        },
        confirmText = stringResource(R.string.common_accept),
        confirmType = HitButtonType.Secondary,
        onConfirm = onDismiss,
        onDismissRequest = onDismiss,
    )
}
