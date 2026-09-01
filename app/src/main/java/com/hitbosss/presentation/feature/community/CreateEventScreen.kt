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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.hitbosss.presentation.designsystem.theme.Secondary800
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitPopup
import kotlinx.coroutines.launch
import com.hitbosss.presentation.feature.ranking.titleRes
import com.hitbosss.presentation.designsystem.components.Counter
import com.hitbosss.presentation.designsystem.components.DateRow
import com.hitbosss.presentation.designsystem.components.EventDatePicker
import com.hitbosss.presentation.designsystem.components.FieldLabel
import com.hitbosss.presentation.designsystem.components.FormField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(viewModel::onCoverPicked) }
    var pickerFor by remember { mutableStateOf<String?>(null) } // "start" | "end"
    LaunchedEffect(state.success) { if (state.success) onCreated() }

    // Confirmar descartar si hay datos introducidos.
    var showDiscard by remember { mutableStateOf(false) }
    val hasChanges = state.name.isNotBlank() || state.description.isNotBlank() || state.coverUri != null ||
        state.exercises.isNotEmpty() || state.startMillis != null || state.endMillis != null
    fun back() { if (hasChanges) showDiscard = true else onBack() }
    androidx.activity.compose.BackHandler(enabled = hasChanges) { showDiscard = true }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.create_event_title), onBack = ::back)

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.TopCenter) {
                Box {
                    AsyncImage(model = state.coverUri, contentDescription = null, contentScale = ContentScale.Crop, placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(), modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp)).background(Gray300))
                    Box(
                        Modifier.align(Alignment.BottomEnd).offset(x = 12.dp, y = 12.dp).size(36.dp).clip(CircleShape)
                            .background(Gray100).border(1.dp, Gray300, CircleShape)
                            .clickable { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.common_photo), tint = Gray600, modifier = Modifier.size(20.dp)) }
                }
            }
            Spacer(Modifier.height(24.dp))

            FieldLabel(stringResource(R.string.create_event_name))
            FormField(state.name, viewModel::onName, 52.dp)
            Counter(state.name.length, viewModel.maxName)
            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.common_description), style = HitbosssType.bodySmallEmphasis, color = Gray800, modifier = Modifier.padding(bottom = 8.dp))
            FormField(state.description, viewModel::onDescription, 152.dp, single = false)
            Counter(state.description.length, viewModel.maxDescription)
            Spacer(Modifier.height(16.dp))

            // Visibilidad (público / privado), igual que iOS.
            Text(stringResource(R.string.visibility_title), style = HitbosssType.bodySmallEmphasis, color = Gray800, modifier = Modifier.padding(bottom = 8.dp))
            Text(stringResource(R.string.visibility_desc_event), style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 8.dp))
            VisibilitySegment(state.isPublic, viewModel::onVisibility)
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.create_event_duration), style = HitbosssType.bodySmallEmphasis, color = Gray800)
            Spacer(Modifier.height(8.dp))
            DateRow(stringResource(R.string.create_event_from), state.startMillis) { pickerFor = "start" }
            Spacer(Modifier.height(8.dp))
            DateRow(stringResource(R.string.create_event_to), state.endMillis) { pickerFor = "end" }
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.common_exercises), style = HitbosssType.bodySmallEmphasis, color = Gray800)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.create_event_exercises_desc), style = HitbosssType.bodySmallRegular, color = Gray500)
            Spacer(Modifier.height(16.dp))
            Sport.entries.forEach { sport ->
                Text(sport.title.uppercase(), style = HitbosssType.bodySmallRegular, color = Gray500)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Exercise.forSport(sport).forEach { ex ->
                        val sel = ex in state.exercises
                        Text(
                            stringResource(ex.titleRes()), style = HitbosssType.bodyDefaultRegular, color = Gray800,
                            modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(Gray100)
                                .border(1.dp, if (sel) Secondary500 else Gray300, RoundedCornerShape(32.dp))
                                .clickable { viewModel.toggleExercise(ex) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
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
            else Text(stringResource(R.string.create_event_button), style = HitbosssType.bodyLargeEmphasis, color = if (enabled) Gray100 else Gray500)
        }
    }

    pickerFor?.let { which ->
        EventDatePicker(
            initial = if (which == "start") state.startMillis else state.endMillis,
            onPick = { millis -> if (which == "start") viewModel.onStart(millis) else viewModel.onEnd(millis); pickerFor = null },
            onDismiss = { pickerFor = null },
        )
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

