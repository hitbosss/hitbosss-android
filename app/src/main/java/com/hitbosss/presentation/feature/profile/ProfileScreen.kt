package com.hitbosss.presentation.feature.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.hitbosss.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.hitbosss.domain.model.Participation
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.Sport
import com.hitbosss.domain.model.UserProfile
import com.hitbosss.presentation.feature.hit.HitVideoData
import com.hitbosss.presentation.feature.hit.HitVideoDialog
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray700
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.feature.ranking.countryFlag
import com.hitbosss.presentation.feature.ranking.levelStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import com.hitbosss.presentation.designsystem.components.HitPopup
import androidx.compose.foundation.combinedClickable
import com.hitbosss.presentation.feature.ranking.titleRes

private enum class MediaTab(@androidx.annotation.StringRes val label: Int) { Marcas(R.string.profile_tab_marks), Hits(R.string.profile_tab_hits) }
private enum class RecordTab(@androidx.annotation.StringRes val label: Int) { Ranking(R.string.tab_ranking), Grupos(R.string.community_tab_groups), Eventos(R.string.community_tab_events) }

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onBack: () -> Unit = {},
    onEditHit: (EditHitNav) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // HIT seleccionado por long-press (solo perfil propio) → menú Editar/Eliminar.
    var menuHit by remember { mutableStateOf<Participation?>(null) }
    // HIT pendiente de confirmar borrado (popup de confirmación antes de eliminar).
    var confirmDeleteHit by remember { mutableStateOf<Participation?>(null) }
    // Menú ⋮ del perfil ajeno (Compartir / Denunciar) y diálogo de denuncia.
    var showProfileMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val shareContext = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) { viewModel.editEvents.collect { onEditHit(it) } }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize().background(Gray200), Alignment.Center) {
            CircularProgressIndicator(color = Primary500)
        }

        state.error != null -> Box(Modifier.fillMaxSize().background(Gray200), Alignment.Center) {
            com.hitbosss.presentation.designsystem.components.ErrorConnectionView(onRetry = viewModel::load)
        }

        state.profile != null -> ProfileContent(
            state.profile!!, state.isOtherUser, state.rankings, state.isRefreshing, viewModel::refresh, onOpenSettings, onBack,
            onHitLongPress = if (!state.isOtherUser) ({ menuHit = it }) else null,
            onOpenProfileMenu = if (state.isOtherUser) ({ showProfileMenu = true }) else null,
            onEditProfile = onEditProfile,
        )
    }

    // Menú del perfil ajeno: Compartir / Denunciar / Cancelar (Figma "Denunciar el perfil de otro usuario").
    if (showProfileMenu) {
        ProfileMenuSheet(
            onShare = {
                showProfileMenu = false
                state.profile?.id?.let { shareProfile(shareContext, it) }
            },
            onReport = { showProfileMenu = false; showReportDialog = true },
            onDismiss = { showProfileMenu = false },
        )
    }

    if (showReportDialog) {
        com.hitbosss.presentation.feature.hit.ReportDialog(
            title = stringResource(R.string.profile_report_title),
            message = stringResource(R.string.profile_report_msg),
            onConfirm = { comment -> showReportDialog = false; viewModel.reportUser(comment) },
            onDismiss = { showReportDialog = false },
        )
    }

    if (state.reportSent) {
        HitPopup(
            title = stringResource(R.string.report_sent_title),
            message = stringResource(R.string.report_sent_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearReportResult,
            onDismissRequest = viewModel::clearReportResult,
        )
    }
    if (state.reportFailed) {
        HitPopup(
            title = stringResource(R.string.common_unexpected_error),
            message = stringResource(R.string.common_unexpected_error_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearReportResult,
            onDismissRequest = viewModel::clearReportResult,
        )
    }

    // Menú de acciones del HIT (1:1 con el action sheet de iOS: Editar / Eliminar).
    menuHit?.let { hit ->
        HitActionSheet(
            onEdit = { menuHit = null; viewModel.startEditHit(hit) },
            onDelete = { menuHit = null; confirmDeleteHit = hit },
            onDismiss = { menuHit = null },
        )
    }

    // Confirmación antes de eliminar un HIT.
    confirmDeleteHit?.let { hit ->
        HitPopup(
            title = stringResource(R.string.hit_delete_confirm_title),
            message = stringResource(R.string.hit_delete_confirm_msg),
            icon = painterResource(R.drawable.im_ico_trash),
            confirmText = stringResource(R.string.hit_delete),
            confirmType = com.hitbosss.presentation.designsystem.components.HitButtonType.Destructive,
            onConfirm = { val id = hit.hitId; confirmDeleteHit = null; id?.let { viewModel.deleteHit(it) } },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { confirmDeleteHit = null },
            onDismissRequest = { confirmDeleteHit = null },
        )
    }

    // Overlay mientras se borra / prepara la edición.
    if (state.isProcessingHit) {
        Box(Modifier.fillMaxSize().background(Secondary800.copy(alpha = 0.4f)), Alignment.Center) {
            CircularProgressIndicator(color = Gray100)
        }
    }

    // Error de borrar/editar (1:1 con iOS: popup "Error inesperado").
    state.actionError?.let {
        HitPopup(
            title = stringResource(R.string.common_unexpected_error),
            message = stringResource(R.string.common_unexpected_error_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearActionError,
            onDismissRequest = viewModel::clearActionError,
        )
    }
}

/** Action sheet inferior con Editar HIT / Eliminar HIT (equivalente al confirmationDialog de iOS). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HitActionSheet(onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.hit_edit),
                style = HitbosssType.bodyLargeRegular, color = Gray800,
                modifier = Modifier.fillMaxWidth().clickable { onEdit() }.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            Text(
                stringResource(R.string.hit_delete),
                style = HitbosssType.bodyLargeRegular, color = Error500,
                modifier = Modifier.fillMaxWidth().clickable { onDelete() }.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

/** Menú ⋮ del perfil ajeno: Compartir perfil / Denunciar perfil / Cancelar (1:1 con el Figma). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProfileMenuSheet(onShare: () -> Unit, onReport: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.profile_share_action),
                style = HitbosssType.bodyLargeRegular, color = Secondary500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onShare() }.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            Text(
                stringResource(R.string.profile_report_action),
                style = HitbosssType.bodyLargeRegular, color = Error500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onReport() }.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            Text(
                stringResource(R.string.common_cancel),
                style = HitbosssType.bodyLargeEmphasis, color = Gray800,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    p: UserProfile,
    isOtherUser: Boolean,
    rankings: Map<String, com.hitbosss.domain.model.SportRanking>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    onHitLongPress: ((Participation) -> Unit)? = null,
    onOpenProfileMenu: (() -> Unit)? = null,
    onEditProfile: () -> Unit = {},
) {
    var media by rememberSaveable { mutableStateOf(MediaTab.Marcas) }
    var record by rememberSaveable { mutableStateOf(RecordTab.Ranking) }
    // Visor de hits: lista a paginar + índice inicial (paginador con puntos, como iOS).
    var viewer by remember { mutableStateOf<Pair<List<HitVideoData>, Int>?>(null) }
    val openViewer: (List<HitVideoData>, Int) -> Unit = { list, idx -> viewer = list to idx }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
    LazyColumn(Modifier.fillMaxSize().background(Gray200)) {
        // Cabecera stringResource(R.string.common_back) solo en perfil ajeno (CustomNavigationHeader de iOS)
        if (isOtherUser) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Gray100).statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Gray800,
                        modifier = Modifier.size(24.dp).clickable { onBack() },
                    )
                    Text(stringResource(R.string.common_back), style = HitbosssType.titleSubsection, color = Gray800)
                    Spacer(Modifier.weight(1f))
                    if (onOpenProfileMenu != null) {
                        Icon(
                            Icons.Filled.MoreVert, contentDescription = stringResource(R.string.common_options), tint = Gray800,
                            modifier = Modifier.size(24.dp).clickable { onOpenProfileMenu() },
                        )
                    }
                }
            }
        }
        item { ProfileHeader(p, isOtherUser, onOpenSettings, onEditProfile) }
        item { MediaTabs(media) { media = it } }

        when (media) {
            MediaTab.Marcas -> {
                item {
                    Spacer(Modifier.height(16.dp))
                    RecordSegment(record) { record = it }
                    Spacer(Modifier.height(16.dp))
                }
                when (record) {
                    RecordTab.Ranking -> {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SportMarksCard(Sport.Powerlifting, p.participations, p.id, rankings["powerlifting"], openViewer)
                                SportMarksCard(Sport.Crossfit, p.participations, p.id, rankings["crossfit"], openViewer)
                            }
                        }
                    }
                    RecordTab.Grupos -> if (p.groups.isEmpty()) {
                        item {
                            EmptyState(
                                if (isOtherUser) stringResource(R.string.profile_empty_groups_other)
                                else stringResource(R.string.profile_empty_groups_mine),
                                R.drawable.im_empty_group,
                            )
                        }
                    } else {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                p.groups.forEach { group -> ProfileGroupCard(group, openViewer) }
                            }
                        }
                    }
                    RecordTab.Eventos -> if (p.events.isEmpty()) {
                        item {
                            EmptyState(
                                if (isOtherUser) stringResource(R.string.profile_empty_events_other)
                                else stringResource(R.string.profile_empty_events_mine),
                                R.drawable.im_empty_event,
                            )
                        }
                    } else {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                p.events.forEach { event -> ProfileEventCard(event, openViewer) }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            MediaTab.Hits -> item { HitsSection(p.participations, openViewer, onHitLongPress) }
        }
    }
    }

    // Visor del vídeo del HIT con paginador (al tocar una marca / hit de grupo / evento / miniatura).
    viewer?.let { (list, idx) -> HitVideoDialog(hits = list, initialPage = idx) { viewer = null } }
}

// MARK: - Cabecera

@Composable
private fun ProfileHeader(p: UserProfile, isOtherUser: Boolean, onOpenSettings: () -> Unit, onEditProfile: () -> Unit = {}) {
    var enlarged by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(Gray100)) {
        Box(Modifier.fillMaxWidth()) {
            AsyncImage(
                model = p.coverPicUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(211.dp).background(Gray400),
            )
            // Botones compartir + ajustes (círculos blancos, arriba-derecha) — solo en perfil propio
            if (!isOtherUser) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Acceso directo a Editar perfil (Figma 8), sin pasar por Ajustes.
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Gray100).clickable { onEditProfile() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.settings_edit_profile), tint = Gray500, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Gray100).clickable { shareProfile(context, p.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.IosShare, contentDescription = stringResource(R.string.profile_share), tint = Gray500, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Gray100).clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title), tint = Gray500, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Avatar cuadrado redondeado, centrado, solapando la portada. Al tocarlo se amplía.
        Box(Modifier.fillMaxWidth().offset(y = (-50).dp), contentAlignment = Alignment.TopCenter) {
            AsyncImage(
                model = p.profilePicUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Gray200)
                    .clickable(enabled = !p.profilePicUrl.isNullOrBlank()) { enlarged = true },
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().offset(y = (-42).dp).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                p.fullName.ifBlank { p.username },
                style = HitbosssType.titleSubsection, color = Gray800, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("@${p.username}", style = HitbosssType.bodySmallRegular, color = Gray500)
                p.countryCode?.takeIf { it.isNotBlank() }?.let { code ->
                    Row(
                        modifier = Modifier.clip(CircleShape).border(1.dp, Gray300, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(countryFlag(code), style = HitbosssType.bodySmallRegular)
                        Text(code.uppercase(), style = HitbosssType.bodySmallRegular, color = Gray800)
                    }
                }
            }
            p.description?.takeIf { it.isNotBlank() }?.let { desc ->
                var expanded by remember { mutableStateOf(false) }
                Spacer(Modifier.height(8.dp))
                Text(
                    desc, style = HitbosssType.bodyDefaultRegular, color = Gray800, textAlign = TextAlign.Center,
                    maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 34.dp).clickable { expanded = !expanded },
                )
                if (desc.length > 90) {
                    Text(
                        if (expanded) stringResource(R.string.common_see_less) else stringResource(R.string.common_see_more),
                        style = HitbosssType.bodySmallEmphasis, color = Secondary500,
                        modifier = Modifier.clickable { expanded = !expanded },
                    )
                }
            }
            // Redes sociales: iconos de marca de iOS (28x28), abren la URL.
            if (p.socialNetworks.isNotEmpty()) {
                val context = LocalContext.current
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    p.socialNetworks.filter { it.url.isNotBlank() }.forEach { sn ->
                        socialIcon(sn.name)?.let { icon ->
                            Image(
                                painterResource(icon),
                                contentDescription = sn.name,
                                modifier = Modifier.size(28.dp).clickable {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sn.url)))
                                    }
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // Visor a pantalla completa de la foto de perfil (cierra al tocar), con un suave zoom de entrada.
    if (enlarged && !p.profilePicUrl.isNullOrBlank()) {
        EnlargedPhotoOverlay(p.profilePicUrl, onDismiss = { enlarged = false })
    }
}

/** Overlay que muestra la foto de perfil ampliada sobre fondo oscuro; se cierra al tocar. */
@Composable
private fun EnlargedPhotoOverlay(url: String?, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var shown by remember { mutableStateOf(false) }
        val scale by androidx.compose.animation.core.animateFloatAsState(if (shown) 1f else 0.85f, label = "photoZoom")
        androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url, contentDescription = null, contentScale = ContentScale.Fit,
                placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                modifier = Modifier.fillMaxWidth().padding(24.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
    }
}

// MARK: - Tabs Marcas / HITS

@Composable
private fun MediaTabs(selected: MediaTab, onSelect: (MediaTab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Gray100).padding(horizontal = 16.dp)) {
        MediaTab.entries.forEach { tab ->
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(tab) }.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(tab.label), style = HitbosssType.bodyLargeRegular, color = Gray800)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(2.dp)
                        .background(if (selected == tab) Secondary500 else Color.Transparent),
                )
            }
        }
    }
}

// MARK: - Segmento Ranking / Grupos / Eventos

@Composable
private fun RecordSegment(selected: RecordTab, onSelect: (RecordTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp))
            .background(Gray300).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RecordTab.entries.forEach { tab ->
            val isSel = selected == tab
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) Gray100 else Color.Transparent)
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

// MARK: - Tarjeta de marcas por deporte

@Composable
private fun SportMarksCard(
    sport: Sport,
    participations: List<Participation>,
    userId: String,
    ranking: com.hitbosss.domain.model.SportRanking?,
    onHitClick: (List<HitVideoData>, Int) -> Unit,
) {
    val isPl = sport == Sport.Powerlifting
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100)) {
        // Cabecera deporte
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isPl) Secondary500 else Error500),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPl) Icons.Filled.FitnessCenter else Icons.Filled.LocalFireDepartment,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp),
                )
            }
            Text(if (isPl) "Powerlifting" else "CrossHIT", style = HitbosssType.titleBody, color = Gray800)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray400))

        val exercises = RankingCategory.forSport(sport)
            .filter { it != RankingCategory.PlOfficial && it != RankingCategory.CfOfficial }
        // Mejor marca por ejercicio + lista paginable de las que tienen vídeo.
        val bestByCat = exercises.associateWith { cat ->
            participations.filter { it.exercise.equals(cat.apiKey, ignoreCase = true) }
                .maxByOrNull { it.maxLift?.value ?: 0.0 }
        }
        val exTitle = rememberExerciseTitleResolver()
        val videoPairs = exercises.mapNotNull { cat ->
            bestByCat[cat]?.takeIf { !it.videoUrl.isNullOrBlank() }?.let { cat to it.toHitVideo(exTitle(cat.apiKey)) }
        }
        val videoList = videoPairs.map { it.second }
        exercises.forEach { cat ->
            val part = bestByCat[cat]
            val total = ranking?.byCategory?.get(cat)?.size
            val idx = videoPairs.indexOfFirst { it.first == cat }
            val clickable: (() -> Unit)? = if (idx >= 0) ({ onHitClick(videoList, idx) }) else null
            ExerciseMarkRow(
                name = exTitle(cat.apiKey),
                weight = part?.maxLift?.let { "${formatWeight(it.value)} ${it.unit}" },
                levelWeight = part?.levelWeight,
                topPercent = topPercentage(part?.position, total),
                isTotal = false,
                onClick = clickable,
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))
        }

        // TOTAL (oficial): viene del ranking oficial, no del perfil.
        val officialCat = if (isPl) RankingCategory.PlOfficial else RankingCategory.CfOfficial
        val officialList = ranking?.byCategory?.get(officialCat)
        val officialEntry = officialList?.firstOrNull { it.userId == userId }
        ExerciseMarkRow(
            name = "TOTAL",
            weight = officialEntry?.lift?.let { "${formatWeight(it.value)} ${it.unit}" },
            levelWeight = officialEntry?.levelWeight,
            topPercent = topPercentage(officialEntry?.rank, officialList?.size),
            isTotal = true,
            onClick = null,
        )
    }
}

@Composable
private fun ExerciseMarkRow(name: String, weight: String?, levelWeight: String?, topPercent: String?, isTotal: Boolean, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = if (isTotal) HitbosssType.bodyDefaultEmphasis else HitbosssType.bodyDefaultRegular,
            color = Gray800,
        )
        Spacer(Modifier.weight(1f))
        if (weight != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                levelStyle(levelWeight)?.let { lvl ->
                    Text(
                        levelBadgeShort(levelWeight), style = HitbosssType.bodySmallRegular, color = lvl.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp).clip(RoundedCornerShape(6.dp)).background(lvl.bg).padding(vertical = 2.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$weight", style = HitbosssType.bodyLargeEmphasis, color = Gray800)
                    topPercent?.let { Text(it, style = HitbosssType.bodySmallRegular, color = Primary500) }
                }
            }
        } else {
            Text(stringResource(R.string.profile_no_mark), style = HitbosssType.bodySmallRegular, color = Gray500)
        }
    }
}

/** TOP% igual que iOS calculateTopPercentage. */
private fun topPercentage(position: Int?, total: Int?): String? {
    if (position == null || position <= 0 || total == null || total <= 0) return null
    if (position == 1) return "TOP 100%"
    val v = ((total - position).toDouble() / total) * 100.0
    return "TOP ${v.toInt()}%"
}

// MARK: - Tarjeta de grupo (perfil)

@Composable
private fun ProfileGroupCard(group: com.hitbosss.domain.model.ProfileGroup, onHitClick: (List<HitVideoData>, Int) -> Unit) {
    val exTitle = rememberExerciseTitleResolver()
    val videoHits = group.userHits.filter { !it.videoUrl.isNullOrBlank() }.map { it.toHitVideo(exTitle(it.exercise)) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100)
            .border(1.dp, Gray300, RoundedCornerShape(12.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(0.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(group.name, style = HitbosssType.titleBody, color = Gray800, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                group.officialSports.forEach { s ->
                    val isPl = s.equals("powerlifting", true)
                    Icon(
                        if (isPl) Icons.Filled.FitnessCenter else Icons.Filled.LocalFireDepartment,
                        contentDescription = null, tint = if (isPl) Secondary500 else Error500, modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray400))
        Column(Modifier.padding(bottom = 16.dp)) {
            group.userHits.forEach { hit -> StatExerciseRow(hit, isTotal = false, videoHits = videoHits, onHitClick = onHitClick) }
        }
    }
}

// MARK: - Tarjeta de evento (perfil)

@Composable
private fun ProfileEventCard(event: com.hitbosss.domain.model.ProfileEvent, onHitClick: (List<HitVideoData>, Int) -> Unit) {
    val exTitle = rememberExerciseTitleResolver()
    val videoHits = event.userHits.filter { !it.videoUrl.isNullOrBlank() }.map { it.toHitVideo(exTitle(it.exercise), event.finalPosition) }
    val isPl = event.sport.equals("powerlifting", true)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100)
            .border(1.dp, Gray300, RoundedCornerShape(12.dp)).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Cabecera: deporte + fecha
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Icon(
                    if (isPl) Icons.Filled.FitnessCenter else Icons.Filled.LocalFireDepartment,
                    contentDescription = null, tint = if (isPl) Secondary500 else Error500, modifier = Modifier.size(24.dp),
                )
                Text(
                    if (isPl) "POWERLIFTING" else "CROSSHIT",
                    style = HitbosssType.bodyDefaultRegular, color = if (isPl) Secondary500 else Error500,
                )
            }
            Row(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Gray400, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(formatShortDate(event.finalizedAt), style = HitbosssType.bodyDefaultRegular, color = Gray800)
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Gray500, modifier = Modifier.size(16.dp))
            }
        }
        Text(event.name.replaceFirstChar { it.uppercase() }, style = HitbosssType.titleBody, color = Gray800, modifier = Modifier.padding(horizontal = 16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray400))

        if (event.userHits.size == 1) {
            StatExerciseRow(event.userHits[0], isTotal = true, fallbackPosition = event.finalPosition, videoHits = videoHits, onHitClick = onHitClick)
        } else {
            event.userHits.forEach { hit ->
                StatExerciseRow(hit, isTotal = false, videoHits = videoHits, onHitClick = onHitClick)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))
            }
            if (event.userHits.isNotEmpty()) {
                val total = event.userHits.sumOf { it.maxLift?.value ?: 0.0 }
                val unit = event.userHits.firstOrNull()?.maxLift?.unit?.uppercase() ?: "KG"
                StatTotalRow("${formatWeight(total)} $unit", event.totalLevel, event.finalPosition)
            }
        }
    }
}

/** Fila de ejercicio en tarjetas de grupo/evento: nombre + peso + badge + #posición. */
@Composable
private fun StatExerciseRow(
    hit: com.hitbosss.domain.model.ProfileHit,
    isTotal: Boolean,
    videoHits: List<HitVideoData>,
    fallbackPosition: Int? = null,
    onHitClick: (List<HitVideoData>, Int) -> Unit,
) {
    val pos = hit.position ?: fallbackPosition
    val weightText = hit.maxLift?.let { "${formatWeight(it.value)} ${it.unit.uppercase()}" } ?: ""
    val exTitle = rememberExerciseTitleResolver()
    val idx = if (hit.videoUrl.isNullOrBlank()) -1 else videoHits.indexOfFirst { it.hitId == hit.hitId && it.exerciseTitle == exTitle(hit.exercise) }
    val onClick: (() -> Unit)? = if (idx >= 0) ({ onHitClick(videoHits, idx) }) else null
    StatRowLayout(
        name = exTitle(hit.exercise),
        weight = weightText,
        levelWeight = hit.levelWeight,
        rankingText = pos?.let { "#$it" } ?: "",
        isTotal = isTotal,
        onClick = onClick,
    )
}

@Composable
private fun StatTotalRow(weight: String, totalLevel: String?, finalPosition: Int?) {
    StatRowLayout(
        name = "TOTAL", weight = weight, levelWeight = totalLevel,
        rankingText = finalPosition?.let { "#$it" } ?: "", isTotal = true, onClick = null,
    )
}

@Composable
private fun StatRowLayout(name: String, weight: String, levelWeight: String?, rankingText: String, isTotal: Boolean, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = if (isTotal) HitbosssType.bodyDefaultEmphasis else HitbosssType.bodyDefaultRegular,
            color = Gray800,
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(weight, style = HitbosssType.bodyDefaultRegular, color = Gray500, modifier = Modifier.width(85.dp), textAlign = TextAlign.End, maxLines = 1)
            levelStyle(levelWeight)?.let { lvl ->
                Text(
                    levelBadgeShort(levelWeight), style = HitbosssType.bodySmallRegular, color = lvl.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(40.dp).clip(RoundedCornerShape(6.dp)).background(lvl.bg).padding(vertical = 2.dp),
                )
            } ?: Spacer(Modifier.width(40.dp))
            Text(
                rankingText,
                style = if (isTotal) HitbosssType.titleBody else HitbosssType.bodyDefaultEmphasis,
                color = Gray800, modifier = Modifier.width(48.dp), textAlign = TextAlign.End,
            )
        }
    }
}

// MARK: - HITS (grid 3 columnas)

private enum class HitTab { Subidos, EnRanking }
private enum class SportFilter(@androidx.annotation.StringRes val label: Int) { Todos(R.string.common_all), Powerlifting(R.string.sport_powerlifting), Crossfit(R.string.sport_crosshit) }

@Composable
private fun HitsSection(
    participations: List<Participation>,
    onHitClick: (List<HitVideoData>, Int) -> Unit,
    onHitLongPress: ((Participation) -> Unit)? = null,
) {
    var tab by rememberSaveable { mutableStateOf(HitTab.Subidos) }
    var sport by rememberSaveable { mutableStateOf(SportFilter.Todos) }

    val withVideo = participations.filter { !it.videoUrl.isNullOrBlank() }
    // "Mejor" hit por ejercicio (el de mayor peso) -> los que aparecen "en ranking".
    val bestPerExercise = withVideo.groupBy { it.exercise.lowercase() }
        .mapNotNull { (_, list) -> list.maxByOrNull { it.maxLift?.value ?: 0.0 } }.toSet()

    val bySport = withVideo.filter {
        when (sport) {
            SportFilter.Todos -> true
            SportFilter.Powerlifting -> it.sport.equals("powerlifting", true)
            SportFilter.Crossfit -> it.sport.equals("crossfit", true)
        }
    }
    val filtered = if (tab == HitTab.EnRanking) bySport.filter { it in bestPerExercise } else bySport

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sub-segmento Subidos / En ranking con conteos
        HitSubSegment(tab, withVideo.size, bestPerExercise.size) { tab = it }
        // Píldoras de deporte
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            val emptyMsg = when (sport) {
                SportFilter.Todos -> stringResource(R.string.profile_empty_hits_all)
                SportFilter.Powerlifting -> stringResource(R.string.profile_empty_hits_pl)
                SportFilter.Crossfit -> stringResource(R.string.profile_empty_hits_cf)
            }
            EmptyState(emptyMsg, R.drawable.im_empty_hits)
        } else {
            // Lista paginable = todos los hits filtrados (en orden del grid).
            val exTitle = rememberExerciseTitleResolver()
            val videoList = filtered.map { it.toHitVideo(exTitle(it.exercise)) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                filtered.chunked(3).forEachIndexed { rowIndex, rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowItems.forEachIndexed { colIndex, hit ->
                            val globalIndex = rowIndex * 3 + colIndex
                            Box(Modifier.weight(1f)) {
                                HitThumb(
                                    hit, isBest = hit in bestPerExercise,
                                    onLongPress = onHitLongPress?.let { lp -> { lp(hit) } },
                                ) { onHitClick(videoList, globalIndex) }
                            }
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HitSubSegment(selected: HitTab, subidos: Int, enRanking: Int, onSelect: (HitTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Gray300).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(HitTab.Subidos to (stringResource(R.string.profile_tab_uploaded) to subidos), HitTab.EnRanking to (stringResource(R.string.profile_tab_on_ranking) to enRanking)).forEach { (t, data) ->
            val sel = t == selected
            Row(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (sel) Gray100 else Color.Transparent).clickable { onSelect(t) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    data.first,
                    style = if (sel) HitbosssType.bodyDefaultEmphasis else HitbosssType.bodyDefaultRegular,
                    color = if (sel) Gray800 else Gray500,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "${data.second}", style = HitbosssType.bodySmallEmphasis,
                    color = if (sel) Gray100 else Gray500,
                    modifier = Modifier.clip(CircleShape).background(if (sel) Secondary500 else Gray400)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HitThumb(hit: Participation, isBest: Boolean, onLongPress: (() -> Unit)? = null, onClick: () -> Unit) {
    val context = LocalContext.current
    val isPl = hit.sport.equals("powerlifting", true)
    val exTitleThumb = rememberExerciseTitleResolver()
    Box(
        Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp)).background(Gray800)
            .let { m ->
                if (onLongPress != null) {
                    m.combinedClickable(onClick = onClick, onLongClick = onLongPress)
                } else {
                    m.clickable { onClick() }
                }
            },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(hit.videoUrl)
                .videoFrameMillis((hit.performedAt * 1000).toLong())
                .decoderFactory(VideoFrameDecoder.Factory()).crossfade(true).build(),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
        // Degradado inferior para legibilidad
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.5f to Color.Transparent, 1f to Gray800.copy(alpha = 0.85f),
                ),
            ),
        )
        // Icono de deporte (caja redondeada arriba-izq)
        Box(
            Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).clip(RoundedCornerShape(8.dp))
                .background(if (isPl) Secondary500 else Error500),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPl) Icons.Filled.FitnessCenter else Icons.Filled.LocalFireDepartment,
                contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp),
            )
        }
        // Estrella "en ranking" (mejor hit) arriba-der
        if (isBest) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(RoundedCornerShape(8.dp))
                    .background(Secondary500),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Leaderboard, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(exTitleThumb(hit.exercise), style = HitbosssType.bodySmallRegular, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            hit.maxLift?.let {
                Text("${formatWeight(it.value)} ${it.unit}", style = HitbosssType.bodyDefaultEmphasis, color = Color.White)
            }
        }
    }
}

/**
 * Resolver de nombre de ejercicio localizado por apiKey. Pre-resuelve los recursos (composable) en un
 * mapa para usarse después dentro de `.map {}` (no-composable) sin romper el matching del visor.
 */
@Composable
private fun rememberExerciseTitleResolver(): (String) -> String {
    val map = RankingCategory.entries.associate { it.apiKey.lowercase() to stringResource(it.titleRes()) }
    return { raw -> map[raw.lowercase()] ?: raw.replaceFirstChar { c -> c.uppercase() } }
}

// MARK: - Vacío

@Composable
private fun EmptyState(message: String, imageRes: Int? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (imageRes != null) {
            Image(painterResource(imageRes), contentDescription = null, modifier = Modifier.size(187.dp))
            Spacer(Modifier.height(16.dp))
        }
        Text(message, style = HitbosssType.titleBody, color = Gray500, textAlign = TextAlign.Center)
    }
}

// MARK: - Helpers

private fun levelBadgeShort(level: String?): String = when (level?.lowercase()) {
    "elite" -> "ÉLT"
    "advanced" -> "AVZ"
    "intermediate" -> "INT"
    "noob" -> "NOV"
    "beginner" -> "PRN"
    else -> ""
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

/** Participation -> HitVideoData (requiere videoUrl no vacío). */
private fun Participation.toHitVideo(title: String) = HitVideoData(
    videoUrl = videoUrl.orEmpty(),
    seekSeconds = performedAt,
    exerciseTitle = title,
    dateText = formatShortDate(createdAt),
    weightText = maxLift?.let { "${formatWeight(it.value)} ${it.unit.uppercase()}" } ?: "",
    levelWeight = levelWeight,
    rankText = position?.let { "#$it" } ?: "",
    pointsText = com.hitbosss.presentation.feature.hit.formatPointsText(wilksScore),
    hitId = hitId,
)

/** ProfileHit -> HitVideoData (requiere videoUrl no vacío). */
private fun com.hitbosss.domain.model.ProfileHit.toHitVideo(title: String, fallbackPosition: Int? = null) = HitVideoData(
    videoUrl = videoUrl.orEmpty(),
    seekSeconds = performedAt,
    exerciseTitle = title,
    dateText = formatShortDate(createdAt),
    weightText = maxLift?.let { "${formatWeight(it.value)} ${it.unit.uppercase()}" } ?: "",
    levelWeight = levelWeight,
    rankText = (position ?: fallbackPosition)?.let { "#$it" } ?: "",
    pointsText = com.hitbosss.presentation.feature.hit.formatPointsText(wilksScore),
    hitId = hitId,
)

/** Icono de marca por red social (mismos assets que iOS: iconInstagram/iconX/iconTikTok/iconFacebook). */
@androidx.annotation.DrawableRes
private fun socialIcon(name: String): Int? = when (name.lowercase()) {
    "facebook" -> R.drawable.im_icon_facebook
    "instagram" -> R.drawable.im_icon_instagram
    "tiktok" -> R.drawable.im_icon_tik_tok
    "twitter", "x" -> R.drawable.im_icon_x
    else -> null
}

private fun formatShortDate(unixSeconds: Long): String =
    if (unixSeconds <= 0) "" else java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
        .format(java.util.Date(unixSeconds * 1000))

/** Comparte el perfil con un deeplink profile/{userId} (1:1 con #637 de iOS). */
private fun shareProfile(context: Context, userId: String) {
    val link = com.hitbosss.core.network.Environment.deeplinkBaseUrl + "profile/$userId"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.profile_share_text, link))
    }
    runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.profile_share))) }
}
