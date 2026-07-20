package com.hitbosss.presentation.feature.community

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.Sport
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitPopup
import kotlinx.coroutines.launch
import com.hitbosss.presentation.feature.ranking.titleRes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(viewModel::onCoverPicked) }
    LaunchedEffect(state.success) { if (state.success) onCreated() }

    // Confirmar descartar si hay datos introducidos.
    var showDiscard by remember { mutableStateOf(false) }
    val hasChanges = state.name.isNotBlank() || state.motto.isNotBlank() || state.description.isNotBlank() ||
        state.coverUri != null || state.exercises.isNotEmpty()
    fun back() { if (hasChanges) showDiscard = true else onBack() }
    androidx.activity.compose.BackHandler(enabled = hasChanges) { showDiscard = true }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.create_group_title), onBack = ::back)

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            // Foto del grupo (cuadrado + botón +)
            Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.TopCenter) {
                Box {
                    AsyncImage(
                        model = state.coverUri, contentDescription = null, contentScale = ContentScale.Crop,
                        placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp)).background(Gray300),
                    )
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(x = 12.dp, y = 12.dp).size(36.dp).clip(CircleShape)
                            .background(Gray100).border(1.dp, Gray300, CircleShape)
                            .clickable { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_group_change_photo), tint = Gray600, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            FieldLabel(stringResource(R.string.create_group_name))
            FormField(state.name, viewModel::onName, 52.dp)
            Counter(state.name.length, viewModel.maxName)
            Spacer(Modifier.height(8.dp))

            FieldLabel(stringResource(R.string.create_group_motto))
            FormField(state.motto, viewModel::onMotto, 52.dp)
            Counter(state.motto.length, viewModel.maxMotto)
            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.common_description), style = HitbosssType.bodySmallEmphasis, color = Gray800, modifier = Modifier.padding(bottom = 8.dp))
            FormField(state.description, viewModel::onDescription, 152.dp, single = false)
            Counter(state.description.length, viewModel.maxDescription)
            Spacer(Modifier.height(16.dp))

            // Visibilidad (público / privado), igual que iOS.
            Text(stringResource(R.string.visibility_title), style = HitbosssType.bodySmallEmphasis, color = Gray800, modifier = Modifier.padding(bottom = 8.dp))
            Text(stringResource(R.string.visibility_desc_group), style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 8.dp))
            VisibilitySegment(state.isPublic, viewModel::onVisibility)
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.common_exercises), style = HitbosssType.bodySmallEmphasis, color = Gray800)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.create_group_exercises_desc),
                style = HitbosssType.bodySmallRegular, color = Gray500,
            )
            Spacer(Modifier.height(16.dp))

            Sport.entries.forEach { sport ->
                val sportColor = if (sport == Sport.Crossfit) Error500 else Secondary500
                val sportExs = Exercise.forSport(sport)
                Text(sport.brandTitle.uppercase(), style = HitbosssType.bodySmallRegular, color = sportColor)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Chip de disciplina (oficial): activa los 3 ejercicios del deporte.
                    SelectChip(sport.brandTitle, state.exercises.containsAll(sportExs.toSet()), sportColor) { viewModel.toggleSport(sport) }
                    sportExs.forEach { ex ->
                        SelectChip(stringResource(ex.titleRes()), ex in state.exercises, sportColor) { viewModel.toggleExercise(ex) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        val enabled = state.isValid && !state.isLoading
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(12.dp)).background(if (enabled) Secondary800 else Gray300)
                .clickable(enabled = enabled) { viewModel.submit() }.padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isLoading) CircularProgressIndicator(color = Gray100, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.create_group_button), style = HitbosssType.bodyLargeEmphasis, color = if (enabled) Gray100 else Gray500)
        }
    }

    state.error?.let { error ->
        HitPopup(
            title = stringResource(R.string.common_something_wrong),
            message = error,
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearError,
            onDismissRequest = viewModel::clearError,
        )
    }

    if (showDiscard) {
        HitPopup(
            title = stringResource(R.string.create_discard_title),
            message = stringResource(R.string.create_discard_msg),
            confirmText = stringResource(R.string.common_discard),
            confirmType = com.hitbosss.presentation.designsystem.components.HitButtonType.Destructive,
            onConfirm = { showDiscard = false; onBack() },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showDiscard = false },
            onDismissRequest = { showDiscard = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String) =
    Text(text, style = HitbosssType.bodyDefaultEmphasis, color = Gray500, modifier = Modifier.padding(bottom = 8.dp))

@Composable
private fun Counter(count: Int, max: Int) = Text(
    "$count/$max", style = HitbosssType.bodySmallRegular, color = Gray500,
    textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
)

@Composable
private fun FormField(value: String, onChange: (String) -> Unit, height: androidx.compose.ui.unit.Dp, single: Boolean = true) {
    Box(
        Modifier.fillMaxWidth().let { if (single) it.height(height) else it.heightIn(min = height) }
            .clip(RoundedCornerShape(8.dp)).background(Gray200).border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(16.dp),
        contentAlignment = if (single) Alignment.CenterStart else Alignment.TopStart,
    ) {
        BasicTextField(value, onChange, singleLine = single, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
    }
}
