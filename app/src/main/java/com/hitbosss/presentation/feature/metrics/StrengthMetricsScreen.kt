package com.hitbosss.presentation.feature.metrics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.R
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.RankingEntry
import com.hitbosss.domain.model.Sport
import com.hitbosss.domain.model.StrengthEntry
import com.hitbosss.domain.util.ProgressStats
import com.hitbosss.presentation.designsystem.components.ChartMarker
import com.hitbosss.presentation.designsystem.components.ChartSeries
import com.hitbosss.presentation.designsystem.components.ErrorConnectionView
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.LineChart
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary100
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.designsystem.theme.Success100
import com.hitbosss.presentation.designsystem.theme.Success500
import com.hitbosss.presentation.designsystem.theme.Warning100
import com.hitbosss.presentation.designsystem.theme.Warning500
import com.hitbosss.presentation.designsystem.theme.Error100
import com.hitbosss.presentation.feature.ranking.countryFlag
import com.hitbosss.presentation.feature.ranking.titleRes
import java.time.LocalDate
import java.time.ZoneId

/** Contenido de la sub-pestaña FUERZA (todo lo del ejercicio seleccionado). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthContent(
    viewModel: StrengthMetricsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddTraining by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<MetricGoal?>(null) }
    var infoPopup by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (titleRes, msgRes)

    // Datos derivados del ranking del ejercicio (calculados en el dispositivo)
    val entries = state.rankings[state.selected.sport.apiValue]?.byCategory?.get(state.selected).orEmpty()
    val ownEntry = entries.firstOrNull { it.userId == state.currentUserId }
    val rival = ownEntry?.let { own -> entries.firstOrNull { it.rank == own.rank - 1 } }
    val ownBest = bestMarkFor(state, state.selected)

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
        state.hasError && state.profile == null ->
            Box(Modifier.fillMaxSize(), Alignment.Center) { ErrorConnectionView(onRetry = viewModel::retry) }
        else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Selector de ejercicio (dropdown → lista full-screen)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Gray100)
                        .clickable { showPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(state.selected.titleRes()), style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = Gray500, modifier = Modifier.size(20.dp))
                }

                MetricsSectionLabel(stringResource(R.string.metrics_section_goals))
                GoalCard(
                    title = stringResource(R.string.metrics_your_goal),
                    goals = state.goals,
                    currentValue = ownBest,
                    unit = state.weightUnit,
                    lowerIsBetter = false,
                    onAdd = { showAddGoal = true },
                    onDelete = { goalToDelete = it },
                )

                MetricsSectionLabel(stringResource(R.string.metrics_competitive))
                CompetitiveSection(
                    state = state,
                    ownEntry = ownEntry,
                    entries = entries,
                    ownBest = ownBest,
                    rival = rival,
                    onInfo = { title, msg -> infoPopup = title to msg },
                )

                MetricsSectionLabel(stringResource(R.string.metrics_section_stats))
                StrengthEvolutionCard(
                    state = state,
                    communityAvg = entries.mapNotNull { it.lift?.value }.takeIf { it.isNotEmpty() }?.average(),
                    onAddTraining = { showAddTraining = true },
                )

                MetricsSectionLabel(stringResource(R.string.metrics_comparison))
                ComparisonCard(state)

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            selected = state.selected,
            onSelect = { viewModel.onSelectExercise(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
    if (showAddGoal) {
        SingleValueSheet(
            title = stringResource(R.string.metrics_goal_add_title),
            hint = stringResource(R.string.metrics_goal_add_hint),
            unit = state.weightUnit,
            buttonText = stringResource(R.string.metrics_goal_add_title),
            isSaving = state.isSaving,
            onDismiss = { showAddGoal = false },
            onSave = { viewModel.onAddGoal(it); showAddGoal = false },
        )
    }
    if (showAddTraining) {
        SingleValueSheet(
            title = stringResource(R.string.metrics_add_training),
            hint = stringResource(R.string.metrics_training_weight),
            unit = state.weightUnit,
            buttonText = stringResource(R.string.metrics_add),
            isSaving = state.isSaving,
            onDismiss = { showAddTraining = false },
            onSave = { viewModel.onAddTraining(it); showAddTraining = false },
        )
    }
    goalToDelete?.let { goal ->
        HitPopup(
            title = stringResource(R.string.metrics_goal_delete_title),
            message = stringResource(R.string.metrics_goal_delete_msg),
            confirmText = stringResource(R.string.common_delete),
            confirmType = HitButtonType.Destructive,
            onConfirm = { viewModel.onDeleteGoal(goal.id); goalToDelete = null },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { goalToDelete = null },
            onDismissRequest = { goalToDelete = null },
        )
    }
    infoPopup?.let { (title, msg) ->
        HitPopup(
            title = stringResource(title),
            message = stringResource(msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = { infoPopup = null },
            onDismissRequest = { infoPopup = null },
        )
    }
    state.actionError?.let { error ->
        HitPopup(
            title = stringResource(R.string.common_something_wrong),
            message = error,
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearActionError,
            onDismissRequest = viewModel::clearActionError,
        )
    }
}

/** Mejor marca propia del ejercicio (participations del perfil, ya en unidad del usuario). */
private fun bestMarkFor(state: StrengthMetricsUiState, category: RankingCategory): Double? =
    state.profile?.participations
        ?.filter { it.exercise.equals(category.apiKey, ignoreCase = true) }
        ?.mapNotNull { it.maxLift?.value }
        ?.maxOrNull()

// ============================ DATOS COMPETITIVOS ============================

@Composable
private fun CompetitiveSection(
    state: StrengthMetricsUiState,
    ownEntry: RankingEntry?,
    entries: List<RankingEntry>,
    ownBest: Double?,
    rival: RankingEntry?,
    onInfo: (Int, Int) -> Unit,
) {
    // Serie temporal propia (entrenamientos + hits) para mejora y ritmo
    val points = (state.history.trainings + state.history.hits).map { it.performedAt to it.lift.value }
    val improvement = ProgressStats.accumulatedImprovement(points)
    val rate = ProgressStats.progressRatePerMonth(points)
    val advantage = ownBest?.let { own -> ProgressStats.communityAdvantage(own, entries.mapNotNull { it.lift?.value }) }
    val surpassed = ownEntry?.let { ProgressStats.percentSurpassed(it.rank, entries.size) }
    val unit = state.weightUnit

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Filled.TrendingUp, iconTint = Success500, iconBg = Success100,
                label = stringResource(R.string.metrics_improvement),
                value = improvement?.let { signed(it) } ?: "--", unit = unit,
                onInfo = { onInfo(R.string.metrics_improvement, R.string.metrics_improvement_info) },
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Filled.ShowChart, iconTint = Warning500, iconBg = Warning100,
                label = stringResource(R.string.metrics_rate),
                value = rate?.let { formatNum(it) } ?: "--",
                unit = stringResource(R.string.metrics_per_month, unit),
                onInfo = { onInfo(R.string.metrics_rate, R.string.metrics_rate_info) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Filled.Groups, iconTint = Secondary500, iconBg = Secondary100,
                label = stringResource(R.string.metrics_advantage),
                value = advantage?.let { signed(it) } ?: "--", unit = unit,
                onInfo = { onInfo(R.string.metrics_advantage, R.string.metrics_advantage_info) },
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Filled.Percent, iconTint = Error500, iconBg = Error100,
                label = stringResource(R.string.metrics_surpassed),
                value = surpassed?.let { formatNum(it) } ?: "--", unit = "%",
                onInfo = { onInfo(R.string.metrics_surpassed, R.string.metrics_surpassed_info) },
                modifier = Modifier.weight(1f),
            )
        }

        // Próximo rival a superar (card sin el padding externo: la sección ya lo aplica)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.metrics_next_rival), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
                InfoIcon(onClick = { onInfo(R.string.metrics_next_rival, R.string.metrics_next_rival_info) })
            }
            Spacer(Modifier.height(12.dp))
            if (rival == null) {
                Text(stringResource(R.string.metrics_no_rival), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = rival.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop,
                        placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Gray300),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(rival.username, style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                        Text("${countryFlag(rival.countryCode)} ${rival.countryCode.orEmpty().uppercase()}", style = HitbosssType.bodySmallRegular, color = Gray500)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${rival.lift?.value?.let { formatNum(it) } ?: "--"} ${rival.lift?.unit?.uppercase() ?: ""}", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                        Text("#${rival.rank}", style = HitbosssType.bodySmallRegular, color = Gray500)
                    }
                }
                val gap = rival.lift?.value?.let { r -> ownBest?.let { r - it } }
                if (gap != null && gap > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.metrics_rival_gap, "${formatNum(gap)} ${state.weightUnit}"),
                        style = HitbosssType.bodySmallRegular, color = Secondary500,
                    )
                }
            }
        }
    }
}

private fun signed(v: Double): String = (if (v >= 0) "+" else "") + formatNum(v)

// ============================ EVOLUCIÓN ============================

@Composable
private fun StrengthEvolutionCard(
    state: StrengthMetricsUiState,
    communityAvg: Double?,
    onAddTraining: () -> Unit,
) {
    var monthly by rememberSaveable(state.selected) { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val start = if (monthly) today.withDayOfMonth(1) else today.with(java.time.DayOfWeek.MONDAY)
    val end = if (monthly) start.plusMonths(1) else start.plusWeeks(1)
    val startSec = start.atStartOfDay(zone).toEpochSecond()
    val endSec = end.atStartOfDay(zone).toEpochSecond()
    val span = (endSec - startSec).toFloat()

    fun List<StrengthEntry>.toPoints() = mapNotNull { e ->
        if (e.performedAt < startSec || e.performedAt >= endSec) null
        else (e.performedAt - startSec) / span to e.lift.value
    }

    val trainingPoints = state.history.trainings.toPoints()
    val hitPoints = state.history.hits.toPoints()
    val goal = state.goals.active?.target?.value
    val xLabels = if (monthly) listOf("1", "8", "15", "22", stringResource(R.string.metrics_month_end_label))
    else stringResource(R.string.metrics_week_days).split(",")

    MetricsCard {
        Text(stringResource(R.string.metrics_evolution), style = HitbosssType.titleGroup, color = Gray800)
        Spacer(Modifier.height(12.dp))
        MetricsSegmented(
            options = listOf(stringResource(R.string.metrics_week), stringResource(R.string.metrics_month)),
            selectedIndex = if (monthly) 1 else 0,
            onSelect = { monthly = it == 1 },
        )
        Spacer(Modifier.height(16.dp))

        if (trainingPoints.isEmpty() && hitPoints.isEmpty() && state.history.trainings.isEmpty() && state.history.hits.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.metrics_empty_logs), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
        } else {
            val series = buildList {
                if (trainingPoints.isNotEmpty()) add(ChartSeries(trainingPoints, color = Primary500, marker = ChartMarker.Circle, fill = true))
                if (hitPoints.isNotEmpty()) add(ChartSeries(hitPoints, color = Primary500, marker = ChartMarker.Diamond, showLine = false))
                communityAvg?.let { add(ChartSeries(listOf(0f to it, 1f to it), color = Gray500, dashed = true)) }
                goal?.let { add(ChartSeries(listOf(0f to it, 1f to it), color = Success500, dashed = true)) }
            }
            LineChart(series = series, xLabels = xLabels, modifier = Modifier.fillMaxWidth().height(160.dp))
            Spacer(Modifier.height(12.dp))
            // Leyenda
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(Primary500, stringResource(R.string.metrics_legend_training))
                LegendDot(Primary500, stringResource(R.string.metrics_legend_hit), diamond = true)
                LegendDot(Gray500, stringResource(R.string.metrics_legend_community))
                LegendDot(Success500, stringResource(R.string.metrics_legend_goal))
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Secondary800)
                .clickable(onClick = onAddTraining).padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ " + stringResource(R.string.metrics_add_training), style = HitbosssType.bodyDefaultEmphasis, color = Gray100)
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String, diamond: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(8.dp)
                .clip(if (diamond) RoundedCornerShape(1.dp) else CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
    }
}

// ============================ COMPARATIVA ============================

@Composable
private fun ComparisonCard(state: StrengthMetricsUiState) {
    MetricsCard {
        Sport.entries.forEachIndexed { sportIndex, sport ->
            val color = if (sport == Sport.Powerlifting) Secondary500 else Error500
            if (sportIndex > 0) Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (sport == Sport.Powerlifting) Icons.Filled.FitnessCenter else Icons.Filled.LocalFireDepartment,
                    null, tint = color, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(sport.brandTitle.uppercase(), style = HitbosssType.bodySmallEmphasis, color = color)
            }
            val categories = RankingCategory.forSport(sport)
                .filter { it != RankingCategory.PlOfficial && it != RankingCategory.CfOfficial }
            categories.forEach { cat ->
                val best = bestMarkFor(state, cat)
                val communityAvg = state.rankings[sport.apiValue]?.byCategory?.get(cat)
                    ?.mapNotNull { it.lift?.value }?.takeIf { it.isNotEmpty() }?.average()
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(cat.titleRes()), style = HitbosssType.bodyDefaultRegular, color = Gray800, modifier = Modifier.weight(1f))
                    if (best == null) {
                        Text(stringResource(R.string.profile_no_mark), style = HitbosssType.bodySmallEmphasis, color = Gray500)
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${formatNum(best)} ${state.weightUnit.uppercase()}", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                            val delta = communityAvg?.let { best - it }
                            if (delta != null) {
                                Text(
                                    "${signed(delta)} ${state.weightUnit}",
                                    style = HitbosssType.bodySmallRegular,
                                    color = if (delta >= 0) Success500 else Error500,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================ PICKER DE EJERCICIO ============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    selected: RankingCategory,
    onSelect: (RankingCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            Sport.entries.forEach { sport ->
                val color = if (sport == Sport.Powerlifting) Secondary500 else Error500
                item(key = sport.apiValue) {
                    Text(
                        sport.brandTitle.uppercase(),
                        style = HitbosssType.bodySmallEmphasis, color = color,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    )
                }
                val cats = RankingCategory.forSport(sport)
                    .filter { it != RankingCategory.PlOfficial && it != RankingCategory.CfOfficial }
                items(cats, key = { it.name }) { cat ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onSelect(cat) }
                            .background(if (cat == selected) Gray200 else Gray100)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(cat.titleRes()), style = HitbosssType.bodyDefaultRegular, color = Gray800, modifier = Modifier.weight(1f))
                        if (cat == selected) Icon(Icons.Filled.Check, null, tint = Success500, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
