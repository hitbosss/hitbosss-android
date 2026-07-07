package com.hitbosss.presentation.feature.metrics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.domain.model.BodyLog
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.util.BmiCalculator
import com.hitbosss.presentation.designsystem.components.ChartMarker
import com.hitbosss.presentation.designsystem.components.ChartSeries
import com.hitbosss.presentation.designsystem.components.ErrorConnectionView
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.LineChart
import com.hitbosss.presentation.designsystem.theme.Error400
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Orange300
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.designsystem.theme.Success400
import com.hitbosss.presentation.designsystem.theme.Success500
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Sub-pestañas de Métricas. */
private enum class MetricsTab { Physical, Strength }

/** Métricas físicas de las gráficas de evolución (pager de ESTADÍSTICAS + detalle). */
enum class PhysicalMetric { Weight, Fat, Muscle }

@Composable
fun MetricsScreen(
    onOpenDetail: (String) -> Unit = {},
    viewModel: MetricsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MetricsTab.Physical) }

    Column(Modifier.fillMaxSize().background(Gray200).statusBarsPadding()) {
        MetricsSegmented(
            options = listOf(stringResource(R.string.metrics_physical), stringResource(R.string.metrics_strength)),
            selectedIndex = tab.ordinal,
            onSelect = { tab = MetricsTab.entries[it] },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        )
        when (tab) {
            MetricsTab.Physical -> PhysicalContent(state, viewModel, onOpenDetail)
            MetricsTab.Strength -> StrengthContent()
        }
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

// ============================ FÍSICO ============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhysicalContent(
    state: MetricsUiState,
    viewModel: MetricsViewModel,
    onOpenDetail: (String) -> Unit,
) {
    var showAddLog by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showEditMeasures by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<MetricGoal?>(null) }
    var photoToDelete by remember { mutableStateOf<com.hitbosss.domain.model.ProgressPhoto?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onAddPhoto)
    }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
        state.hasError && state.personalInfo == null ->
            Box(Modifier.fillMaxSize(), Alignment.Center) { ErrorConnectionView(onRetry = viewModel::retry) }
        else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                MetricsSectionLabel(stringResource(R.string.metrics_section_state))
                BodyMeasuresCard(state.personalInfo, onEdit = { showEditMeasures = true })

                MetricsSectionLabel(stringResource(R.string.metrics_section_goals))
                GoalCard(
                    title = stringResource(R.string.metrics_weight_goal),
                    goals = state.weightGoals,
                    currentValue = state.personalInfo?.weight?.value,
                    unit = state.weightUnit,
                    lowerIsBetter = (state.personalInfo?.weight?.value ?: 0.0) > (state.weightGoals.active?.target?.value ?: Double.MAX_VALUE),
                    onAdd = { showAddGoal = true },
                    onDelete = { goalToDelete = it },
                )

                MetricsSectionLabel(stringResource(R.string.metrics_section_stats))
                PhysicalEvolutionPager(state, onAddLog = { showAddLog = true }, onOpenDetail = onOpenDetail)

                MetricsSectionLabel(stringResource(R.string.metrics_section_visual))
                ProgressPhotosCard(
                    photos = state.photos,
                    onAdd = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onDelete = { photoToDelete = it },
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAddLog) {
        AddBodyLogSheet(
            unit = state.weightUnit,
            isSaving = state.isSaving,
            onDismiss = { showAddLog = false },
            onSave = { w, f, m -> viewModel.onAddBodyLog(w, f, m); showAddLog = false },
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
            onSave = { viewModel.onAddWeightGoal(it); showAddGoal = false },
        )
    }
    if (showEditMeasures) {
        EditMeasuresSheet(
            info = state.personalInfo,
            unitSystem = state.unitSystem,
            isSaving = state.isSaving,
            onDismiss = { showEditMeasures = false },
            onSave = { h, w -> viewModel.onEditMeasures(h, w); showEditMeasures = false },
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
    photoToDelete?.let { photo ->
        HitPopup(
            title = stringResource(R.string.metrics_photo_delete_title),
            message = stringResource(R.string.metrics_photo_delete_msg),
            confirmText = stringResource(R.string.common_delete),
            confirmType = HitButtonType.Destructive,
            onConfirm = { viewModel.onDeletePhoto(photo.id); photoToDelete = null },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { photoToDelete = null },
            onDismissRequest = { photoToDelete = null },
        )
    }
}

/** Medidas corporales: altura + peso + IMC con slider y clasificación. */
@Composable
private fun BodyMeasuresCard(info: PersonalInfo?, onEdit: () -> Unit) {
    MetricsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metrics_body_measures), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Edit, stringResource(R.string.common_edit), tint = Gray500, modifier = Modifier.size(18.dp).clickable(onClick = onEdit))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeasureBox(stringResource(R.string.metrics_height), info?.height?.value, info?.height?.unit ?: "cm", Modifier.weight(1f))
            MeasureBox(stringResource(R.string.metrics_weight), info?.weight?.value, info?.weight?.unit ?: "kg", Modifier.weight(1f))
        }

        // IMC (siempre a partir de valores métricos)
        val heightCm = info?.height?.let { if (it.unit.equals("in", true)) it.value * 2.54 else it.value } ?: 0.0
        val bmi = BmiCalculator.bmi(info?.bodyWeightKg ?: 0.0, heightCm)
        if (bmi != null) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.metrics_bmi), style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
                Text(formatNum(bmi), style = HitbosssType.titleBody, color = Gray800)
            }
            Spacer(Modifier.height(8.dp))
            BmiSlider(bmi)
            Spacer(Modifier.height(12.dp))
            val band = BmiCalculator.band(bmi)
            val (titleRes, msgRes, color) = when (band) {
                BmiCalculator.Band.Low -> Triple(R.string.metrics_bmi_low, R.string.metrics_bmi_low_msg, Secondary500)
                BmiCalculator.Band.Normal -> Triple(R.string.metrics_bmi_normal, R.string.metrics_bmi_normal_msg, Success500)
                BmiCalculator.Band.Overweight -> Triple(R.string.metrics_bmi_over, R.string.metrics_bmi_over_msg, Orange300)
                BmiCalculator.Band.Obese -> Triple(R.string.metrics_bmi_obese, R.string.metrics_bmi_obese_msg, Error400)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray200).padding(12.dp)) {
                Text(stringResource(titleRes), style = HitbosssType.bodyDefaultEmphasis, color = color)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(msgRes), style = HitbosssType.bodySmallRegular, color = Gray500)
            }
        }
    }
}

@Composable
private fun MeasureBox(label: String, value: Double?, unit: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Gray200).padding(12.dp)) {
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value?.let { formatNum(it) } ?: "--", style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

/** Objetivo (de peso o de fuerza — mismo card): activo con progreso + historial desplegable. */
@Composable
fun GoalCard(
    title: String,
    goals: com.hitbosss.domain.model.MetricGoals,
    currentValue: Double?,
    unit: String,
    lowerIsBetter: Boolean,
    onAdd: () -> Unit,
    onDelete: (MetricGoal) -> Unit,
) {
    var historyOpen by remember { mutableStateOf(false) }
    MetricsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Add, stringResource(R.string.metrics_goal_add_title), tint = Gray500, modifier = Modifier.size(20.dp).clickable(onClick = onAdd))
        }
        val active = goals.active
        if (active == null) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.metrics_goal_empty), style = HitbosssType.bodyDefaultRegular, color = Gray500)
        } else {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(currentValue?.let { formatNum(it) } ?: "--", style = HitbosssType.titleSection, color = Gray800)
                Text(
                    " /${formatNum(active.target.value)} $unit",
                    style = HitbosssType.bodyDefaultRegular, color = Gray500,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (currentValue != null && currentValue > 0) {
                val progress = (minOf(currentValue, active.target.value) / maxOf(currentValue, active.target.value)).toFloat().coerceIn(0f, 1f)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.metrics_progress), style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.weight(1f))
                    Text("${(progress * 100).toInt()}%", style = HitbosssType.bodySmallEmphasis, color = Gray800)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Gray300)) {
                    Box(
                        Modifier.fillMaxWidth(progress).height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (lowerIsBetter) Secondary500 else Primary500),
                    )
                }
                val remaining = kotlin.math.abs(active.target.value - currentValue)
                if (remaining > 0.01) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.metrics_goal_remaining, "${formatNum(remaining)} $unit"),
                        style = HitbosssType.bodySmallRegular, color = Success500,
                    )
                }
            }
        }

        if (goals.history.isNotEmpty() || active != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clickable { historyOpen = !historyOpen },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.metrics_goal_history), style = HitbosssType.bodyDefaultRegular, color = Gray500, modifier = Modifier.weight(1f))
                Icon(
                    if (historyOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    null, tint = Gray500, modifier = Modifier.size(20.dp),
                )
            }
            if (historyOpen) {
                val dateFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
                (listOfNotNull(active) + goals.history).forEach { goal ->
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${stringResource(R.string.metrics_goal)}: ${formatNum(goal.target.value)} $unit", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                            Text(
                                java.time.Instant.ofEpochSecond(goal.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt),
                                style = HitbosssType.bodySmallRegular, color = Gray500,
                            )
                        }
                        Icon(
                            Icons.Filled.Delete, stringResource(R.string.common_delete), tint = Error400,
                            modifier = Modifier.size(18.dp).clickable { onDelete(goal) },
                        )
                    }
                }
            }
        }
    }
}

/** Pager de evolución (Peso / Grasa / Músculo) con dots. */
@Composable
private fun PhysicalEvolutionPager(
    state: MetricsUiState,
    onAddLog: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val pager = rememberPagerState { PhysicalMetric.entries.size }
    HorizontalPager(state = pager, beyondViewportPageCount = 1) { page ->
        val metric = PhysicalMetric.entries[page]
        EvolutionCard(
            metric = metric,
            logs = state.bodyLogs,
            goalValue = if (metric == PhysicalMetric.Weight) state.weightGoals.active?.target?.value else null,
            unit = if (metric == PhysicalMetric.Fat) "%" else state.weightUnit,
            onAddLog = onAddLog,
            onOpenDetail = { onOpenDetail(metric.name.lowercase()) },
        )
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PagerDots(count = PhysicalMetric.entries.size, current = pager.currentPage)
    }
}

@Composable
private fun PhysicalMetric.title(): String = when (this) {
    PhysicalMetric.Weight -> stringResource(R.string.metrics_weight_evolution)
    PhysicalMetric.Fat -> stringResource(R.string.metrics_fat_evolution)
    PhysicalMetric.Muscle -> stringResource(R.string.metrics_muscle_evolution)
}

private fun BodyLog.valueOf(metric: PhysicalMetric): Double? = when (metric) {
    PhysicalMetric.Weight -> weight.value
    PhysicalMetric.Fat -> bodyFatPct
    PhysicalMetric.Muscle -> muscleMass?.value
}

/** Card de evolución con gráfica Semana/Mes, tiles y añadir registro. */
@Composable
private fun EvolutionCard(
    metric: PhysicalMetric,
    logs: List<BodyLog>,
    goalValue: Double?,
    unit: String,
    onAddLog: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    var monthly by rememberSaveable(metric) { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    // Rango temporal visible
    val start = if (monthly) today.withDayOfMonth(1) else today.with(java.time.DayOfWeek.MONDAY)
    val end = if (monthly) start.plusMonths(1) else start.plusWeeks(1)
    val startSec = start.atStartOfDay(zone).toEpochSecond()
    val endSec = end.atStartOfDay(zone).toEpochSecond()
    val span = (endSec - startSec).toFloat()

    val points = logs.mapNotNull { log ->
        val v = log.valueOf(metric) ?: return@mapNotNull null
        if (log.loggedAt < startSec || log.loggedAt >= endSec) return@mapNotNull null
        (log.loggedAt - startSec) / span to v
    }

    val xLabels = if (monthly) {
        listOf("1", "8", "15", "22", stringResource(R.string.metrics_month_end_label))
    } else {
        stringResource(R.string.metrics_week_days).split(",")
    }

    MetricsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(metric.title(), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
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

        if (points.isEmpty() && logs.none { it.valueOf(metric) != null }) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.metrics_empty_logs), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
        } else {
            val series = buildList {
                if (points.isNotEmpty()) {
                    add(ChartSeries(points, color = if (metric == PhysicalMetric.Weight) Secondary500 else Success400, marker = ChartMarker.Circle, fill = true))
                }
                goalValue?.let { add(ChartSeries(listOf(0f to it, 1f to it), color = Success500, dashed = true, marker = ChartMarker.None)) }
            }
            LineChart(series = series, xLabels = xLabels, modifier = Modifier.fillMaxWidth().height(160.dp))
        }

        // Tiles semana actual / pasada + mensaje delta
        val weekStart = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay(zone).toEpochSecond()
        val lastWeekStart = weekStart - 7 * 86400
        fun avgIn(from: Long, to: Long): Double? =
            logs.filter { it.loggedAt in from until to }.mapNotNull { it.valueOf(metric) }
                .takeIf { it.isNotEmpty() }?.average()
        val currentAvg = avgIn(weekStart, weekStart + 7 * 86400)
        val pastAvg = avgIn(lastWeekStart, weekStart)

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeekTile(stringResource(R.string.metrics_current_week), currentAvg, unit, Modifier.weight(1f))
            WeekTile(stringResource(R.string.metrics_past_week), pastAvg, unit, Modifier.weight(1f))
        }
        if (metric == PhysicalMetric.Weight && currentAvg != null && pastAvg != null) {
            val delta = currentAvg - pastAvg
            if (kotlin.math.abs(delta) > 0.001) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        if (delta < 0) R.string.metrics_lost_msg else R.string.metrics_gained_msg,
                        "${formatNum(kotlin.math.abs(delta))} $unit",
                    ),
                    style = HitbosssType.bodySmallRegular,
                    color = if (delta < 0) Error400 else Success500,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Secondary800)
                .clickable(onClick = onAddLog).padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = Gray100, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.metrics_add_log), style = HitbosssType.bodyDefaultEmphasis, color = Gray100)
            }
        }
    }
}

@Composable
private fun WeekTile(label: String, value: Double?, unit: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Gray200).padding(12.dp)) {
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value?.let { formatNum(it) } ?: "--", style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

// ============================ SHEETS ============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBodyLogSheet(
    unit: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (weight: Double, fat: Double?, muscle: Double?) -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.metrics_add_log), style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.height(16.dp))
            NumberField(weight, { weight = it }, stringResource(R.string.metrics_weight), unit)
            Spacer(Modifier.height(12.dp))
            NumberField(fat, { fat = it }, stringResource(R.string.metrics_fat_pct), "%")
            Spacer(Modifier.height(12.dp))
            NumberField(muscle, { muscle = it }, stringResource(R.string.metrics_muscle_mass), unit)
            Spacer(Modifier.height(20.dp))
            SheetButton(stringResource(R.string.metrics_add), enabled = weight.toDoubleOrNull()?.let { it > 0 } == true && !isSaving) {
                onSave(weight.toDouble(), fat.toDoubleOrNull(), muscle.toDoubleOrNull())
            }
        }
    }
}

/** Sheet genérico de un solo valor numérico (objetivo de peso/fuerza, entrenamiento). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleValueSheet(
    title: String,
    hint: String,
    unit: String,
    buttonText: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(title, style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.height(16.dp))
            NumberField(value, { value = it }, hint, unit)
            Spacer(Modifier.height(20.dp))
            SheetButton(buttonText, enabled = value.toDoubleOrNull()?.let { it > 0 } == true && !isSaving) {
                onSave(value.toDouble())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMeasuresSheet(
    info: PersonalInfo?,
    unitSystem: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (height: Double?, weight: Double?) -> Unit,
) {
    var height by remember { mutableStateOf(info?.height?.value?.let { formatNum(it) } ?: "") }
    var weight by remember { mutableStateOf(info?.weight?.value?.let { formatNum(it) } ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.metrics_body_measures), style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.height(16.dp))
            NumberField(height, { height = it }, stringResource(R.string.metrics_height), if (unitSystem == "imperial") "in" else "cm")
            Spacer(Modifier.height(12.dp))
            NumberField(weight, { weight = it }, stringResource(R.string.metrics_weight), if (unitSystem == "imperial") "lbs" else "kg")
            Spacer(Modifier.height(20.dp))
            SheetButton(stringResource(R.string.common_save_changes), enabled = !isSaving) {
                onSave(height.toDoubleOrNull(), weight.toDoubleOrNull())
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, unit: String) {
    Column {
        Text(label, style = HitbosssType.bodySmallEmphasis, color = Gray800, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray200)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { new -> if (new.isEmpty() || new.replace(',', '.').toDoubleOrNull() != null) onChange(new.replace(',', '.')) },
                singleLine = true,
                textStyle = HitbosssType.bodyLargeEmphasis.copy(color = Gray800),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Text(unit, style = HitbosssType.bodyDefaultRegular, color = Gray500)
        }
    }
}

@Composable
private fun SheetButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Secondary800 else Gray300)
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = HitbosssType.bodyLargeEmphasis, color = if (enabled) Gray100 else Gray500)
    }
}

// ============================ FOTOS DE PROGRESO ============================

/** Fotos de progreso: Individual (carrusel) / Comparado (primera vs última) + medidas snapshot. */
@Composable
private fun ProgressPhotosCard(
    photos: List<com.hitbosss.domain.model.ProgressPhoto>,
    onAdd: () -> Unit,
    onDelete: (com.hitbosss.domain.model.ProgressPhoto) -> Unit,
) {
    var compare by rememberSaveable { mutableStateOf(false) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM") }
    fun dateOf(p: com.hitbosss.domain.model.ProgressPhoto): String =
        java.time.Instant.ofEpochSecond(p.takenAt).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)

    MetricsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metrics_progress_photos), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Add, stringResource(R.string.metrics_photo_add), tint = Gray500, modifier = Modifier.size(20.dp).clickable(onClick = onAdd))
        }
        Spacer(Modifier.height(12.dp))
        MetricsSegmented(
            options = listOf(stringResource(R.string.metrics_photos_individual), stringResource(R.string.metrics_photos_compare)),
            selectedIndex = if (compare) 1 else 0,
            onSelect = { compare = it == 1 },
        )
        Spacer(Modifier.height(16.dp))

        if (photos.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.metrics_photos_empty), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
            return@MetricsCard
        }

        if (!compare) {
            val pager = rememberPagerState { photos.size }
            HorizontalPager(state = pager, beyondViewportPageCount = 1) { page ->
                val photo = photos[page]
                Column {
                    ProgressPhotoImage(photo.photoUrl, dateOf(photo), onLongPress = { onDelete(photo) })
                    Spacer(Modifier.height(12.dp))
                    PhotoStatsRow(photo)
                }
            }
            if (photos.size > 1) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PagerDots(count = photos.size, current = pager.currentPage)
                }
            }
        } else {
            // Comparado: la más antigua vs la más reciente (photos viene ordenado por fecha DESC)
            val newest = photos.first()
            val oldest = photos.last()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    ProgressPhotoImage(oldest.photoUrl, dateOf(oldest), onLongPress = { onDelete(oldest) })
                    Spacer(Modifier.height(8.dp))
                    PhotoStatsRow(oldest, compact = true)
                }
                Column(Modifier.weight(1f)) {
                    ProgressPhotoImage(newest.photoUrl, dateOf(newest), onLongPress = { onDelete(newest) })
                    Spacer(Modifier.height(8.dp))
                    PhotoStatsRow(newest, compact = true)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProgressPhotoImage(url: String, dateLabel: String, onLongPress: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(10.dp)).background(Gray300)
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
    ) {
        coil.compose.AsyncImage(
            model = url, contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            dateLabel,
            style = HitbosssType.bodySmallEmphasis, color = Gray800,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .clip(RoundedCornerShape(6.dp)).background(Gray100.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PhotoStatsRow(photo: com.hitbosss.domain.model.ProgressPhoto, compact: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
        PhotoStat(stringResource(R.string.metrics_weight), photo.weight?.let { "${formatNum(it.value)} ${it.unit}" }, Modifier.weight(1f))
        PhotoStat(stringResource(R.string.metrics_fat), photo.bodyFatPct?.let { "${formatNum(it)}%" }, Modifier.weight(1f))
        PhotoStat(stringResource(R.string.metrics_muscle), photo.muscleMass?.let { "${formatNum(it.value)} ${it.unit}" }, Modifier.weight(1f))
    }
}

@Composable
private fun PhotoStat(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Gray200).padding(8.dp)) {
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
        Text(value ?: "--", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
    }
}

/** Formato compacto: sin decimales si es entero, un decimal si no. */
fun formatNum(v: Double): String =
    if (v % 1.0 == 0.0) "${v.toInt()}" else String.format(Locale.US, "%.1f", v)
