package com.hitbosss.presentation.feature.ranking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.hitbosss.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.RankingEntry
import com.hitbosss.domain.model.Sport
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary100
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Primary800
import com.hitbosss.presentation.designsystem.theme.Secondary100
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary700
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.designsystem.theme.Warning100
import com.hitbosss.presentation.designsystem.theme.Warning500
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.feature.hit.HitVideoDialog
import com.hitbosss.presentation.feature.hit.UploadHitFab
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.hitbosss.presentation.feature.ranking.titleRes

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    onRecordHit: (String, Double) -> Unit = { _, _ -> },
    onSavedHits: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    viewModel: RankingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Tutorial coach-marks (1:1 iOS): se muestra una vez en la primera visita al ranking.
    var showOnboarding by remember { mutableStateOf(!TutorialTracker.hasSeenRankingOnboarding(context)) }
    var onboardingStep by remember { mutableStateOf(RankingOnboardingStep.Exercises) }
    var exercisesFrame by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var filterFrame by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var orderFrame by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var fabFrame by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    Box(Modifier.fillMaxSize().background(Gray100)) {
        Column(Modifier.fillMaxSize()) {
            SportHeader(sport = state.sport, onSelect = viewModel::load)
            SearchRow(
                value = state.searchText,
                onChange = viewModel::onSearch,
                filtersActive = state.hasActiveFilters,
                onFilter = { showFilterSheet = true },
                onSort = { showSortSheet = true },
                onFilterPositioned = { filterFrame = it },
                onSortPositioned = { orderFrame = it },
            )
            Box(Modifier.onGloballyPositioned { exercisesFrame = it.boundsInRoot() }) {
                ExerciseTabs(state.categories, state.selectedCategory, viewModel::selectCategory)
            }
            LevelFilters(state.displayedEntries.size, state.selectedLevels, viewModel::toggleLevel)

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Primary500)
                }

                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    com.hitbosss.presentation.designsystem.components.ErrorConnectionView(onRetry = viewModel::refresh)
                }

                else -> androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.weight(1f),
                ) {
                  LazyColumn(
                    modifier = Modifier.fillMaxSize().background(Gray300),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.isOfficial && state.notParticipating) {
                        item { NoParticipaCard(state.sport.replaceFirstChar { it.uppercase() }, state.requiredExercises) }
                    }
                    if (state.displayedEntries.isEmpty() && !state.isOfficial) {
                        item { RankingEmptyState() }
                    }
                    items(state.displayedEntries) { entry ->
                        RankingRow(entry, state.orderByPoints) { selectedUserId = entry.userId }
                    }
                  }
                }
            }

            state.currentUserId?.let { CurrentUserRow(state.currentUserEntry, state.currentUserCountry, state.orderByPoints, state.currentUserPicUrl) }
        }

        UploadHitFab(
            sport = state.selectedCategory.sport,
            isOfficial = state.isOfficial,
            exerciseKey = state.selectedCategory.uploadExerciseKey,
            onRecordHit = onRecordHit,
            onSavedHits = onSavedHits,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 88.dp)
                .onGloballyPositioned { fabFrame = it.boundsInRoot() },
        )

        if (showOnboarding) {
            val frame = when (onboardingStep) {
                RankingOnboardingStep.Exercises -> exercisesFrame
                RankingOnboardingStep.Filters -> filterFrame
                RankingOnboardingStep.OrderBy -> orderFrame
                RankingOnboardingStep.UploadHit -> fabFrame
            }
            RankingOnboardingOverlay(
                step = onboardingStep,
                highlightFrame = frame,
                onNext = {
                    val next = RankingOnboardingStep.entries.getOrNull(onboardingStep.ordinal + 1)
                    if (next != null) {
                        onboardingStep = next
                    } else {
                        TutorialTracker.markRankingOnboardingSeen(context)
                        showOnboarding = false
                    }
                },
                onBack = {
                    RankingOnboardingStep.entries.getOrNull(onboardingStep.ordinal - 1)?.let { onboardingStep = it }
                },
                onSkip = {
                    TutorialTracker.markRankingOnboardingSeen(context)
                    showOnboarding = false
                },
            )
        }
    }

    if (showSortSheet) {
        RankingSortSheet(
            current = state.orderBy,
            onSelect = viewModel::setOrder,
            onDismiss = { showSortSheet = false },
        )
    }
    if (showFilterSheet) {
        RankingFilterSheet(
            location = state.selectedLocation,
            selectedLevels = state.selectedLevels,
            selectedAges = state.selectedAges,
            gender = state.selectedGender,
            onLocation = viewModel::setLocation,
            onToggleLevel = viewModel::toggleLevel,
            onToggleAge = viewModel::toggleAge,
            onGender = viewModel::setGender,
            onSave = { /* filtros aplicados en vivo vía estado */ },
            onDismiss = { showFilterSheet = false },
        )
    }

    val ranking = state.ranking
    selectedUserId?.let { uid ->
        if (ranking != null) UserDetailModal(
            userId = uid,
            ranking = ranking,
            onDismiss = { selectedUserId = null },
            onVisitProfile = {
                selectedUserId = null
                onOpenUserProfile(uid)
            },
        )
    }
}

@Composable
private fun SportHeader(sport: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val isPl = sport == "powerlifting"
    Box {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Badge de deporte de iOS (iconPowerlifting/iconCrossfit), 32x32 directo (sin caja envolvente).
            Image(
                painterResource(if (isPl) R.drawable.im_icon_powerlifting else R.drawable.im_icon_crossfit),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Text(if (isPl) "Powerlifting" else "CrossHIT", style = HitbosssType.titleSection, color = Gray800)
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).border(1.dp, Gray300, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.ranking_change_sport), tint = Gray800, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("powerlifting" to "Powerlifting", "crossfit" to "CrossHIT").forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label, style = HitbosssType.bodyLargeRegular) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

@Composable
private fun SearchRow(
    value: String,
    onChange: (String) -> Unit,
    filtersActive: Boolean,
    onFilter: () -> Unit,
    onSort: () -> Unit,
    onFilterPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    onSortPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Gray200)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(stringResource(R.string.ranking_search), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                BasicTextField(
                    value = value, onValueChange = onChange, singleLine = true,
                    textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Icon(Icons.Filled.Search, contentDescription = null, tint = Gray500)
        }
        // Botón de filtro (azul si hay filtros activos)
        CircleIconButton(
            icon = Icons.Filled.Tune,
            borderColor = if (filtersActive) Secondary500 else Gray300,
            onClick = onFilter,
            contentDescription = stringResource(R.string.ranking_configure),
            modifier = Modifier.onGloballyPositioned { onFilterPositioned(it.boundsInRoot()) },
        )
        // Botón de orden
        CircleIconButton(
            icon = Icons.Filled.SwapVert,
            borderColor = Gray300,
            onClick = onSort,
            contentDescription = stringResource(R.string.ranking_sort),
            modifier = Modifier.onGloballyPositioned { onSortPositioned(it.boundsInRoot()) },
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    borderColor: Color,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(48.dp).clip(CircleShape).background(Gray100)
            .border(1.dp, borderColor, CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Gray800, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ExerciseTabs(categories: List<RankingCategory>, selected: RankingCategory, onSelect: (RankingCategory) -> Unit) {
    // Color del indicador según deporte: Powerlifting=Secondary500 (azul), CrossHIT=Error500 (rojo).
    val indicator = if (selected.sport == Sport.Crossfit) Error500 else Secondary500
    LazyRow(
        modifier = Modifier.padding(top = 8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(categories) { category ->
            val sel = category == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // IntrinsicSize.Max da al Column el ancho del texto para que el subrayado lo rellene
                // (dentro de un LazyRow fillMaxWidth colapsaría a 0 por el constraint horizontal infinito).
                modifier = Modifier.width(IntrinsicSize.Max).clickable { onSelect(category) },
            ) {
                Text(
                    if (category == RankingCategory.PlOfficial || category == RankingCategory.CfOfficial) {
                        if (category == RankingCategory.PlOfficial) "Powerlifting" else "CrossHIT"
                    } else stringResource(category.titleRes()),
                    style = HitbosssType.bodyLargeRegular,
                    color = Gray800,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Box(Modifier.fillMaxWidth().height(2.dp).background(if (sel) indicator else Color.Transparent))
            }
        }
    }
}

@Composable
internal fun LevelFilters(count: Int, selected: Set<String>, onToggle: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(15.dp)).border(1.dp, Gray300, RoundedCornerShape(15.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Gray800, modifier = Modifier.size(16.dp))
            Text("$count", style = HitbosssType.bodyDefaultRegular, color = Secondary800)
        }
        Spacer(Modifier.width(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(levelFilterOptions) { (key, label) ->
                val sel = key in selected
                Text(
                    stringResource(label),
                    style = HitbosssType.bodyDefaultRegular,
                    color = Secondary800,
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Gray100)
                        .border(1.dp, if (sel) Secondary500 else Gray300, RoundedCornerShape(24.dp))
                        .clickable { onToggle(key) }.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun RankingRow(entry: RankingEntry, usePoints: Boolean, onClick: () -> Unit) {
    // Sin padding uniforme: el badge de posición va pegado a la esquina (como PowerLiftingRowView).
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Gray100)
            .border(1.dp, Gray300, RoundedCornerShape(10.dp)).clickable { onClick() }.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            PositionBadge(entry.rank)
            Spacer(Modifier.weight(1f))
            levelStyle(if (usePoints) entry.levelWilks else entry.levelWeight)?.let { lvl ->
                LevelBadge(lvl, Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = entry.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop,
                placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                modifier = Modifier.padding(horizontal = 8.dp).size(45.dp).clip(RoundedCornerShape(10.dp)).background(Gray200),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.username, style = HitbosssType.bodyDefaultRegular, color = Gray800)
                Text("${countryFlag(entry.countryCode)} ${entry.countryCode ?: ""}", style = HitbosssType.bodySmallRegular, color = Gray800)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 12.dp, bottom = 8.dp),
            ) {
                entry.lift?.let { Text("${it.value.toInt()} ${it.unit.uppercase()}", style = HitbosssType.bodyDefaultEmphasis, color = Gray800) }
                Text("${formatPoints(entry.score)} POINTS", style = HitbosssType.bodySmallRegular, color = Gray500)
            }
        }
    }
}

/**
 * Estado vacío del ranking (sin usuarios en la categoría): ilustración + mensaje, igual que iOS
 * RankingListView. Reutilizado por el ranking global y los de grupo/evento.
 */
@Composable
internal fun RankingEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 70.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Image(
            painterResource(R.drawable.im_empty_ranking),
            contentDescription = null,
            modifier = Modifier.size(width = 251.dp, height = 187.dp),
        )
        Text(
            stringResource(R.string.ranking_no_users),
            style = HitbosssType.titleBody,
            color = Gray500,
            textAlign = TextAlign.Center,
        )
    }
}

/** Badge de nivel (ÉLITE, AVANZADO…) igual que LevelView de iOS: bodySmallRegular, h8/v2, radio 4. */
@Composable
internal fun LevelBadge(lvl: LevelStyle, modifier: Modifier = Modifier) {
    Text(
        stringResource(lvl.labelRes).uppercase(), style = HitbosssType.bodySmallRegular, color = lvl.text,
        modifier = modifier.clip(RoundedCornerShape(4.dp)).background(lvl.bg).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
internal fun PositionBadge(position: Int) {
    // CustomCorner(topLeft, bottomRight) de iOS → esquinas topStart + bottomEnd redondeadas.
    val shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    val star: Int? = when (position) {
        1 -> R.drawable.im_star_top1
        2 -> R.drawable.im_star_top2
        3 -> R.drawable.im_star_top3
        else -> null
    }
    val (bg, txt) = when (position) {
        1 -> Color(0xFFFFE9AE) to Color(0xFFE37F05)
        2 -> Color(0xFFE0E3EA) to Color(0xFF444F65)
        3 -> Color(0xFFFFE3CF) to Color(0xFF8E4900)
        else -> Secondary100 to Secondary700
    }
    Row(
        modifier = Modifier.clip(shape).background(bg).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        star?.let { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(14.dp)) }
        Text(if (position in 1..3) "TOP $position" else "$position", style = HitbosssType.bodyDefaultEmphasis, color = txt)
    }
}

@Composable
internal fun CurrentUserRow(entry: RankingEntry?, country: String?, usePoints: Boolean = false, picUrl: String? = null) {
    val code = entry?.countryCode ?: country
    // La entrada del ranking no siempre trae profilePic; usamos la del perfil como fallback fiable.
    val photo = entry?.profilePicUrl ?: picUrl
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp)).background(Secondary800).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = photo, contentDescription = null, contentScale = ContentScale.Crop,
            placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
            modifier = Modifier.size(45.dp).clip(RoundedCornerShape(10.dp)).background(Gray500),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ranking_you), style = HitbosssType.bodySmallRegular, color = Gray100)
                if (entry != null) {
                    Text(
                        "${entry.rank}", style = HitbosssType.bodySmallRegular, color = Primary800,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Primary100).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                } else {
                    Text(
                        stringResource(R.string.ranking_not_ranked), style = HitbosssType.bodySmallEmphasis, color = Primary500,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Gray100).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                code?.let { Text("${countryFlag(it)} $it", style = HitbosssType.bodySmallRegular, color = Gray100) }
                entry?.let { e -> levelStyle(if (usePoints) e.levelWilks else e.levelWeight)?.let { LevelBadge(it) } }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry?.lift?.let { "${it.value.toInt()} ${it.unit.uppercase()}" } ?: "-- KG", style = HitbosssType.bodyDefaultEmphasis, color = Gray100)
            Text(entry?.let { "${formatPoints(it.score)} POINTS" } ?: "-- POINTS", style = HitbosssType.bodySmallRegular, color = Gray100)
        }
    }
}

@Composable
internal fun NoParticipaCard(sportLabel: String, required: List<RequiredExercise>) {
    var expanded by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100)
            .border(1.dp, Warning500, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Warning500, modifier = Modifier.size(22.dp))
            Text(stringResource(R.string.ranking_not_participating), style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Warning500,
                modifier = Modifier.size(22.dp).rotate(if (expanded) 0f else 180f),
            )
        }
        if (expanded) {
            Text(
                stringResource(R.string.ranking_required_exercises, sportLabel),
                style = HitbosssType.bodySmallRegular, color = Gray500,
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                required.forEach { ex ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(if (ex.done) Warning500 else Gray100)
                                .border(1.dp, Warning500, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (ex.done) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Gray100, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(stringResource(ex.labelRes), style = HitbosssType.bodySmallRegular, color = Gray800, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun formatPoints(points: Double): String =
    if (points % 1.0 == 0.0) points.toInt().toString() else "%.2f".format(points)
