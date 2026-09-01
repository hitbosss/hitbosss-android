package com.hitbosss.presentation.feature.hit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.domain.model.SavedHit
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Close
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.components.formatEpochDate

/** stringResource(R.string.hit_saved_list) — lista los HITs guardados localmente, permite borrarlos y reintentar la subida. */
@Composable
fun SavedHitsScreen(
    onBack: () -> Unit,
    viewModel: SavedHitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var viewingHit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SavedHit?>(null) }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.hit_saved_list), onBack = onBack)
        if (state.isUploading) {
            // iOS #649: sin overlay bloqueante; la subida corre en el HitUploadManager (notificación)
            // y aquí solo se ve una barra de progreso. Se puede salir de la pantalla sin cortar nada.
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.upload_notif_title), style = HitbosssType.bodySmallRegular, color = Gray800)
                    Spacer(Modifier.weight(1f))
                    Text("${(state.progress * 100).toInt()}%", style = HitbosssType.bodySmallRegular, color = Gray800)
                }
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { state.progress },
                    color = Primary500,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            Text(
                stringResource(R.string.saved_hits_desc),
                style = HitbosssType.bodySmallRegular, color = Gray500,
            )
        }

        if (state.hits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.saved_hits_empty), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
        } else {
            // Segmentado por contexto (Ranking / Grupos / Eventos), igual que iOS.
            SavedHitsFilterSegment(state.selectedFilter, viewModel::setFilter)
            val filtered = state.filteredHits
            if (filtered.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.saved_hits_empty), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { hit ->
                        SavedHitRow(
                            hit = hit,
                            title = viewModel.exerciseTitle(hit.exercise),
                            selected = hit.id == state.selectedId,
                            onSelect = { viewModel.select(hit.id) },
                            onView = {
                                viewModel.select(hit.id)
                                if (viewModel.videoUri(hit) != null) viewingHit = hit else viewModel.showError()
                            },
                            onDelete = { viewModel.askDelete(hit) },
                        )
                    }
                }
                HitButton(
                    stringResource(R.string.hit_upload),
                    onClick = viewModel::uploadSelected,
                    type = HitButtonType.Primary,
                    enabled = state.selectedId != null && !state.isUploading,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }


    state.deleteTarget?.let {
        HitPopup(
            title = stringResource(R.string.hit_delete_confirm_title),
            message = stringResource(R.string.hit_delete_confirm_msg),
            confirmText = stringResource(R.string.common_delete),
            confirmType = HitButtonType.Destructive,
            onConfirm = viewModel::confirmDelete,
            cancelText = stringResource(R.string.common_cancel),
            onCancel = viewModel::dismissDelete,
            onDismissRequest = viewModel::dismissDelete,
        )
    }

    if (state.success) {
        HitPopup(
            title = stringResource(R.string.hit_success_title),
            message = stringResource(R.string.hit_success_msg),
            icon = painterResource(R.drawable.im_icon_success),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::dismissSuccess,
            onDismissRequest = viewModel::dismissSuccess,
        )
    }

    if (state.error) {
        HitPopup(
            title = stringResource(R.string.common_unexpected_error),
            message = stringResource(R.string.common_unexpected_error_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::dismissError,
            onDismissRequest = viewModel::dismissError,
        )
    }

    // "Ver HIT": reproduce el vídeo local a pantalla completa.
    viewingHit?.let { hit ->
        val uri = viewModel.videoUri(hit)
        if (uri == null) {
            viewingHit = null
        } else {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { viewingHit = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(Modifier.fillMaxSize().background(Secondary800)) {
                    com.hitbosss.presentation.feature.ranking.VideoPlayer(url = uri, seekSeconds = 0.0, isActive = true)
                    Box(
                        Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopEnd).size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape).background(Gray100)
                            .border(1.dp, Gray300, androidx.compose.foundation.shape.CircleShape)
                            .clickable { viewingHit = null },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Gray800, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedHitRow(
    hit: SavedHit,
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit,
) {
    val isPl = !hit.sport.contains("cross", true)
    val sportColor = if (isPl) Secondary500 else Error500
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray100)
            .border(1.dp, if (selected) Primary500 else Gray300, RoundedCornerShape(8.dp))
            .clickable { onSelect() }.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        // Deporte + fecha
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(
                painterResource(if (isPl) R.drawable.im_icon_powerlifting else R.drawable.im_icon_crossfit),
                contentDescription = null, modifier = Modifier.size(24.dp),
            )
            Text(if (isPl) "POWERLIFTING" else "CROSSHIT", style = HitbosssType.bodyDefaultRegular, color = sportColor)
            Spacer(Modifier.weight(1f))
            Text(formatEpochDate(hit.createdAt), style = HitbosssType.bodyDefaultRegular, color = Gray500)
        }
        Spacer(Modifier.size(16.dp))
        // Ejercicio + peso (alineados a la línea base, igual que iOS #627)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(title, style = HitbosssType.bodyLargeRegular, color = Gray800, modifier = Modifier.weight(1f))
            Text("${"%.1f".format(hit.lift)} ${hit.unit.uppercase()}", style = HitbosssType.titleBody, color = Gray800)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300))
        Spacer(Modifier.height(12.dp))
        // Ver HIT + eliminar
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HitButton(
                stringResource(R.string.saved_hit_view),
                onClick = onView,
                type = HitButtonType.Secondary,
                size = com.hitbosss.presentation.designsystem.components.HitButtonSize.Medium,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Gray300, RoundedCornerShape(8.dp))
                    .clickable { onDelete() }.padding(horizontal = 17.dp, vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.hit_delete), tint = Gray500, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SavedHitsFilterSegment(selected: SavedHitsFilter, onSelect: (SavedHitsFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(10.dp))
            .background(Gray300).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SavedHitsFilter.entries.forEach { tab ->
            val isSel = selected == tab
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) Gray100 else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(tab) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(tab.label),
                    style = if (isSel) HitbosssType.bodyDefaultEmphasis else HitbosssType.bodyDefaultRegular,
                    color = if (isSel) Gray800 else Gray500,
                )
            }
        }
    }
}

