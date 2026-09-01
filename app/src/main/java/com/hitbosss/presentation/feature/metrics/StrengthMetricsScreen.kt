package com.hitbosss.presentation.feature.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import com.hitbosss.presentation.feature.settings.tutorialCardDrawable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.R
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.RankingEntry
import com.hitbosss.domain.model.Sport
import com.hitbosss.presentation.designsystem.components.ChartMarker
import com.hitbosss.presentation.designsystem.components.ChartSeries
import com.hitbosss.presentation.designsystem.components.ErrorConnectionView
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.LineChart
import com.hitbosss.domain.model.StrengthMark
import com.hitbosss.presentation.feature.hit.HitVideoData
import com.hitbosss.presentation.feature.hit.HitVideoDialog
import com.hitbosss.presentation.feature.hit.formatPointsText
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
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
import com.hitbosss.presentation.designsystem.components.formatEpochDate

/** Un punto de la gráfica de fuerza (valor ya en unidad del usuario). */
private data class StrengthPoint(val performedAt: Long, val value: Double)

/** Contenido de la sub-pestaña FUERZA (todo lo del ejercicio seleccionado). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthContent(
    onOpenDetail: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    viewModel: StrengthMetricsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddTraining by remember { mutableStateOf(false) }
    var editTraining by remember { mutableStateOf<StrengthMark?>(null) }
    var infoPopup by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (titleRes, msgRes)

    // Rival: on-device del ranking del ejercicio (igual que iOS). ownBest = mejor marca del servidor (/bests).
    val entries = state.rankings[state.selected.sport.apiValue]?.byCategory?.get(state.selected).orEmpty()
    val ownEntry = entries.firstOrNull { it.userId == state.currentUserId }
    val rival = ownEntry?.let { own -> entries.firstOrNull { it.rank == own.rank - 1 } }
    val ownBest = bestMarkFor(state, state.selected)

    val exTitle = stringResource(state.selected.titleRes())
    val unitLabel = state.weightUnit
    var viewerHit by remember { mutableStateOf<HitVideoData?>(null) }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
        state.hasError && state.profile == null ->
            Box(Modifier.fillMaxSize(), Alignment.Center) { ErrorConnectionView(onRetry = viewModel::retry) }
        else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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

                // Objetivo de Fuerza = objetivo de levantamiento POR EJERCICIO; "actual" = mejor marca.
                MetricsSectionLabel(stringResource(R.string.metrics_section_goals))
                GoalCard(
                    title = stringResource(R.string.metrics_your_goal),
                    goal = state.strengthGoal,
                    unit = state.weightUnit,
                    onAdd = { showAddGoal = true },
                    history = state.goalHistory,
                    showHistory = true,
                    onExpand = {}, // el historial ya viene en el estado (cargado por ejercicio)
                    onDelete = viewModel::onDeleteStrengthGoal,
                )

                MetricsSectionLabel(stringResource(R.string.metrics_competitive))
                CompetitiveSection(
                    state = state,
                    ownBest = ownBest,
                    rival = rival,
                    onInfo = { title, msg -> infoPopup = title to msg },
                    onVisitProfile = onOpenUserProfile,
                )

                MetricsSectionLabel(stringResource(R.string.metrics_section_stats))
                StrengthEvolutionCard(
                    marks = state.marks,
                    unitLabel = unitLabel,
                    onEditTraining = { editTraining = it },
                    // Al pulsar un HIT (rombo) se abre el vídeo, con datos autocontenidos del propio punto.
                    onHitTap = { m ->
                        m.videoUrl?.let { url ->
                            viewerHit = HitVideoData(
                                videoUrl = url,
                                seekSeconds = m.videoSecond ?: 0.0,
                                exerciseTitle = exTitle,
                                dateText = formatEpochDate(m.performedAt, "dd/MM/yy"),
                                weightText = "${formatNum(m.weightKg)} ${unitLabel.uppercase()}",
                                levelWeight = m.levelWeight,
                                rankText = "",   // sin rank: solo el mejor HIT está en el ranking
                                pointsText = formatPointsText(m.wilksScore),
                                hitId = m.hitId,
                            )
                        }
                    },
                    // media de comunidad = miMejor − ventaja (del servidor); robusto y escalable, no depende de las rankings en cliente.
                    communityAvg = state.stats?.communityAdvantageKg?.let { adv -> ownBest?.let { it - adv } },
                    goal = null,
                    onAddTraining = { showAddTraining = true },
                    onOpenDetail = { onOpenDetail(state.selected.apiKey) },
                )
                viewerHit?.let { HitVideoDialog(hits = listOf(it)) { viewerHit = null } }

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
            description = stringResource(R.string.metrics_strength_goal_desc),
            hint = stringResource(R.string.metrics_goal_add_hint),
            unit = state.weightUnit,
            buttonText = stringResource(R.string.metrics_goal_add_title),
            isSaving = state.isSaving,
            onDismiss = { showAddGoal = false },
            onSave = { viewModel.onAddStrengthGoal(it); showAddGoal = false },
        )
    }
    if (showAddTraining) {
        SingleValueSheet(
            title = stringResource(R.string.metrics_add_training),
            description = stringResource(R.string.metrics_training_add_desc),
            hint = stringResource(R.string.metrics_training_weight),
            unit = state.weightUnit,
            buttonText = stringResource(R.string.metrics_add_training),
            isSaving = state.isSaving,
            onDismiss = { showAddTraining = false },
            onSave = { viewModel.onAddTraining(it); showAddTraining = false },
        )
    }
    editTraining?.let { m ->
        m.trainingId?.let { id ->
            EditTrainingSheet(
                initialWeight = m.weightKg,
                unit = state.weightUnit,
                isSaving = state.isSaving,
                onDismiss = { editTraining = null },
                onSave = { w -> viewModel.onEditTraining(id, w); editTraining = null },
                onDelete = { viewModel.onDeleteTraining(id); editTraining = null },
            )
        }
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

/** Mejor marca (HIT) del ejercicio, del servidor (`/strength/bests`). Antes se hacía `max` sobre
 *  participations en cliente → no escalaba con la paginación del perfil. */
private fun bestMarkFor(state: StrengthMetricsUiState, category: RankingCategory): Double? =
    state.bests[category.apiKey]

// ============================ DATOS COMPETITIVOS (del servidor) ============================

@Composable
private fun CompetitiveSection(
    state: StrengthMetricsUiState,
    ownBest: Double?,
    rival: RankingEntry?,
    onInfo: (Int, Int) -> Unit,
    onVisitProfile: (String) -> Unit = {},
) {
    val stats = state.stats
    val unit = state.weightUnit

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Filled.TrendingUp, iconTint = Success500, iconBg = Success100,
                label = stringResource(R.string.metrics_improvement),
                value = stats?.accumulatedImprovementKg?.let { signed(it) } ?: "--", unit = unit,
                onInfo = { onInfo(R.string.metrics_improvement, R.string.metrics_improvement_info) },
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Filled.ShowChart, iconTint = Warning500, iconBg = Warning100,
                label = stringResource(R.string.metrics_rate),
                value = stats?.progressRateKgPerMonth?.let { formatNum(it) } ?: "--",
                unit = stringResource(R.string.metrics_per_month, unit),
                onInfo = { onInfo(R.string.metrics_rate, R.string.metrics_rate_info) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Filled.Groups, iconTint = Secondary500, iconBg = Secondary100,
                label = stringResource(R.string.metrics_advantage),
                value = stats?.communityAdvantageKg?.let { signed(it) } ?: "--", unit = unit,
                onInfo = { onInfo(R.string.metrics_advantage, R.string.metrics_advantage_info) },
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Filled.Percent, iconTint = Error500, iconBg = Error100,
                label = stringResource(R.string.metrics_surpassed),
                value = stats?.rankingPercentage?.let { formatNum(it) } ?: "--", unit = "%",
                onInfo = { onInfo(R.string.metrics_surpassed, R.string.metrics_surpassed_info) },
                modifier = Modifier.weight(1f),
            )
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Error100),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Person, null, tint = Error500, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.metrics_next_rival), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
                InfoIcon(onClick = { onInfo(R.string.metrics_next_rival, R.string.metrics_next_rival_info) })
            }
            Spacer(Modifier.height(12.dp))
            if (rival == null) {
                Text(stringResource(R.string.metrics_no_rival), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            } else {
                // Card interna con borde, que abre el perfil del rival (salvo usuario eliminado, no tocable).
                val canVisit = !rival.isDeleted
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Gray400, RoundedCornerShape(12.dp))
                        .then(if (canVisit) Modifier.clickable { onVisitProfile(rival.userId) } else Modifier)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = rival.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop,
                        placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Gray300),
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
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = Success500, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.metrics_rival_gap, "${formatNum(gap)} ${state.weightUnit}"),
                            style = HitbosssType.bodySmallRegular, color = Success500,
                        )
                    }
                }
            }
        }
    }
}

private fun signed(v: Double): String = (if (v >= 0) "+" else "") + formatNum(v)

// ============================ EVOLUCIÓN ============================

@Composable
private fun StrengthEvolutionCard(
    marks: List<StrengthMark>,
    unitLabel: String,
    onHitTap: (StrengthMark) -> Unit,
    onEditTraining: (StrengthMark) -> Unit,
    communityAvg: Double?,
    goal: Double?,
    onAddTraining: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    var monthly by rememberSaveable { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val start = if (monthly) today.minusWeeks(3).with(java.time.DayOfWeek.MONDAY) else today.with(java.time.DayOfWeek.MONDAY)
    val end = if (monthly) start.plusWeeks(4) else start.plusWeeks(1)
    val startSec = start.atStartOfDay(zone).toEpochSecond()
    val endSec = end.atStartOfDay(zone).toEpochSecond()
    val span = (endSec - startSec).toFloat().coerceAtLeast(1f)
    val week = stringResource(R.string.metrics_week)
    val xLabels = if (monthly) List(4) { "$week ${it + 1}" }
    else stringResource(R.string.metrics_week_days).split(",")
    val slots = xLabels.size

    // Igual que iOS (workoutDataPoints): UNA marca por bucket (día en Semana, semana en Mes) = la MEJOR
    // (más peso; el HIT gana el empate). Se dibuja una línea con esos puntos y un marcador por punto
    // (círculo=entreno, rombo=HIT clicable). Los buckets vacíos no pintan.
    val bestPoints = marks
        .filter { it.performedAt in startSec until endSec }
        .groupBy { (((it.performedAt - startSec).toFloat() / span) * slots).toInt().coerceIn(0, slots - 1) }
        .mapValues { (_, ms) -> ms.maxWith(compareBy({ it.weightKg }, { it.isHit })) }
        .toSortedMap()
        .map { (slot, m) -> Triple(if (slots > 1) slot.toFloat() / (slots - 1) else 0.5f, m.weightKg, m) }
    val linePoints = bestPoints.map { it.first to it.second }
    val trainingPoints = bestPoints.filter { !it.third.isHit }.map { it.first to it.second }
    val hitMarkerMarks = bestPoints.filter { it.third.isHit }
    val hitPoints = hitMarkerMarks.map { it.first to it.second }

    // Tiles "Semana actual/pasada": MEDIA de la mejor marca POR DÍA de cada semana (ojo: por medias, no la mejor
    // marca a secas). Se calcula en cliente desde marks (no hay endpoint trend para fuerza).
    val thisMon = today.with(java.time.DayOfWeek.MONDAY)
    fun weekAvg(fromWeeksAgo: Int): Double? {
        val from = thisMon.minusWeeks(fromWeeksAgo.toLong()).atStartOfDay(zone).toEpochSecond()
        val to = thisMon.minusWeeks((fromWeeksAgo - 1).toLong()).atStartOfDay(zone).toEpochSecond()
        val inWk = marks.filter { it.performedAt in from until to }
        if (inWk.isEmpty()) return null
        return inWk.groupBy { java.time.Instant.ofEpochSecond(it.performedAt).atZone(zone).toLocalDate() }
            .values.map { day -> day.maxOf { m -> m.weightKg } }.average()
    }
    val curWeekAvg = weekAvg(0)
    val prevWeekAvg = weekAvg(1)

    MetricsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metrics_evolution), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.metrics_more_details),
                style = HitbosssType.bodySmallLink, color = Secondary500,
                modifier = Modifier.clickable(onClick = onOpenDetail),
            )
        }
        Spacer(Modifier.height(12.dp))
        MetricsSegmented(
            options = listOf(stringResource(R.string.metrics_week), stringResource(R.string.metrics_month)),
            selectedIndex = if (monthly) 1 else 0,
            onSelect = { monthly = it == 1 },
        )
        Spacer(Modifier.height(16.dp))

        if (bestPoints.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.metrics_empty_logs), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
        } else {
            // Selección de un ENTRENO (para editar): tooltip + link "Editar entrenamiento" debajo.
            var selectedTraining by remember(monthly, marks) { mutableStateOf<StrengthMark?>(null) }
            var selectedIdx by remember(monthly, marks) { mutableStateOf<Int?>(null) }
            val dayFmt = remember { java.time.format.DateTimeFormatter.ofPattern("EEEE d", java.util.Locale("es")) }
            val series = buildList {
                // Índice 0 = línea con TODOS los puntos (mejor por bucket) → es la serie clicable.
                add(ChartSeries(linePoints, color = Primary500, marker = ChartMarker.None, fill = true))
                if (trainingPoints.isNotEmpty()) add(ChartSeries(trainingPoints, color = Primary500, marker = ChartMarker.Circle, showLine = false))
                if (hitPoints.isNotEmpty()) add(ChartSeries(hitPoints, color = Primary500, marker = ChartMarker.Diamond, showLine = false))
                communityAvg?.let { add(ChartSeries(listOf(0f to it, 1f to it), color = Gray500, dashed = true)) }
                goal?.let { add(ChartSeries(listOf(0f to it, 1f to it), color = Success500, dashed = true)) }
            }
            LineChart(
                series = series, xLabels = xLabels, modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                tapSeriesIndex = 0,                 // la línea con todos los puntos
                selectedIndex = selectedIdx,        // tooltip del entreno seleccionado
                onSelect = { i ->
                    val m = i?.let { bestPoints.getOrNull(it)?.third }
                    when {
                        m == null -> { selectedTraining = null; selectedIdx = null }
                        m.isHit -> { selectedTraining = null; selectedIdx = null; onHitTap(m) }   // HIT → abre vídeo
                        selectedIdx == i -> { selectedTraining = null; selectedIdx = null }        // re-toque → deselecciona
                        else -> { selectedTraining = m; selectedIdx = i }                          // entreno → selecciona
                    }
                },
                pointLabel = { i ->
                    val m = bestPoints[i].third
                    val d = java.time.Instant.ofEpochSecond(m.performedAt).atZone(zone).toLocalDate()
                        .format(dayFmt).replaceFirstChar { it.uppercase() }
                    "$d\n${formatNum(m.weightKg)} $unitLabel"
                },
            )
            // Link "Editar entrenamiento" cuando hay un entreno seleccionado (con id del servidor).
            selectedTraining?.takeIf { it.trainingId != null }?.let { m ->
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.metrics_edit_training),
                    style = HitbosssType.bodySmallLink, color = Primary500,
                    modifier = Modifier.clickable { onEditTraining(m) },
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendMark(LegendKind.Training, stringResource(R.string.metrics_legend_training))
                LegendMark(LegendKind.Hit, stringResource(R.string.metrics_legend_hit))
                LegendMark(LegendKind.Community, stringResource(R.string.metrics_legend_community))
            }

            // Tiles "Semana actual/pasada" (media de la semana) + diferencia.
            var showTileInfo by remember { mutableStateOf(false) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StrengthWeekTile(stringResource(R.string.metrics_current_week), curWeekAvg, unitLabel, { showTileInfo = true }, Modifier.weight(1f))
                StrengthWeekTile(stringResource(R.string.metrics_past_week), prevWeekAvg, unitLabel, { showTileInfo = true }, Modifier.weight(1f))
            }
            if (curWeekAvg != null && prevWeekAvg != null) {
                val delta = curWeekAvg - prevWeekAvg
                if (kotlin.math.abs(delta) > 0.001) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        (if (delta < 0) "↓ " else "↑ ") + stringResource(
                            if (delta < 0) R.string.metrics_strength_lost else R.string.metrics_strength_gained,
                            "${formatNum(kotlin.math.abs(delta))} $unitLabel",
                        ),
                        style = HitbosssType.bodySmallRegular,
                        color = if (delta >= 0) Success500 else Error500,   // más fuerza = mejor (verde)
                    )
                }
            }
            if (showTileInfo) {
                HitPopup(
                    title = stringResource(R.string.metrics_records),
                    message = stringResource(R.string.metrics_strength_week_info),
                    confirmText = stringResource(R.string.common_accept),
                    onConfirm = { showTileInfo = false },
                    onDismissRequest = { showTileInfo = false },
                )
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

private enum class LegendKind { Training, Hit, Community }

/** Marcador de leyenda que replica exactamente lo que pinta la gráfica: círculo hueco (entreno),
 *  rombo relleno (HIT), línea discontinua gris (media de comunidad). */
@Composable
private fun LegendMark(kind: LegendKind, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 14.dp, height = 10.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            when (kind) {
                LegendKind.Training -> {
                    val r = 4.dp.toPx()
                    drawCircle(androidx.compose.ui.graphics.Color.White, r, Offset(cx, cy))
                    drawCircle(Primary500, r, Offset(cx, cy), style = Stroke(2.dp.toPx()))
                }
                LegendKind.Hit -> {
                    val r = 5.dp.toPx()
                    drawPath(
                        Path().apply {
                            moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
                        },
                        Primary500,
                    )
                }
                LegendKind.Community -> drawLine(
                    Gray500, Offset(0f, cy), Offset(size.width, cy),
                    strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
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
                        sport.title.uppercase(),
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

/** Sheet de editar/eliminar un entrenamiento manual (peso + Guardar cambios + Eliminar con confirmación). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTrainingSheet(
    initialWeight: Double,
    unit: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    var value by remember { mutableStateOf(formatNum(initialWeight)) }
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.metrics_edit_training), style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.metrics_edit_training_desc), style = HitbosssType.bodySmallRegular, color = Gray500)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.metrics_training_weight), style = HitbosssType.bodySmallEmphasis, color = Gray800, modifier = Modifier.padding(bottom = 6.dp))
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray200).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { new -> val n = new.replace(',', '.'); if (new.isEmpty() || n.toDoubleOrNull() != null) value = n },
                    singleLine = true, textStyle = HitbosssType.bodyLargeEmphasis.copy(color = Gray800),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Text(unit, style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
            Spacer(Modifier.height(20.dp))
            val canSave = value.toDoubleOrNull()?.let { it > 0 } == true && !isSaving
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (canSave) Secondary800 else Gray300)
                    .then(if (canSave) Modifier.clickable { onSave(value.toDouble()) } else Modifier).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.common_save_changes), style = HitbosssType.bodyDefaultEmphasis, color = if (canSave) Gray100 else Gray500) }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Error500)
                    .clickable { confirmDelete = true }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.common_delete), style = HitbosssType.bodyDefaultEmphasis, color = Gray100) }
        }
    }
    if (confirmDelete) {
        HitPopup(
            title = stringResource(R.string.metrics_delete_training_title),
            message = stringResource(R.string.metrics_delete_training_msg),
            confirmText = stringResource(R.string.common_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = { confirmDelete = false; onDelete() },
            onCancel = { confirmDelete = false },
            onDismissRequest = { confirmDelete = false },
        )
    }
}

/** Tile "Semana actual/pasada" del card de Fuerza: etiqueta + ⓘ arriba, media grande abajo. */
@Composable
private fun StrengthWeekTile(label: String, value: Double?, unit: String, onInfo: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Gray100)
            .border(1.dp, Gray400, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // 1. Fila superior: Se mantiene igual (Título a la izquierda, icono a la derecha)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.weight(1f))
            InfoIcon(onClick = onInfo)
        }

        Spacer(Modifier.height(4.dp))

        // 2. Fila inferior: Envuelta en un Box para centrarla horizontalmente
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value?.let { formatNum(it) } ?: "--", style = HitbosssType.titleBody, color = Gray800)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}
