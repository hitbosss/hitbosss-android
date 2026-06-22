package com.hitbosss.presentation.feature.community

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Close
import com.hitbosss.presentation.designsystem.theme.Secondary100
import com.hitbosss.presentation.designsystem.theme.Secondary700
import com.hitbosss.presentation.designsystem.theme.Error100
import com.hitbosss.presentation.designsystem.theme.Error700
import com.hitbosss.presentation.designsystem.theme.Success100
import com.hitbosss.presentation.designsystem.theme.Success500
import com.hitbosss.presentation.designsystem.theme.Warning100
import com.hitbosss.presentation.feature.settings.TutorialViewModel
import com.hitbosss.presentation.feature.settings.rememberTutorialData
import com.hitbosss.presentation.feature.settings.TutorialSectionView
import com.hitbosss.presentation.feature.settings.TutorialThresholdTable
import com.hitbosss.presentation.feature.settings.TutorialVideoThumbnail
import com.hitbosss.presentation.feature.settings.tutorialThumbDrawable
import com.hitbosss.presentation.feature.settings.boldMarkdown
import com.hitbosss.presentation.feature.settings.techniqueText
import com.hitbosss.presentation.feature.settings.recordingText
import com.hitbosss.presentation.feature.settings.thresholds
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.Sport
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.feature.hit.UploadHitFab
import com.hitbosss.presentation.feature.ranking.CurrentUserRow
import com.hitbosss.presentation.feature.ranking.centerItem
import com.hitbosss.presentation.feature.ranking.NoParticipaCard
import com.hitbosss.presentation.feature.ranking.RankingRow
import com.hitbosss.presentation.feature.ranking.RankingSortSheet
import com.hitbosss.presentation.feature.ranking.RankingOrder
import com.hitbosss.presentation.feature.ranking.RequiredExercise
import com.hitbosss.presentation.feature.ranking.countryFlag
import com.hitbosss.presentation.feature.ranking.VideoPlayer
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.presentation.feature.ranking.titleRes

@Composable
fun EventRankingScreen(
    onBack: () -> Unit,
    onOpenMembers: (Int) -> Unit = {},
    onEditEvent: (Int) -> Unit = {},
    onRecordHit: (String, Double) -> Unit = { _, _ -> },
    onSavedHits: () -> Unit = {},
    onTutorial: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val e = state.event
    var selectedTab by rememberSaveable { mutableStateOf(CommunityTabs.Ranking) }
    var showMenu by remember { mutableStateOf(false) }
    var showLeave by remember { mutableStateOf(false) }
    var showAdminBlock by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    LaunchedEffect(state.left, state.deleted) { if (state.left || state.deleted) onBack() }

    Box(Modifier.fillMaxSize().background(Gray100)) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
            e == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { com.hitbosss.presentation.designsystem.components.ErrorConnectionView(onRetry = viewModel::retry) }
            else -> Column(Modifier.fillMaxSize()) {
                // Cabecera del evento: atrás + nombre + menú (sin miniatura; la portada va en el banner).
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Gray800, modifier = Modifier.size(24.dp).clickable { onBack() })
                    Spacer(Modifier.width(12.dp))
                    Text(e.name, style = HitbosssType.titleBody, color = Gray800, modifier = Modifier.weight(1f))
                    Box {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.common_options), tint = Gray800, modifier = Modifier.size(24.dp).clickable { showMenu = true })
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (state.isAdmin && !state.isPast) DropdownMenuItem(text = { Text(stringResource(R.string.event_edit)) }, onClick = { showMenu = false; onEditEvent(e.id) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.event_share)) }, onClick = { showMenu = false; shareEvent(context, e.id) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.event_report), color = Error500) }, onClick = { showMenu = false; showReport = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.event_leave), color = if (state.isAdmin) Gray800 else Error500) }, onClick = { showMenu = false; if (state.canLeaveDirectly) showLeave = true else showAdminBlock = true })
                            if (state.isAdmin) DropdownMenuItem(text = { Text(stringResource(R.string.event_delete), color = Error500) }, onClick = { showMenu = false; showDelete = true })
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))
                // Banner de portada del evento (estado + miembros + ejercicio·deporte).
                EventCoverBanner(e, state.isPast)
                Spacer(Modifier.height(8.dp))
                CommunityTabsBar(selectedTab) { selectedTab = it }
                Spacer(Modifier.height(8.dp))

                Box(Modifier.weight(1f).fillMaxWidth()) {
                  androidx.compose.animation.AnimatedContent(
                      targetState = selectedTab,
                      transitionSpec = { communityTabTransition() },
                      label = "commTab",
                  ) { tab ->
                  when (tab) {
                    CommunityTabs.Ranking -> EventRankingContent(e, state.currentUserId, state.currentUserCountry, state.currentUserPicUrl, onRecordHit, onSavedHits, onTutorial, onOpenUserProfile, state.isAdmin, state.isPast, viewModel::resetHit, viewModel::removeMember)
                    CommunityTabs.Information -> {
                        val visibilityValue = stringResource(if (e.isPublic) R.string.visibility_public else R.string.visibility_private)
                        val exerciseTitle = e.exercises.firstOrNull()?.let { eventExerciseLabel(it) } ?: ""
                        val detailRows = buildList {
                            if (exerciseTitle.isNotBlank()) add(CommunityDetailRowData(Icons.Filled.FitnessCenter, R.string.event_exercise, exerciseTitle))
                            add(CommunityDetailRowData(Icons.Filled.EventAvailable, R.string.detail_start_date, formatDate(e.startTime)))
                            add(CommunityDetailRowData(Icons.Filled.EventBusy, R.string.detail_end_date, formatDate(e.endTime)))
                            e.joinedAt?.let { add(CommunityDetailRowData(Icons.Filled.Flag, R.string.detail_admission_date, formatDate(it))) }
                            add(CommunityDetailRowData(Icons.Filled.Lock, R.string.visibility_title, visibilityValue))
                        }
                        CommunityInformationTab(
                            contextType = CommunityInfoContextType.Event,
                            description = e.description,
                            motto = null,
                            detailsTitle = stringResource(R.string.event_details),
                            detailRows = detailRows,
                            memberCount = e.stats.memberCount,
                            admins = communityAdmins(e.members, e.createdBy?.id),
                            creatorId = e.createdBy?.id,
                            extraInfoIcon = Icons.Filled.Straighten,
                            extraInfoTitle = stringResource(R.string.event_rules),
                            extraInfoCount = null,
                            isExpandable = true,
                            shareTitle = stringResource(R.string.event_share),
                            shareSubtitle = stringResource(R.string.community_share_subtitle),
                            onSeeMembers = { onOpenMembers(e.id) },
                            onShare = { shareEvent(context, e.id) },
                            extraContent = { EventNormasContent(e.exercises) },
                        )
                    }
                  }
                  }
                }
            }
        }
    }

    // Diálogos de acciones (portados de EventDetailScreen).
    if (showLeave) HitPopup(
        title = stringResource(R.string.event_leave_confirm_title), message = stringResource(R.string.event_leave_confirm_msg),
        confirmText = stringResource(R.string.event_leave), confirmType = HitButtonType.Destructive,
        onConfirm = { showLeave = false; viewModel.leave() },
        cancelText = stringResource(R.string.common_cancel), onCancel = { showLeave = false }, onDismissRequest = { showLeave = false },
    )
    if (showAdminBlock) HitPopup(
        title = stringResource(R.string.event_admin_block_title), message = stringResource(R.string.event_admin_block_msg),
        confirmText = stringResource(R.string.common_accept), onConfirm = { showAdminBlock = false }, onDismissRequest = { showAdminBlock = false },
    )
    if (showDelete) HitPopup(
        title = stringResource(R.string.event_delete_confirm_title), message = stringResource(R.string.event_delete_confirm_msg),
        icon = painterResource(R.drawable.im_ico_trash), confirmText = stringResource(R.string.common_accept), confirmType = HitButtonType.Destructive,
        onConfirm = { showDelete = false; viewModel.delete() },
        cancelText = stringResource(R.string.common_cancel), onCancel = { showDelete = false }, onDismissRequest = { showDelete = false },
    )
    if (showReport) com.hitbosss.presentation.feature.hit.ReportDialog(
        title = stringResource(R.string.event_report_title), message = stringResource(R.string.event_report_msg),
        onConfirm = { comment -> showReport = false; viewModel.report(comment) }, onDismiss = { showReport = false },
    )
    if (state.reportSent) HitPopup(
        title = stringResource(R.string.report_sent_title), message = stringResource(R.string.report_sent_msg),
        confirmText = stringResource(R.string.common_accept), onConfirm = viewModel::clearReportResult, onDismissRequest = viewModel::clearReportResult,
    )
    if (state.reportFailed) HitPopup(
        title = stringResource(R.string.common_unexpected_error), message = stringResource(R.string.common_unexpected_error_msg),
        confirmText = stringResource(R.string.common_accept), onConfirm = viewModel::clearReportResult, onDismissRequest = viewModel::clearReportResult,
    )
}

private fun shareEvent(context: Context, eventId: Int) {
    val link = com.hitbosss.core.network.Environment.deeplinkBaseUrl + "event/$eventId"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.event_share_text, link))
    }
    runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.event_share))) }
}

@Composable
private fun EventRankingContent(
    e: EventDetail,
    currentUserId: String?,
    currentUserCountry: String?,
    currentUserPicUrl: String?,
    onRecordHit: (String, Double) -> Unit,
    onSavedHits: () -> Unit,
    onTutorial: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
    isAdmin: Boolean,
    isPast: Boolean,
    onResetHit: (Int) -> Unit,
    onRemoveMember: (String) -> Unit,
) {
    val sportEnum = Sport.entries.firstOrNull { it.apiValue == e.sport } ?: Sport.Powerlifting
    val isPl = sportEnum == Sport.Powerlifting
    val ranking = e.ranking[e.sport]
    val officialCat = if (isPl) RankingCategory.PlOfficial else RankingCategory.CfOfficial
    val exerciseCats = RankingCategory.forSport(sportEnum)
        .filter { it != RankingCategory.PlOfficial && it != RankingCategory.CfOfficial }
        .filter { cat -> e.exercises.any { it.equals(cat.apiKey, true) } }
    val categories = listOf(officialCat) + exerciseCats

    var selected by rememberSaveable { mutableStateOf(officialCat) }
    var search by rememberSaveable { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var order by rememberSaveable { mutableStateOf(RankingOrder.Lift) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    // Si eres admin del evento, tocar un hit abre el menú de acciones (igual que iOS); si no, el visor.
    var actionEntry by remember { mutableStateOf<com.hitbosss.domain.model.RankingEntry?>(null) }

    val sportColor = if (isPl) Secondary500 else Error500
    val isOfficial = selected == officialCat

    val allEntries = ranking?.byCategory?.get(selected).orEmpty()
    val usePoints = order == RankingOrder.Points
    // Re-ordena por el criterio elegido (peso/puntos) con desempate y re-numera la posición, igual
    // que el ranking principal y iOS.
    val entries = allEntries
        .filter { search.isBlank() || it.username.contains(search, true) }
        .sortedWith(
            if (usePoints) compareByDescending<com.hitbosss.domain.model.RankingEntry> { it.score }.thenByDescending { it.lift?.value ?: 0.0 }
            else compareByDescending<com.hitbosss.domain.model.RankingEntry> { it.lift?.value ?: 0.0 }.thenByDescending { it.score },
        )
        .mapIndexed { i, e -> e.copy(rank = i + 1) }
    val currentUserEntry = ranking?.byCategory?.get(officialCat)?.firstOrNull { it.userId == currentUserId }

    fun has(cat: RankingCategory) = ranking?.byCategory?.get(cat)?.any { it.userId == currentUserId } == true
    val required = if (isPl) listOf(
        RequiredExercise(R.string.exercise_squat, has(RankingCategory.Squat)),
        RequiredExercise(R.string.exercise_bench, has(RankingCategory.BenchPress)),
        RequiredExercise(R.string.exercise_deadlift_sumo, has(RankingCategory.Deadlift) || has(RankingCategory.SumoDeadlift)),
    ) else listOf(
        RequiredExercise(R.string.exercise_snatch, has(RankingCategory.Snatch)),
        RequiredExercise(R.string.exercise_clean, has(RankingCategory.Clean)),
        RequiredExercise(R.string.exercise_clean_jerk, has(RankingCategory.CleanAndJerk)),
    )
    val notParticipating = required.count { it.done } < 3
    // Levanta tarjeta "Tú" + FAB del borde inferior (sin barra de navegación que los suba aquí).
    val bottomBase = 80.dp

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Buscador + orden
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF2F2F2)).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (search.isEmpty()) Text(stringResource(R.string.ranking_search), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                        BasicTextField(search, { search = it }, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
                    }
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Gray500)
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Gray100).border(1.dp, Gray300, CircleShape).clickable { showSort = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.ranking_sort), tint = Gray800, modifier = Modifier.size(22.dp)) }
            }
            // Tabs ejercicio
            val tabsState = androidx.compose.foundation.lazy.rememberLazyListState()
            // Centra la categoría seleccionada en el viewport (igual que en el ranking principal).
            androidx.compose.runtime.LaunchedEffect(selected, categories) {
                val idx = categories.indexOf(selected)
                if (idx >= 0) tabsState.centerItem(idx)
            }
            LazyRow(
                state = tabsState,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(categories) { cat ->
                    val sel = cat == selected
                    val label = if (cat == officialCat) sportEnum.brandTitle else stringResource(cat.titleRes())
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selected = cat }) {
                        Text(label, style = HitbosssType.titleBody, color = if (sel) Gray800 else Gray500, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(vertical = 8.dp))
                        Box(Modifier.height(2.dp).width(if (sel) 40.dp else 0.dp).background(sportColor))
                    }
                }
            }

            LazyColumn(
                Modifier.weight(1f).background(Gray300),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isOfficial && notParticipating) {
                    item { NoParticipaCard(sportEnum.title, required) }
                }
                if (entries.isEmpty() && !(isOfficial && notParticipating)) {
                    item { com.hitbosss.presentation.feature.ranking.RankingEmptyState() }
                }
                items(entries) { entry -> RankingRow(entry, order == RankingOrder.Points) { if (isAdmin) actionEntry = entry else selectedUserId = entry.userId } }
            }

            // Padding inferior común para tarjeta "Tú" y FAB (los sube respecto al borde), igual que
            // en el ranking principal. La distancia FAB↔tarjeta se añade aparte sobre ese padding.
            currentUserId?.let { CurrentUserRow(currentUserEntry, currentUserCountry, order == RankingOrder.Points, currentUserPicUrl, Modifier.navigationBarsPadding().padding(bottom = bottomBase)) }
        }

        UploadHitFab(
            sport = selected.sport,
            isOfficial = isOfficial,
            exerciseKey = selected.uploadExerciseKey,
            onRecordHit = onRecordHit,
            onSavedHits = onSavedHits,
            onTutorial = onTutorial,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = bottomBase + 88.dp),
        )
    }

    if (showSort) RankingSortSheet(current = order, onSelect = { order = it }, onDismiss = { showSort = false })

    selectedUserId?.let { uid ->
        if (ranking != null) com.hitbosss.presentation.feature.ranking.UserDetailModal(
            userId = uid,
            ranking = ranking,
            onDismiss = { selectedUserId = null },
            onVisitProfile = { selectedUserId = null; onOpenUserProfile(uid) },
        )
    }

    // Menú de acciones del hit (solo admin de evento), 1:1 con iOS.
    actionEntry?.let { entry ->
        EventHitActionSheet(
            isPast = isPast,
            canReset = entry.hitId != null,
            onProfile = { val id = entry.userId; actionEntry = null; onOpenUserProfile(id) },
            onCheckHit = { selectedUserId = entry.userId; actionEntry = null },
            onResetHit = { entry.hitId?.let { onResetHit(it) }; actionEntry = null },
            onRemove = { val id = entry.userId; actionEntry = null; onRemoveMember(id) },
            onDismiss = { actionEntry = null },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EventHitActionSheet(
    isPast: Boolean,
    canReset: Boolean,
    onProfile: () -> Unit,
    onCheckHit: () -> Unit,
    onResetHit: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(),
        containerColor = Gray100,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
            SheetItem(stringResource(R.string.member_view_profile), Secondary500, onProfile)
            SheetItem(stringResource(R.string.event_check_hit), Secondary500, onCheckHit)
            if (!isPast) {
                if (canReset) SheetItem(stringResource(R.string.event_reset_hit), Error500, onResetHit)
                SheetItem(stringResource(R.string.event_remove_member), Error500, onRemove)
            }
            SheetItem(stringResource(R.string.common_cancel), Gray500, onDismiss)
        }
    }
}

@Composable
private fun SheetItem(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text, style = HitbosssType.bodyLargeEmphasis, color = color,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
    )
}

@Composable
private fun EventHeader(e: EventDetail, sportTitle: String, onBack: () -> Unit, onInfo: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(200.dp)) {
        AsyncImage(model = e.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(Gray400))
        // Degradado para legibilidad
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))

        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Color.White, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.weight(1f))
            // Los tres puntos abren directamente la info del evento (igual que iOS), sin desplegable.
            Box(Modifier.size(32.dp).clip(CircleShape).background(Gray100).clickable { onInfo() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.event_info_title), tint = Gray800, modifier = Modifier.size(20.dp))
            }
        }

        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            // Badge estado
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Secondary500).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(stringResource(eventStatus(e)), style = HitbosssType.bodySmallEmphasis, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            // Fechas
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Gray100).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Gray500, modifier = Modifier.size(16.dp))
                Text("${formatDate(e.startTime)} — ${formatDate(e.endTime)}", style = HitbosssType.bodySmallRegular, color = Gray800)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = HitbosssType.titleSection, color = Color.White)
                    Text(sportTitle.uppercase(), style = HitbosssType.bodySmallEmphasis, color = Color.White)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Gray100).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Gray800, modifier = Modifier.size(16.dp))
                    Text("${e.stats.memberCount}", style = HitbosssType.bodyDefaultRegular, color = Gray800)
                }
            }
        }
    }
}

@androidx.annotation.StringRes
private fun eventStatus(e: EventDetail): Int {
    val now = System.currentTimeMillis()
    return when {
        e.endTime * 1000 < now -> R.string.event_state_finished
        e.startTime * 1000 > now -> R.string.event_state_upcoming
        else -> R.string.event_state_active
    }
}

private fun formatDate(s: Long): String =
    if (s <= 0) "—" else java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(s * 1000))

/** Etiqueta localizada del ejercicio de un evento. */
@Composable
private fun eventExerciseLabel(apiKey: String): String =
    com.hitbosss.presentation.feature.ranking.exerciseTitleResByApi(apiKey)?.let { stringResource(it) }
        ?: apiKey.replaceFirstChar { it.uppercase() }

/** Banner de portada del evento (1:1 con iOS): estado + miembros + ejercicio·deporte sobre la portada. */
@Composable
private fun EventCoverBanner(e: EventDetail, isPast: Boolean) {
    val sport = Sport.entries.firstOrNull { it.apiValue == e.sport } ?: Sport.Powerlifting
    val isPl = sport == Sport.Powerlifting
    val sportColor = if (isPl) Secondary700 else Error700
    val sportName = (if (isPl) "PowerHIT" else "CrossHIT").uppercase()
    val exerciseTitle = e.exercises.firstOrNull()?.let { eventExerciseLabel(it) }?.uppercase() ?: ""
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).height(160.dp)
            .clip(RoundedCornerShape(16.dp)).background(Gray100),
    ) {
        AsyncImage(
            model = e.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop,
            placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
            modifier = Modifier.fillMaxSize(),
        )
        // Franja degradada inferior del color del deporte.
        Box(
            Modifier.fillMaxWidth().height(32.dp).align(Alignment.BottomStart)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(sportColor.copy(alpha = 0.5f), sportColor))),
        )
        // Píldoras superiores: estado (izq) + miembros (der).
        Row(Modifier.fillMaxWidth().align(Alignment.TopStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Gray100).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (isPast) Error500 else Success500))
                Text(stringResource(if (isPast) R.string.event_state_finished else R.string.event_state_active), style = HitbosssType.bodySmallRegular, color = Gray800)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Gray100).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.People, contentDescription = null, tint = Gray500, modifier = Modifier.size(16.dp))
                Text("${e.stats.memberCount}", style = HitbosssType.bodySmallRegular, color = Gray800)
            }
        }
        // Etiqueta inferior: ejercicio · deporte (o solo deporte si oficial).
        Row(Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (e.hasOfficial || exerciseTitle.isBlank()) {
                Text(sportName, style = HitbosssType.bodySmallEmphasis, color = Gray100)
            } else {
                Text(exerciseTitle, style = HitbosssType.bodySmallEmphasis, color = Gray100)
                Text("·", style = HitbosssType.bodySmallEmphasis, color = Gray100)
                Text(sportName, style = HitbosssType.bodySmallRegular, color = Gray100)
            }
        }
    }
}

/**
 * "Normas del evento": chips de ejercicio del evento + el tutorial del ejercicio seleccionado
 * (información+vídeo, técnica, grabación, umbrales). Reutiliza los componentes de tutoriales.
 */
@Composable
private fun EventNormasContent(
    exercises: List<String>,
    tutorialVM: TutorialViewModel = hiltViewModel(),
) {
    val data = rememberTutorialData()
    val isMetric by tutorialVM.isMetric.collectAsStateWithLifecycle()
    var selected by remember(exercises) { mutableStateOf(exercises.firstOrNull().orEmpty()) }
    var showVideo by remember { mutableStateOf(false) }
    val ex = data.firstOrNull { it.apiKey == selected }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Chips de ejercicio del evento.
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            exercises.forEach { apiKey ->
                val sel = apiKey == selected
                val exSport = data.firstOrNull { it.apiKey == apiKey }?.sport ?: "powerlifting"
                val isPl = exSport == "powerlifting"
                val sportColor = if (isPl) Secondary500 else Error500
                val chipBg = if (isPl) Secondary100 else Error100
                Text(
                    eventExerciseLabel(apiKey), style = HitbosssType.bodyDefaultRegular, color = Gray800,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) chipBg else Gray100)
                        .border(1.dp, if (sel) sportColor else Gray300, RoundedCornerShape(50))
                        .clickable { selected = apiKey }.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        ex?.let { exercise ->
            TutorialSectionView(
                R.drawable.ic_tutorial_info, Secondary100,
                stringResource(R.string.tutorial_section_info),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(boldMarkdown(exercise.info), style = HitbosssType.bodySmallRegular, color = Gray500)
                    val thumb = tutorialThumbDrawable(exercise.apiKey)
                    if (thumb != null && exercise.videoUrl != null) {
                        TutorialVideoThumbnail(exercise.title, thumb) { showVideo = true }
                    }
                }
            }
            exercise.techniqueText?.let { technique ->
                TutorialSectionView(
                    R.drawable.ic_tutorial_technique, Success100, stringResource(R.string.tutorial_section_technique),
                ) { Text(boldMarkdown(technique), style = HitbosssType.bodySmallRegular, color = Gray500) }
            }
            exercise.recordingText?.let { recording ->
                TutorialSectionView(
                    R.drawable.ic_tutorial_record, Error100, stringResource(R.string.tutorial_section_recording),
                ) { Text(boldMarkdown(recording), style = HitbosssType.bodySmallRegular, color = Gray500) }
            }
            exercise.thresholds?.let { table ->
                TutorialSectionView(
                    R.drawable.ic_tutorial_thresholds, Warning100, stringResource(R.string.tutorial_section_thresholds),
                ) { TutorialThresholdTable(table, isMetric) }
            }
        }
    }

    if (showVideo && ex?.videoUrl != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showVideo = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Gray800)) {
                VideoPlayer(url = ex.videoUrl!!, seekSeconds = 0.0, isActive = true)
                Box(
                    Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopEnd).size(32.dp).clip(CircleShape)
                        .background(Gray100).border(1.dp, Gray300, CircleShape).clickable { showVideo = false },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Gray800, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
