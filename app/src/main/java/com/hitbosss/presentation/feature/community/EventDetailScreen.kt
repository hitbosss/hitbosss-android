package com.hitbosss.presentation.feature.community

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.Member
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray700
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary100
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    onOpenMembers: () -> Unit = {},
    onEditEvent: () -> Unit = {},
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLeave by remember { mutableStateOf(false) }
    var showAdminBlock by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    LaunchedEffect(state.left, state.deleted) { if (state.left || state.deleted) onBack() }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Gray800, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.event_info_title), style = HitbosssType.titleSubsection, color = Gray800, modifier = Modifier.weight(1f))
            Box {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.common_options), tint = Gray800, modifier = Modifier.size(24.dp).clickable { showMenu = true })
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (state.isAdmin && !state.isPast) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.event_edit)) }, onClick = { showMenu = false; onEditEvent() })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.event_share)) }, onClick = {
                        showMenu = false
                        state.event?.let { ev ->
                            // Enlace de deeplink (equivalente al universal link de iOS): abre el evento
                            // en la app si está instalada, o lleva a la tienda si no.
                            val link = com.hitbosss.core.network.Environment.deeplinkBaseUrl + "event/${ev.id}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.event_share_text, link))
                            }
                            runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.event_share))) }
                        }
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.event_report), color = Error500) }, onClick = { showMenu = false; showReport = true })
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
            state.event == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                com.hitbosss.presentation.designsystem.components.ErrorConnectionView(onRetry = viewModel::retry)
            }
            else -> {
                val e = state.event!!
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = e.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.width(240.dp).height(132.dp).clip(RoundedCornerShape(8.dp)).background(Gray400),
                        )
                    }
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Section(stringResource(R.string.create_event_name)) { Value(e.name) }
                        Section(stringResource(R.string.common_description)) { Value(e.description.orEmpty()) }
                        Section(stringResource(R.string.create_event_duration)) {
                            Value("${formatDate(e.startTime)} — ${formatDate(e.endTime)}")
                            eventEndMessage(e, context)?.let { Text(it, style = HitbosssType.bodySmallRegular, color = Error500) }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.common_members), style = HitbosssType.titleBody, color = Gray800, modifier = Modifier.weight(1f))
                                Text(stringResource(R.string.community_see_list), style = HitbosssType.bodySmallEmphasis, color = Primary500, modifier = Modifier.clickable { onOpenMembers() })
                            }
                            Value("${e.stats.memberCount}")
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.common_exercise), style = HitbosssType.titleBody, color = Gray800)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                e.exercises.forEach { ex ->
                                    Text(
                                        exerciseTitle(ex), style = HitbosssType.bodyDefaultRegular, color = Gray800,
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Secondary100).padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Salir: bloquea solo si es el único admin con otros miembros (igual que iOS).
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (state.isAdmin) Gray700 else Error500)
                            .clickable { if (state.canLeaveDirectly) showLeave = true else showAdminBlock = true }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.event_leave), style = HitbosssType.bodyLargeEmphasis, color = Gray100) }
                    if (state.isAdmin) {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Error500)
                                .clickable { showDelete = true }.padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(stringResource(R.string.event_delete), style = HitbosssType.bodyLargeEmphasis, color = Gray100) }
                    }
                }
            }
        }
    }

    if (showLeave) {
        HitPopup(
            title = stringResource(R.string.event_leave_confirm_title),
            message = stringResource(R.string.event_leave_confirm_msg),
            confirmText = stringResource(R.string.event_leave),
            confirmType = HitButtonType.Destructive,
            onConfirm = { showLeave = false; viewModel.leave() },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showLeave = false },
            onDismissRequest = { showLeave = false },
        )
    }
    if (showAdminBlock) {
        HitPopup(
            title = stringResource(R.string.event_admin_block_title),
            message = stringResource(R.string.event_admin_block_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = { showAdminBlock = false },
            onDismissRequest = { showAdminBlock = false },
        )
    }
    if (showDelete) {
        HitPopup(
            title = stringResource(R.string.event_delete_confirm_title),
            message = stringResource(R.string.event_delete_confirm_msg),
            icon = painterResource(R.drawable.im_ico_trash),
            confirmText = stringResource(R.string.common_accept),
            confirmType = HitButtonType.Destructive,
            onConfirm = { showDelete = false; viewModel.delete() },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showDelete = false },
            onDismissRequest = { showDelete = false },
        )
    }
    if (showReport) com.hitbosss.presentation.feature.hit.ReportDialog(
        title = stringResource(R.string.event_report_title),
        message = stringResource(R.string.event_report_msg),
        onConfirm = { comment -> showReport = false; viewModel.report(comment) },
        onDismiss = { showReport = false },
    )
    // Confirmación tras enviar la denuncia (éxito) / error si falla.
    if (state.reportSent) HitPopup(
        title = stringResource(R.string.report_sent_title),
        message = stringResource(R.string.report_sent_msg),
        confirmText = stringResource(R.string.common_accept),
        onConfirm = viewModel::clearReportResult,
        onDismissRequest = viewModel::clearReportResult,
    )
    if (state.reportFailed) HitPopup(
        title = stringResource(R.string.common_unexpected_error),
        message = stringResource(R.string.common_unexpected_error_msg),
        confirmText = stringResource(R.string.common_accept),
        onConfirm = viewModel::clearReportResult,
        onDismissRequest = viewModel::clearReportResult,
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = HitbosssType.titleBody, color = Gray800)
        content()
    }
}

@Composable
private fun Value(text: String) = Text(text, style = HitbosssType.bodySmallRegular, color = Gray500)

@Composable
private fun exerciseTitle(apiKey: String): String =
    com.hitbosss.presentation.feature.ranking.exerciseTitleResByApi(apiKey)?.let { stringResource(it) }
        ?: apiKey.replaceFirstChar { it.uppercase() }

private fun formatDate(unixSeconds: Long): String =
    if (unixSeconds <= 0) "—" else java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(unixSeconds * 1000))

/** "Faltan N días para que finalice el evento." (igual que iOS eventEndMessage). */
private fun eventEndMessage(e: EventDetail, context: android.content.Context): String? {
    val diff = e.endTime * 1000 - System.currentTimeMillis()
    if (diff <= 0) return null
    val days = (diff / 86_400_000L).toInt()
    return when {
        days > 1 -> context.getString(R.string.event_ends_in_days, days)
        days == 1 -> context.getString(R.string.event_ends_one_day)
        else -> context.getString(R.string.event_ends_today)
    }
}
