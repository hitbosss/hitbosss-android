package com.hitbosss.presentation.feature.hit

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onTutorial: () -> Unit = {},
) {
    var showSheet by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { if (isOfficial) showAlert = true else showSheet = true },
        containerColor = Primary500,
        contentColor = Gray100,
        modifier = modifier,
    ) { Icon(Icons.Filled.Add, contentDescription = "Subir HIT") }

    if (showAlert) OfficialNotExerciseAlert(sport) { showAlert = false }

    if (showSheet) {
        val exercise = Exercise.entries.firstOrNull { it.apiValue.equals(exerciseKey, true) } ?: Exercise.Squat
        UploadHitSheet(
            exercise = exercise,
            onDismiss = { showSheet = false },
            onRecord = { weight -> showSheet = false; onRecordHit(exerciseKey, weight) },
            onTutorial = onTutorial,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadHitSheet(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onRecord: (Double) -> Unit,
    onTutorial: () -> Unit,
    viewModel: HitUploadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                Text("Registra tu HIT", style = HitbosssType.titleSection, color = Gray800, textAlign = TextAlign.Center)
                Text(
                    exercise.title,
                    style = HitbosssType.bodySmallEmphasis,
                    color = sportColor,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(sportChipBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Text(
                "Peso total a levantar",
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
                        decorationBox = { inner ->
                            if (state.lift.isEmpty()) Text("0", style = HitbosssType.bodyLargeRegular, color = Gray500)
                            inner()
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
                    Text("Importante", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                }
                Text(
                    "Incluye el peso de la barra más el de los discos. Revisa las normas para evitar penalizaciones.",
                    style = HitbosssType.bodyDefaultRegular,
                    color = Gray500,
                )
            }

            ActionRow(
                icon = { Icon(Icons.Filled.SaveAlt, contentDescription = null, tint = Orange300, modifier = Modifier.size(18.dp)) },
                iconBg = Orange100,
                iconBorder = Orange200,
                title = "HITS guardados",
                subtitle = "Ninguno pendiente de subir",
                onClick = {},
            )
            ActionRow(
                icon = { Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Purple400, modifier = Modifier.size(18.dp)) },
                iconBg = Purple100,
                iconBorder = Purple200,
                title = "Ver tutorial",
                subtitle = "Cómo grabar tu HIT",
                onClick = onTutorial,
            )

            HitButton(
                text = "Grabar hit",
                onClick = { state.lift.toDoubleOrNull()?.let(onRecord) },
                enabled = state.canRecord,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            )
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
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Gray500)
    }
}

/** Aviso al pulsar subir en una pestaña oficial (Powerlifting/CrossHIT). */
@Composable
private fun OfficialNotExerciseAlert(sport: Sport, onDismiss: () -> Unit) {
    val isPl = sport == Sport.Powerlifting
    val name = if (isPl) "Powerlifting" else "CrossHIT"
    val exercisesText = if (isPl) "Sentadilla, Press Banca y Peso Muerto (o Sumo)" else "Snatch, Clean y Clean & Jerk"

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Gray100,
        title = {
            Text(
                "$name no es un ejercicio",
                style = HitbosssType.titleSection,
                color = Gray800,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Text(
                buildAnnotatedString {
                    append("No puedes subir un hit directamente a $name, porque se calcula sumando tus marcas en $exercisesText.\n\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Para mejorar tu posición, ") }
                    append("sube un nuevo hit que supere el anterior en cualquiera de esos ejercicios.")
                },
                style = HitbosssType.bodyDefaultRegular,
                color = Gray500,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            HitButton("Aceptar", onClick = onDismiss, type = HitButtonType.Secondary)
        },
    )
}
