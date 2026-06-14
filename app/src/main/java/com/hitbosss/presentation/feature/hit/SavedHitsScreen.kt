package com.hitbosss.presentation.feature.hit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.DeleteOutline

/** stringResource(R.string.hit_saved_list) — lista los HITs guardados localmente, permite borrarlos y reintentar la subida. */
@Composable
fun SavedHitsScreen(
    onBack: () -> Unit,
    viewModel: SavedHitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.hit_saved_list), onBack = onBack)
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
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.hits, key = { it.id }) { hit ->
                    SavedHitRow(
                        hit = hit,
                        title = viewModel.exerciseTitle(hit.exercise),
                        selected = hit.id == state.selectedId,
                        onSelect = { viewModel.select(hit.id) },
                        onDelete = { viewModel.askDelete(hit) },
                    )
                }
            }
            HitButton(
                stringResource(R.string.hit_upload),
                onClick = viewModel::uploadSelected,
                type = HitButtonType.Primary,
                enabled = state.selectedId != null,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (state.isUploading) {
        Box(
            Modifier.fillMaxSize().background(Secondary800.copy(alpha = 0.9f)).padding(horizontal = 60.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.hit_uploading_progress), style = HitbosssType.titleSubsection, color = Gray800)
                CircularProgressIndicator(progress = { state.progress }, color = Primary500)
                Text("${(state.progress * 100).toInt()}%", style = HitbosssType.titleSection, color = Gray800)
                HitButton("Cancelar", onClick = viewModel::cancelUpload, type = HitButtonType.Tertiary, size = com.hitbosss.presentation.designsystem.components.HitButtonSize.Medium)
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
}

@Composable
private fun SavedHitRow(
    hit: SavedHit,
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val isPl = !hit.sport.contains("cross", true)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Gray100)
            .border(if (selected) 2.dp else 1.dp, if (selected) Primary500 else Gray300, RoundedCornerShape(10.dp))
            .clickable { onSelect() }.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painterResource(if (isPl) R.drawable.im_icon_powerlifting else R.drawable.im_icon_crossfit),
                contentDescription = null, modifier = Modifier.size(20.dp),
            )
            Text(if (isPl) "POWERLIFTING" else "CROSSHIT", style = HitbosssType.bodySmallRegular, color = Gray800)
            Spacer(Modifier.weight(1f))
            Text(formatSavedDate(hit.createdAt), style = HitbosssType.bodySmallRegular, color = Gray500)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = HitbosssType.titleBody, color = Gray800, modifier = Modifier.weight(1f))
            Text("${"%.1f".format(hit.lift)} ${hit.unit.uppercase()}", style = HitbosssType.titleBody, color = Gray800)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Icon(
                Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.hit_delete), tint = Error500,
                modifier = Modifier.size(24.dp).clickable { onDelete() },
            )
        }
    }
}

private fun formatSavedDate(epochSec: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochSec * 1000))
