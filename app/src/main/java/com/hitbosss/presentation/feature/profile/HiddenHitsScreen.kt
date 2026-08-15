package com.hitbosss.presentation.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.domain.model.Participation
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.feature.hit.HitVideoDialog

@Composable
fun HiddenHitsScreen(
    onBack: () -> Unit,
    viewModel: HiddenHitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    var sport by remember { mutableStateOf(SportFilter.Todos) }
    var optionsFor by remember { mutableStateOf<Participation?>(null) }   // hit con menú abierto (long-press)
    var confirmRestore by remember { mutableStateOf<Participation?>(null) }
    var confirmDelete by remember { mutableStateOf<Participation?>(null) }
    var viewer by remember { mutableStateOf<Pair<List<com.hitbosss.presentation.feature.hit.HitVideoData>, Int>?>(null) }

    val filtered = state.hits.filter {
        when (sport) {
            SportFilter.Todos -> true
            SportFilter.Powerlifting -> it.sport.equals("powerlifting", true)
            SportFilter.Crossfit -> it.sport.equals("crossfit", true)
        }
    }

    Box(Modifier.fillMaxSize().background(Gray200)) {
        Column(Modifier.fillMaxSize()) {
            HitTopBar(title = stringResource(R.string.hidden_hits_title), onBack = onBack)

            // Píldoras de deporte (idénticas a las del perfil).
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SportFilter.entries.forEach { s ->
                    val sel = s == sport
                    Text(
                        stringResource(s.label), style = HitbosssType.bodyDefaultRegular, color = Gray800,
                        modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(Gray100)
                            .border(1.dp, if (sel) Secondary500 else Gray300, RoundedCornerShape(32.dp))
                            .clickable { sport = s }.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.hidden_hits_empty),
                        style = HitbosssType.titleBody, color = Gray500, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                val exTitle = rememberExerciseTitleResolver()
                val videoList = filtered.map { it.toHitVideo(exTitle(it.exercise)) }
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    filtered.chunked(3).forEachIndexed { rowIndex, rowItems ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowItems.forEachIndexed { colIndex, hit ->
                                val globalIndex = rowIndex * 3 + colIndex
                                Box(Modifier.weight(1f)) {
                                    HitThumb(hit, isBest = false, onLongPress = { optionsFor = hit }) {
                                        viewer = videoList to globalIndex
                                    }
                                }
                            }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        if (state.isLoading || state.isProcessing) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gray100)
            }
        }
    }

    // Menú long-press: Restaurar / Eliminar / Cancelar.
    optionsFor?.let { hit ->
        HiddenHitOptionsSheet(
            onRestore = { optionsFor = null; confirmRestore = hit },
            onDelete = { optionsFor = null; confirmDelete = hit },
            onDismiss = { optionsFor = null },
        )
    }

    confirmRestore?.let { hit ->
        HitPopup(
            title = stringResource(R.string.hit_restore_confirm_title),
            message = stringResource(R.string.hit_restore_confirm_msg),
            confirmText = stringResource(R.string.hit_restore),
            confirmType = HitButtonType.Primary,
            onConfirm = { val id = hit.hitId; confirmRestore = null; id?.let { viewModel.restore(it) } },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { confirmRestore = null },
            onDismissRequest = { confirmRestore = null },
        )
    }

    confirmDelete?.let { hit ->
        HitPopup(
            title = stringResource(R.string.hit_delete_hidden_confirm_title),
            message = stringResource(R.string.hit_delete_hidden_confirm_msg),
            confirmText = stringResource(R.string.common_delete),
            confirmType = HitButtonType.Destructive,
            onConfirm = { val id = hit.hitId; confirmDelete = null; id?.let { viewModel.delete(it) } },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { confirmDelete = null },
            onDismissRequest = { confirmDelete = null },
        )
    }

    viewer?.let { (list, idx) -> HitVideoDialog(hits = list, initialPage = idx) { viewer = null } }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HiddenHitOptionsSheet(onRestore: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.hit_restore),
                style = HitbosssType.bodyLargeRegular, color = Secondary500, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onRestore() }.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            Text(
                stringResource(R.string.common_delete),
                style = HitbosssType.bodyLargeRegular,
                color = com.hitbosss.presentation.designsystem.theme.Error500, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onDelete() }.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            Text(
                stringResource(R.string.common_cancel),
                style = HitbosssType.bodyLargeEmphasis, color = Gray800, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}
