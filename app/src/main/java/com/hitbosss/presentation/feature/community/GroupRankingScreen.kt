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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.GroupDetail
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.Sport
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.HitButtonType
import androidx.compose.ui.res.painterResource
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import com.hitbosss.presentation.feature.hit.UploadHitFab
import com.hitbosss.presentation.feature.ranking.CurrentUserRow
import com.hitbosss.presentation.feature.ranking.centerItem
import com.hitbosss.presentation.feature.ranking.LevelFilters
import com.hitbosss.presentation.feature.ranking.NoParticipaCard
import com.hitbosss.presentation.feature.ranking.RankingOrder
import com.hitbosss.presentation.feature.ranking.RankingRow
import com.hitbosss.presentation.feature.ranking.RankingSortSheet
import com.hitbosss.presentation.feature.ranking.RequiredExercise
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.presentation.feature.ranking.titleRes

@Composable
fun GroupRankingScreen(
    onBack: () -> Unit,
    onOpenMembers: (Int) -> Unit = {},
    onEditGroup: (Int) -> Unit = {},
    onRecordHit: (String, Double) -> Unit = { _, _ -> },
    onSavedHits: () -> Unit = {},
    onTutorial: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val g = state.group
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
            g == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { com.hitbosss.presentation.designsystem.components.ErrorConnectionView(onRetry = viewModel::retry) }
            else -> Column(Modifier.fillMaxSize()) {
                // Cabecera compartida: atrás + portada + nombre + menú de acciones.
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Gray800, modifier = Modifier.size(24.dp).clickable { onBack() })
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(model = g.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(), modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Gray300))
                    Spacer(Modifier.width(12.dp))
                    Text(g.name, style = HitbosssType.titleBody, color = Gray800, modifier = Modifier.weight(1f))
                    Box {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.common_options), tint = Gray800, modifier = Modifier.size(24.dp).clickable { showMenu = true })
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (state.isAdmin) DropdownMenuItem(text = { Text(stringResource(R.string.group_edit)) }, onClick = { showMenu = false; onEditGroup(g.id) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.group_share)) }, onClick = { showMenu = false; shareGroup(context, g.id) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.group_report), color = Error500) }, onClick = { showMenu = false; showReport = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.group_leave), color = if (state.isAdmin) Gray800 else Error500) }, onClick = { showMenu = false; if (state.canLeaveDirectly) showLeave = true else showAdminBlock = true })
                            if (state.isAdmin) DropdownMenuItem(text = { Text(stringResource(R.string.group_delete), color = Error500) }, onClick = { showMenu = false; showDelete = true })
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))
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
                    CommunityTabs.Ranking -> GroupRankingContent(g, state.currentUserId, state.currentUserCountry, state.currentUserPicUrl, onRecordHit, onSavedHits, onTutorial, onOpenUserProfile)
                    CommunityTabs.Information -> {
                        val visibilityValue = stringResource(if (g.isPublic) R.string.visibility_public else R.string.visibility_private)
                        val detailRows = buildList {
                            g.createdAt?.let { add(CommunityDetailRowData(Icons.Filled.CalendarToday, R.string.detail_creation_date, formatCommDate(it))) }
                            g.joinedAt?.let { add(CommunityDetailRowData(Icons.Filled.Flag, R.string.detail_admission_date, formatCommDate(it))) }
                            add(CommunityDetailRowData(Icons.Filled.Lock, R.string.visibility_title, visibilityValue))
                        }
                        // Claves disponibles = ejercicios del grupo + oficiales activos.
                        val availableKeys = g.exercises + g.officialSports.mapNotNull {
                            when (it.lowercase()) { "powerlifting" -> "officialPowerlifting"; "crossfit" -> "officialCrossfit"; else -> null }
                        }
                        CommunityInformationTab(
                            contextType = CommunityInfoContextType.Group,
                            description = g.description,
                            motto = g.motto,
                            detailsTitle = stringResource(R.string.group_details),
                            detailRows = detailRows,
                            memberCount = g.stats.memberCount,
                            admins = communityAdmins(g.members, g.createdBy?.id),
                            creatorId = g.createdBy?.id,
                            extraInfoIcon = Icons.Filled.FitnessCenter,
                            extraInfoTitle = stringResource(R.string.community_exercises),
                            extraInfoCount = g.stats.exerciseCount,
                            isExpandable = false,
                            shareTitle = stringResource(R.string.group_share),
                            shareSubtitle = stringResource(R.string.community_share_subtitle),
                            onSeeMembers = { onOpenMembers(g.id) },
                            onShare = { shareGroup(context, g.id) },
                            extraContent = { GroupExercisesContent(availableKeys) },
                        )
                    }
                  }
                  }
                }
            }
        }
    }

    // Diálogos de acciones (portados de GroupDetailScreen).
    if (showLeave) HitPopup(
        title = stringResource(R.string.group_leave_confirm_title),
        message = stringResource(R.string.group_leave_confirm_msg),
        confirmText = stringResource(R.string.group_leave), confirmType = HitButtonType.Destructive,
        onConfirm = { showLeave = false; viewModel.leave() },
        cancelText = stringResource(R.string.common_cancel), onCancel = { showLeave = false },
        onDismissRequest = { showLeave = false },
    )
    if (showAdminBlock) HitPopup(
        title = stringResource(R.string.group_admin_block_title), message = stringResource(R.string.group_admin_block_msg),
        confirmText = stringResource(R.string.common_accept),
        onConfirm = { showAdminBlock = false }, onDismissRequest = { showAdminBlock = false },
    )
    if (showDelete) HitPopup(
        title = stringResource(R.string.group_delete_confirm_title), message = stringResource(R.string.group_delete_confirm_msg),
        icon = painterResource(R.drawable.im_ico_trash), confirmText = stringResource(R.string.common_accept), confirmType = HitButtonType.Destructive,
        onConfirm = { showDelete = false; viewModel.delete() },
        cancelText = stringResource(R.string.common_cancel), onCancel = { showDelete = false },
        onDismissRequest = { showDelete = false },
    )
    if (showReport) com.hitbosss.presentation.feature.hit.ReportDialog(
        title = stringResource(R.string.group_report_title), message = stringResource(R.string.group_report_msg),
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

private fun shareGroup(context: Context, groupId: Int) {
    val link = com.hitbosss.core.network.Environment.deeplinkBaseUrl + "group/$groupId"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.group_share_text, link))
    }
    runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.group_share))) }
}

private fun formatCommDate(unixSeconds: Long): String =
    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(unixSeconds * 1000))

@Composable
private fun GroupRankingContent(
    g: GroupDetail,
    currentUserId: String?,
    currentUserCountry: String?,
    currentUserPicUrl: String?,
    onRecordHit: (String, Double) -> Unit,
    onSavedHits: () -> Unit,
    onTutorial: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit,
) {
    // Pestañas de TODOS los deportes del grupo: oficial (si el grupo es oficial en ese deporte) + sus ejercicios.
    val categories = Sport.entries.flatMap { sp ->
        val official = if (sp == Sport.Powerlifting) RankingCategory.PlOfficial else RankingCategory.CfOfficial
        val exCats = RankingCategory.forSport(sp)
            .filter { it != RankingCategory.PlOfficial && it != RankingCategory.CfOfficial }
            .filter { cat -> g.exercises.any { it.equals(cat.apiKey, true) } }
        val isOfficialSport = g.officialSports.any { it.equals(sp.apiValue, true) }
        (if (isOfficialSport) listOf(official) else emptyList()) + exCats
    }

    var selected by rememberSaveable { mutableStateOf(categories.firstOrNull() ?: RankingCategory.PlOfficial) }
    var search by rememberSaveable { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var order by rememberSaveable { mutableStateOf(RankingOrder.Lift) }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var selectedLevels by rememberSaveable(stateSaver = levelSetSaver) { mutableStateOf(emptySet<String>()) }

    // Todo lo dependiente del deporte se deriva de la pestaña seleccionada (soporta grupos con ambos deportes).
    val sportEnum = selected.sport
    val isPl = sportEnum == Sport.Powerlifting
    val sportColor = if (isPl) Secondary500 else Error500
    val officialCat = if (isPl) RankingCategory.PlOfficial else RankingCategory.CfOfficial
    val ranking = g.ranking[sportEnum.apiValue]
    val isOfficial = selected == officialCat
    val usePoints = order == RankingOrder.Points

    val entries = ranking?.byCategory?.get(selected).orEmpty()
        .filter { e ->
            (search.isBlank() || e.username.contains(search, true)) &&
                (selectedLevels.isEmpty() || (if (usePoints) e.levelWilks else e.levelWeight).orEmpty() in selectedLevels)
        }
        // Re-ordena por el criterio elegido (peso/puntos) con desempate y re-numera, igual que iOS.
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
    val bottomBase = 12.dp

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Buscador + orden
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF2F2F2)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (search.isEmpty()) Text(stringResource(R.string.ranking_search), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                        BasicTextField(search, { search = it }, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
                    }
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Gray500)
                }
                Box(Modifier.size(48.dp).clip(CircleShape).background(Gray100).border(1.dp, Gray300, CircleShape).clickable { showSort = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.ranking_sort), tint = Gray800, modifier = Modifier.size(22.dp))
                }
            }

            // Tabs ejercicio
            val tabsState = androidx.compose.foundation.lazy.rememberLazyListState()
            // Centra la categoría seleccionada en el viewport (igual que en el ranking principal).
            androidx.compose.runtime.LaunchedEffect(selected, categories) {
                val idx = categories.indexOf(selected)
                if (idx >= 0) tabsState.centerItem(idx)
            }
            LazyRow(state = tabsState, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(categories) { cat ->
                    val sel = cat == selected
                    val label = if (cat == officialCat) sportEnum.brandTitle else stringResource(cat.titleRes())
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selected = cat }) {
                        Text(label, style = HitbosssType.titleBody, color = if (sel) Gray800 else Gray500, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(vertical = 8.dp))
                        Box(Modifier.height(2.dp).width(if (sel) 40.dp else 0.dp).background(sportColor))
                    }
                }
            }

            // Filtros de nivel
            LevelFilters(entries.size, selectedLevels) { level ->
                selectedLevels = if (level in selectedLevels) selectedLevels - level else selectedLevels + level
            }

            LazyColumn(Modifier.weight(1f).background(Gray300), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isOfficial && notParticipating) item { NoParticipaCard(sportEnum.title, required) }
                if (entries.isEmpty() && !(isOfficial && notParticipating)) {
                    item { com.hitbosss.presentation.feature.ranking.RankingEmptyState() }
                }
                items(entries) { entry -> RankingRow(entry, usePoints) { selectedUserId = entry.userId } }
            }

            // Padding inferior común para tarjeta "Tú" y FAB (los sube respecto al borde), igual que
            // en el ranking principal donde la barra de navegación los levanta. La distancia FAB↔tarjeta
            // se añade aparte sobre ese mismo padding.
            currentUserId?.let { CurrentUserRow(currentUserEntry, currentUserCountry, usePoints, currentUserPicUrl, Modifier.navigationBarsPadding().padding(bottom = bottomBase)) }
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
}

/** Saver para conservar los niveles seleccionados (Set<String>) en rememberSaveable. */
private val levelSetSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() },
)
