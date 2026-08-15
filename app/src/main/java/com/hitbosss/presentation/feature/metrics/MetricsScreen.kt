package com.hitbosss.presentation.feature.metrics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.domain.model.BodyComposition
import com.hitbosss.domain.model.BodyHistoryPoint
import com.hitbosss.domain.model.MetricGoal
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.model.TrendPoint
import com.hitbosss.domain.util.BmiCalculator
import com.hitbosss.presentation.designsystem.components.ChartMarker
import com.hitbosss.presentation.designsystem.components.ChartSeries
import com.hitbosss.presentation.designsystem.components.ErrorConnectionView
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.LineChart
import com.hitbosss.presentation.designsystem.theme.Error400
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Orange300
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.designsystem.theme.Success500
import com.hitbosss.presentation.designsystem.theme.Warning400
import androidx.compose.ui.graphics.Color
import com.hitbosss.domain.model.GoalHistoryEntry
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Sub-pestañas de Métricas. */
private enum class MetricsTab { Physical, Strength }

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
            MetricsTab.Strength -> StrengthContent(onOpenDetail = onOpenDetail)
        }
    }

    state.actionError?.let { error ->
        HitPopup(
            title = state.actionErrorTitle ?: stringResource(R.string.common_something_wrong),
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
    var addRecordMetric by remember { mutableStateOf<PhysicalMetric?>(null) }
    var goalMetric by remember { mutableStateOf<PhysicalMetric?>(null) }
    var showEditMeasures by remember { mutableStateOf(false) }
    var showEditComposition by remember { mutableStateOf(false) }
    var showPhotoSource by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onAddPhoto)
    }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let(viewModel::onAddPhoto)
    }
    fun launchCamera() {
        val dir = java.io.File(context.cacheDir, "progress").apply { mkdirs() }
        val file = java.io.File(dir, "cam_${System.currentTimeMillis()}.jpg")
        cameraUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        takePhoto.launch(cameraUri!!)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
        state.hasError && state.personalInfo == null ->
            Box(Modifier.fillMaxSize(), Alignment.Center) { ErrorConnectionView(onRetry = viewModel::retry) }
        else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                MetricsSectionLabel(stringResource(R.string.metrics_section_state))
                BodyStatePager(
                    state = state,
                    onEditMeasures = { showEditMeasures = true },
                    onEditComposition = { showEditComposition = true },
                )

                MetricsSectionLabel(stringResource(R.string.metrics_section_goals))
                GoalsPager(state, viewModel, onAdd = { goalMetric = it })

                MetricsSectionLabel(stringResource(R.string.metrics_section_stats))
                PhysicalEvolutionPager(state, viewModel, onAddLog = { addRecordMetric = it }, onOpenDetail = onOpenDetail)

                MetricsSectionLabel(stringResource(R.string.metrics_section_visual))
                ProgressPhotosCard(
                    photos = state.photos,
                    unit = state.weightUnit,
                    onAdd = { showPhotoSource = true },
                    onDelete = viewModel::onDeletePhoto,
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    addRecordMetric?.let { metric ->
        SingleValueSheet(
            title = stringResource(R.string.metrics_record_add_title_fmt, metric.label()),
            description = stringResource(R.string.metrics_record_add_desc),
            hint = stringResource(R.string.metrics_record_new),
            unit = state.unitFor(metric),
            buttonText = stringResource(R.string.metrics_record_add),
            isSaving = state.isSaving,
            onDismiss = { addRecordMetric = null },
            onSave = { viewModel.onAddRecord(metric, it); addRecordMetric = null },
        )
    }
    goalMetric?.let { metric ->
        SingleValueSheet(
            title = stringResource(R.string.metrics_goal_add_title_fmt, metric.label()),
            description = stringResource(R.string.metrics_goal_add_desc),
            hint = stringResource(R.string.metrics_goal_add_hint),
            unit = state.unitFor(metric),
            buttonText = stringResource(R.string.metrics_goal_add_title),
            isSaving = state.isSaving,
            onDismiss = { goalMetric = null },
            onSave = { viewModel.onCreateGoal(metric, it); goalMetric = null },
        )
    }
    if (showEditMeasures) {
        EditMeasuresSheet(
            info = state.personalInfo,
            currentWeight = state.composition?.weightKg,
            unitSystem = state.unitSystem,
            isSaving = state.isSaving,
            onDismiss = { showEditMeasures = false },
            onSave = { h, w -> viewModel.onEditMeasures(h, w); showEditMeasures = false },
        )
    }
    if (showPhotoSource) {
        PhotoSourceSheet(
            onCamera = {
                showPhotoSource = false
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED) launchCamera()
                else cameraPermission.launch(android.Manifest.permission.CAMERA)
            },
            onGallery = {
                showPhotoSource = false
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDismiss = { showPhotoSource = false },
        )
    }
    if (showEditComposition) {
        EditCompositionSheet(
            composition = state.composition,
            unit = state.weightUnit,
            isSaving = state.isSaving,
            onDismiss = { showEditComposition = false },
            onSave = { f, m -> viewModel.onEditComposition(f, m); showEditComposition = false },
        )
    }
}

@Composable
private fun PhysicalMetric.label(): String = when (this) {
    PhysicalMetric.Weight -> stringResource(R.string.metrics_weight)
    PhysicalMetric.Fat -> stringResource(R.string.metrics_fat)
    PhysicalMetric.Muscle -> stringResource(R.string.metrics_muscle)
}

@Composable
private fun PhysicalMetric.goalTitle(): String = when (this) {
    PhysicalMetric.Weight -> stringResource(R.string.metrics_weight_goal)
    PhysicalMetric.Fat -> stringResource(R.string.metrics_fat_goal)
    PhysicalMetric.Muscle -> stringResource(R.string.metrics_muscle_goal)
}

/** Color por métrica (diseño): peso azul, grasa ámbar, músculo rojo. */
fun metricColor(metric: String): Color = when (metric) {
    "fat" -> Warning400
    "muscle" -> Error400
    else -> Secondary500
}
private fun PhysicalMetric.color(): Color = metricColor(apiKey)

/** ESTADO FÍSICO: Medidas corporales (M1) y Composición (M2) en scroll lateral con dots. */
@Composable
private fun BodyStatePager(state: MetricsUiState, onEditMeasures: () -> Unit, onEditComposition: () -> Unit) {
    val pager = rememberPagerState { 2 }
    HorizontalPager(state = pager, beyondViewportPageCount = 1, verticalAlignment = Alignment.Top) { page ->
        if (page == 0) BodyMeasuresCard(state, onEdit = onEditMeasures)
        else CompositionCard(state.composition, state.weightUnit, onEdit = onEditComposition)
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PagerDots(count = 2, current = pager.currentPage) }
}

/** OBJETIVOS: Peso / Grasa / Músculo en scroll lateral con dots (M3). */
@Composable
private fun GoalsPager(state: MetricsUiState, viewModel: MetricsViewModel, onAdd: (PhysicalMetric) -> Unit) {
    val pager = rememberPagerState { PhysicalMetric.entries.size }
    HorizontalPager(state = pager, beyondViewportPageCount = 1, verticalAlignment = Alignment.Top) { page ->
        val metric = PhysicalMetric.entries[page]
        GoalCard(
            title = metric.goalTitle(),
            goal = state.goals[metric],
            unit = state.unitFor(metric),
            onAdd = { onAdd(metric) },
            history = state.goalHistory[metric] ?: emptyList(),
            barColor = metric.color(),
            showHistory = true,
            onExpand = { viewModel.loadGoalHistory(metric) },
            onDelete = viewModel::onDeleteGoal,
        )
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PagerDots(count = PhysicalMetric.entries.size, current = pager.currentPage)
    }
}

/** Medidas corporales: altura + peso + IMC con slider y clasificación. */
@Composable
private fun BodyMeasuresCard(state: MetricsUiState, onEdit: () -> Unit) {
    val info = state.personalInfo
    val weightVal = state.composition?.weightKg ?: info?.weight?.value
    MetricsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metrics_body_measures), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Edit, stringResource(R.string.common_edit), tint = Gray500, modifier = Modifier.size(18.dp).clickable(onClick = onEdit))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeasureBox(stringResource(R.string.metrics_height), info?.height?.value, info?.height?.unit ?: "cm", Modifier.weight(1f))
            MeasureBox(stringResource(R.string.metrics_weight), weightVal, state.weightUnit, Modifier.weight(1f))
        }

        val heightCm = info?.height?.let { if (it.unit.equals("in", true)) it.value * 2.54 else it.value } ?: 0.0
        val weightKg = state.composition?.weightKg?.let { if (state.unitSystem == "imperial") it / 2.20462 else it }
            ?: info?.bodyWeightKg ?: 0.0
        val bmi = BmiCalculator.bmi(weightKg, heightCm)
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
            val badgeRes = when (band) {
                BmiCalculator.Band.Low -> R.string.metrics_bmi_low_badge
                BmiCalculator.Band.Normal -> R.string.metrics_bmi_normal_badge
                BmiCalculator.Band.Overweight -> R.string.metrics_bmi_over_badge
                BmiCalculator.Band.Obese -> R.string.metrics_bmi_obese_badge
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray200).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(titleRes), style = HitbosssType.bodyDefaultEmphasis, color = color, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(badgeRes),
                        style = HitbosssType.bodySmallEmphasis, color = color,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(msgRes), style = HitbosssType.bodySmallRegular, color = Gray500)
            }
        }
    }
}

/** Composición corporal (M2): descripción + barra de proporción + filas Músculo/Grasa/Otras (kg y %). */
@Composable
private fun CompositionCard(composition: BodyComposition?, unit: String, onEdit: () -> Unit) {
    var showOtherInfo by remember { mutableStateOf(false) }
    MetricsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metrics_composition), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Edit, stringResource(R.string.common_edit), tint = Gray500, modifier = Modifier.size(18.dp).clickable(onClick = onEdit))
        }
        Text(stringResource(R.string.metrics_composition_desc), style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        ProportionBar(composition?.muscleKg, composition?.fatKg, composition?.otherKg)
        Spacer(Modifier.height(10.dp))
        CompositionRow(Error400, stringResource(R.string.metrics_muscle), composition?.muscleKg, composition?.musclePercent, unit)
        CompositionRow(Warning400, stringResource(R.string.metrics_fat), composition?.fatKg, composition?.fatPercent, unit)
        CompositionRow(Gray400, stringResource(R.string.metrics_other), composition?.otherKg, composition?.otherPercent, unit, onInfo = { showOtherInfo = true })
    }
    if (showOtherInfo) {
        HitPopup(
            title = stringResource(R.string.metrics_other_info_title),
            message = stringResource(R.string.metrics_other_info_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = { showOtherInfo = false },
            onDismissRequest = { showOtherInfo = false },
        )
    }
}

/** Barra proporcional músculo/grasa/otras. Gris completa si no hay datos. */
@Composable
private fun ProportionBar(muscle: Double?, fat: Double?, other: Double?) {
    val m = (muscle ?: 0.0).toFloat().coerceAtLeast(0f)
    val f = (fat ?: 0.0).toFloat().coerceAtLeast(0f)
    val o = (other ?: 0.0).toFloat().coerceAtLeast(0f)
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Gray400)) {
        if (m + f + o > 0f) {
            if (m > 0f) Box(Modifier.fillMaxHeight().weight(m).background(Error400))
            if (f > 0f) Box(Modifier.fillMaxHeight().weight(f).background(Warning400))
            if (o > 0f) Box(Modifier.fillMaxHeight().weight(o).background(Gray400))
        }
    }
}

@Composable
private fun CompositionRow(dot: Color, label: String, kg: Double?, pct: Double?, unit: String, onInfo: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(label, style = HitbosssType.bodyDefaultRegular, color = Gray500)
        if (onInfo != null) { Spacer(Modifier.width(4.dp)); InfoIcon(onInfo) }
        Spacer(Modifier.weight(1f))
        if (kg != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${formatNum(kg)} $unit", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                pct?.let {
                    Spacer(Modifier.width(8.dp))
                    Text("${formatNum(it)}%", style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 1.dp))
                }
            }
        } else {
            NoDataBadge()
        }
    }
}

@Composable
private fun NoDataBadge() {
    Text(
        stringResource(R.string.metrics_no_data),
        style = HitbosssType.bodySmallEmphasis, color = Gray500,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Gray300).padding(horizontal = 8.dp, vertical = 3.dp),
    )
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

/**
 * Objetivo (M3). Barra de color por métrica, progreso derivado, y "Historial de objetivos" desplegable
 * (con check de cumplido y borrado por pulsación larga). Reutilizado en Fuerza con history vacío.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoalCard(
    title: String,
    goal: MetricGoal?,
    unit: String,
    onAdd: () -> Unit,
    history: List<GoalHistoryEntry> = emptyList(),
    barColor: Color = Primary500,
    showHistory: Boolean = false,
    onExpand: () -> Unit = {},
    onDelete: (Long) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<GoalHistoryEntry?>(null) }

    MetricsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Add, stringResource(R.string.metrics_goal_add_title), tint = Gray500, modifier = Modifier.size(20.dp).clickable(onClick = onAdd))
        }
        if (goal == null) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.metrics_goal_empty), style = HitbosssType.bodyDefaultRegular, color = Gray500)
        } else {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatNum(goal.currentValue), style = HitbosssType.titleSection, color = Gray800)
                Text(" /${formatNum(goal.targetValue)} $unit", style = HitbosssType.bodyDefaultRegular, color = Gray500, modifier = Modifier.padding(bottom = 4.dp))
            }
            val progress = goal.progress.toFloat().coerceIn(0f, 1f)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.metrics_progress), style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.weight(1f))
                Text("${(progress * 100).toInt()}%", style = HitbosssType.bodySmallEmphasis, color = Gray800)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Gray300)) {
                Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(RoundedCornerShape(4.dp)).background(barColor))
            }
            // Al 100% (objetivo alcanzado) no tiene sentido seguir mostrando la diferencia → felicitación.
            val remaining = kotlin.math.abs(goal.targetValue - goal.currentValue)
            if (progress >= 1f) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.metrics_goal_reached),
                    style = HitbosssType.bodySmallEmphasis, color = Success500,
                )
            } else if (remaining > 0.01 && goal.currentValue > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.metrics_goal_remaining, "${formatNum(remaining)} $unit"),
                    style = HitbosssType.bodySmallRegular, color = Success500,
                )
            }
        }

        // Historial de objetivos (desplegable): carga bajo demanda al abrir.
        if (showHistory) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300))
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded; if (expanded) onExpand() }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.metrics_goal_history), style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Gray500, modifier = Modifier.size(20.dp))
            }
            if (expanded) {
                if (history.isEmpty()) {
                    Text(stringResource(R.string.metrics_goal_empty), style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 8.dp))
                } else {
                    history.forEach { entry ->
                        GoalHistoryRow(entry, unit, onLongPress = { pendingDelete = entry })
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        HitPopup(
            title = stringResource(R.string.metrics_goal_delete_title),
            message = stringResource(R.string.metrics_goal_delete_msg),
            confirmText = stringResource(R.string.common_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = { entry.id?.let(onDelete); pendingDelete = null },
            onCancel = { pendingDelete = null },
            onDismissRequest = { pendingDelete = null },
        )
    }
}

/** Fila del historial: check de cumplido + "Objetivo: X kg" + fecha. Pulsación larga → borrar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalHistoryRow(entry: GoalHistoryEntry, unit: String, onLongPress: () -> Unit) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es")) }
    val date = java.time.Instant.ofEpochSecond(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)
    Row(
        Modifier.fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalCheck(entry.reached)
        Spacer(Modifier.width(10.dp))
        Text(
            "${stringResource(R.string.metrics_goal)}: ${formatNum(entry.targetValue)} $unit",
            style = HitbosssType.bodyDefaultRegular, color = Gray800, modifier = Modifier.weight(1f),
        )
        Text(date, style = HitbosssType.bodySmallRegular, color = Gray500)
        Spacer(Modifier.width(8.dp))
        // Botón de borrar visible (además de la pulsación larga, poco descubrible).
        Icon(
            Icons.Filled.Delete, stringResource(R.string.common_delete), tint = Gray500,
            modifier = Modifier.size(18.dp).clickable(onClick = onLongPress),
        )
    }
}

/** Círculo de estado del objetivo: verde con check si cumplido, vacío si no / activo. */
@Composable
private fun GoalCheck(reached: Boolean?) {
    if (reached == true) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(Success500), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = Gray100, modifier = Modifier.size(13.dp))
        }
    } else {
        Box(Modifier.size(20.dp).clip(CircleShape).background(Gray300))
    }
}

/** Pager de evolución (Peso / Grasa / Músculo) con dots. */
@Composable
private fun PhysicalEvolutionPager(
    state: MetricsUiState,
    viewModel: MetricsViewModel,
    onAddLog: (PhysicalMetric) -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val pager = rememberPagerState { PhysicalMetric.entries.size }
    HorizontalPager(state = pager, beyondViewportPageCount = 1) { page ->
        val metric = PhysicalMetric.entries[page]
        EvolutionCard(
            metric = metric,
            state = state,
            loadHistory = viewModel::loadHistory,
            loadTrend = viewModel::loadTrend,
            goalValue = state.goals[metric]?.targetValue,
            unit = state.unitFor(metric),
            onAddLog = { onAddLog(metric) },
            onOpenDetail = { onOpenDetail(metric.apiKey) },
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

/** Card de evolución con gráfica Semana/Mes (historial del servidor), tiles y añadir registro. */
@Composable
private fun EvolutionCard(
    metric: PhysicalMetric,
    state: MetricsUiState,
    loadHistory: (PhysicalMetric, String) -> Unit,
    loadTrend: (String) -> Unit,
    goalValue: Double?,
    unit: String,
    onAddLog: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    var monthly by rememberSaveable(metric) { mutableStateOf(false) }
    val timeframe = if (monthly) "month" else "week"
    LaunchedEffect(metric, timeframe) { loadHistory(metric, timeframe) }
    LaunchedEffect(timeframe) { loadTrend(timeframe) }
    val history: List<BodyHistoryPoint> = state.history[metric to timeframe] ?: emptyList()
    // Registros abiertos por el ⓘ de un tile: (esPeríodoActual, puntos).
    var registros by remember(metric) { mutableStateOf<Pair<Boolean, List<TrendPoint>>?>(null) }

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val start = if (monthly) today.minusWeeks(3).with(java.time.DayOfWeek.MONDAY) else today.with(java.time.DayOfWeek.MONDAY)
    val end = if (monthly) start.plusWeeks(4) else start.plusWeeks(1)
    val startSec = start.atStartOfDay(zone).toEpochSecond()
    val endSec = end.atStartOfDay(zone).toEpochSecond()
    val span = (endSec - startSec).toFloat().coerceAtLeast(1f)

    // (fracción X, valor, fecha) — la fecha alimenta el tooltip.
    val pts = history.mapNotNull { p ->
        if (p.measuredAt < startSec || p.measuredAt >= endSec) return@mapNotNull null
        Triple((p.measuredAt - startSec) / span, p.value, p.measuredAt)
    }
    val points = pts.map { it.first to it.second }
    var selectedPoint by remember(metric, timeframe) { mutableStateOf<Int?>(null) }
    val pointFmt = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale("es")) }

    val xLabels = if (monthly) listOf("1", "2", "3", "4")
    else stringResource(R.string.metrics_week_days).split(",")

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

        if (history.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.metrics_empty_logs), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
        } else {
            val series = buildList {
                if (points.isNotEmpty()) {
                    add(ChartSeries(points, color = metric.color(), marker = ChartMarker.Circle, fill = true))
                }
                goalValue?.let { add(ChartSeries(listOf(0f to it, 1f to it), color = Success500, dashed = true, marker = ChartMarker.None)) }
            }
            LineChart(
                series = series,
                xLabels = xLabels,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                selectedIndex = selectedPoint,
                onSelect = { selectedPoint = it },
                pointLabel = { i ->
                    val (_, v, date) = pts[i]
                    val d = java.time.Instant.ofEpochSecond(date).atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(pointFmt).replaceFirstChar { it.uppercase() }
                    "$d · ${formatNum(v)} $unit"
                },
            )
        }

        // Tendencia (M5): media del período actual vs anterior (endpoint trend). ⓘ abre "Registros".
        val trend = state.trend[timeframe]?.forMetric(metric.apiKey)
        val currentVal = trend?.currentAvg
        val prevVal = trend?.previousAvg
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeekTile(
                label = stringResource(if (monthly) R.string.metrics_current_month else R.string.metrics_current_week),
                value = currentVal, unit = unit, modifier = Modifier.weight(1f),
                onInfo = trend?.let { t -> { registros = true to t.current } },
            )
            WeekTile(
                label = stringResource(if (monthly) R.string.metrics_past_month else R.string.metrics_past_week),
                value = prevVal, unit = unit, modifier = Modifier.weight(1f),
                onInfo = trend?.let { t -> { registros = false to t.previous } },
            )
        }
        if (currentVal != null && prevVal != null) {
            val delta = currentVal - prevVal
            if (kotlin.math.abs(delta) > 0.001) {
                Spacer(Modifier.height(8.dp))
                // "Bueno" según la métrica. Grasa: bajar; músculo: subir. Peso: depende del OBJETIVO
                // del usuario (si quiere subir y sube → verde; si no hay objetivo → neutro).
                val good = when (metric) {
                    PhysicalMetric.Fat -> delta < 0
                    PhysicalMetric.Muscle -> delta > 0
                    PhysicalMetric.Weight -> state.goals[metric]
                        ?.takeIf { it.targetValue != it.currentValue }
                        ?.let { (delta > 0) == (it.targetValue > it.currentValue) }
                }
                Text(
                    stringResource(
                        if (delta < 0) R.string.metrics_lost_msg else R.string.metrics_gained_msg,
                        "${formatNum(kotlin.math.abs(delta))} $unit",
                    ),
                    style = HitbosssType.bodySmallRegular,
                    color = when (good) { true -> Success500; false -> Error400; null -> Gray500 },
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
                Text(stringResource(R.string.metrics_add_record), style = HitbosssType.bodyDefaultEmphasis, color = Gray100)
            }
        }
    }

    registros?.let { (esActual, pts) ->
        val body = buildString {
            append(registrosText(pts, monthly, unit))
            if (esActual) { append("\n\n"); append(stringResource(R.string.metrics_records_disclaimer)) }
        }
        HitPopup(
            title = stringResource(R.string.metrics_records),
            message = body.ifBlank { stringResource(R.string.metrics_no_compare) },
            confirmText = stringResource(R.string.common_accept),
            onConfirm = { registros = null },
            onDismissRequest = { registros = null },
        )
    }
}

/** Texto del popup "Registros": semana → días con dato; mes → 4 semanas (o SIN DATOS). */
@Composable
private fun registrosText(points: List<TrendPoint>, monthly: Boolean, unit: String): String {
    if (monthly) {
        return points.mapIndexed { i, p ->
            val v = p.value?.let { "${formatNum(it)} $unit" } ?: stringResource(R.string.metrics_no_data)
            "${stringResource(R.string.metrics_week)} ${i + 1}: $v"
        }.joinToString("\n")
    }
    val fmt = remember { DateTimeFormatter.ofPattern("EEEE d MMM", Locale("es")) }
    val zone = ZoneId.systemDefault()
    return points.filter { it.value != null }.joinToString("\n") { p ->
        val d = java.time.Instant.ofEpochSecond(p.date).atZone(zone).toLocalDate().format(fmt)
            .replaceFirstChar { it.uppercase() }
        "$d: ${formatNum(p.value!!)} $unit"
    }
}

@Composable
private fun WeekTile(label: String, value: Double?, unit: String, modifier: Modifier = Modifier, onInfo: (() -> Unit)? = null) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Gray200).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.weight(1f))
            if (onInfo != null) InfoIcon(onInfo)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value?.let { formatNum(it) } ?: "--", style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

// ============================ SHEETS ============================

/** Sheet genérico de un solo valor numérico (registro, objetivo, entrenamiento). */
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
    description: String? = null,
) {
    var value by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(title, style = HitbosssType.titleBody, color = Gray800)
            if (description != null) {
                Spacer(Modifier.height(6.dp))
                Text(description, style = HitbosssType.bodySmallRegular, color = Gray500)
            }
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
    currentWeight: Double?,
    unitSystem: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (height: Double?, weight: Double?) -> Unit,
) {
    var height by remember { mutableStateOf(info?.height?.value?.let { formatNum(it) } ?: "") }
    var weight by remember { mutableStateOf(currentWeight?.let { formatNum(it) } ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.metrics_edit_measures), style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.metrics_edit_measures_desc), style = HitbosssType.bodySmallRegular, color = Gray500)
            Spacer(Modifier.height(16.dp))
            NumberField(weight, { weight = it }, stringResource(R.string.metrics_weight), if (unitSystem == "imperial") "lbs" else "kg")
            Spacer(Modifier.height(12.dp))
            NumberField(height, { height = it }, stringResource(R.string.metrics_height), if (unitSystem == "imperial") "in" else "cm")
            Spacer(Modifier.height(20.dp))
            SheetButton(stringResource(R.string.metrics_update_data), enabled = !isSaving) {
                onSave(height.toDoubleOrNull(), weight.toDoubleOrNull())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCompositionSheet(
    composition: BodyComposition?,
    unit: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (fatKg: Double?, muscleKg: Double?) -> Unit,
) {
    var fat by remember { mutableStateOf(composition?.fatKg?.let { formatNum(it) } ?: "") }
    var muscle by remember { mutableStateOf(composition?.muscleKg?.let { formatNum(it) } ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.metrics_edit_composition), style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.metrics_edit_composition_desc), style = HitbosssType.bodySmallRegular, color = Gray500)
            Spacer(Modifier.height(16.dp))
            NumberField(muscle, { muscle = it }, stringResource(R.string.metrics_muscle), unit)
            Spacer(Modifier.height(12.dp))
            NumberField(fat, { fat = it }, stringResource(R.string.metrics_fat), unit)
            Spacer(Modifier.height(20.dp))
            SheetButton(stringResource(R.string.metrics_update_data), enabled = !isSaving) {
                onSave(fat.toDoubleOrNull(), muscle.toDoubleOrNull())
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

/** Action sheet de origen de foto: Hacer foto / Subir foto de Galería / Cancelar (1:1 con iOS). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(onCamera: () -> Unit, onGallery: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.metrics_photo_camera),
                style = HitbosssType.bodyLargeRegular, color = Gray800, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onCamera).padding(horizontal = 24.dp, vertical = 16.dp),
            )
            Text(
                stringResource(R.string.metrics_photo_gallery),
                style = HitbosssType.bodyLargeRegular, color = Secondary500, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onGallery).padding(horizontal = 24.dp, vertical = 16.dp),
            )
            Text(
                stringResource(R.string.common_cancel),
                style = HitbosssType.bodyLargeEmphasis, color = Gray800, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

/** Fotos de progreso: Individual (carrusel) / Comparado (arrastrar/tocar dos fotos + diferencia) + snapshot. */
@Composable
private fun ProgressPhotosCard(
    photos: List<com.hitbosss.domain.model.ProgressPhoto>,
    unit: String,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit = {},
) {
    var compare by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<com.hitbosss.domain.model.ProgressPhoto?>(null) }
    var fullscreenPhoto by remember { mutableStateOf<String?>(null) }
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
                    Box {
                        ProgressPhotoImage(photo.photoUrl, dateOf(photo), onClick = { fullscreenPhoto = photo.photoUrl })
                        if (photo.id != null) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).clip(CircleShape)
                                    .background(Gray800.copy(alpha = 0.55f)).clickable { pendingDelete = photo },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.common_delete), tint = Gray100, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PhotoStatsRow(photo, unit)
                }
            }
            if (photos.size > 1) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PagerDots(count = photos.size, current = pager.currentPage)
                }
            }
        } else {
            // Comparado: dos huecos + carrusel. Tocar una miniatura la asigna al primer hueco libre
            // (tocar la seleccionada la quita). Con ambos huecos → fila de diferencias.
            // Fotos vienen DESC (más reciente primero): izquierda = más antigua, derecha = más reciente.
            var slotA by rememberSaveable { mutableStateOf(photos.lastIndex) }
            var slotB by rememberSaveable { mutableStateOf(if (photos.size > 1) 0 else -1) }
            val a = photos.getOrNull(slotA)
            val b = photos.getOrNull(slotB)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    if (a != null) { ProgressPhotoImage(a.photoUrl, dateOf(a)); Spacer(Modifier.height(8.dp)); PhotoStatsRow(a, unit, compact = true) } else EmptySlot()
                }
                Column(Modifier.weight(1f)) {
                    if (b != null) { ProgressPhotoImage(b.photoUrl, dateOf(b)); Spacer(Modifier.height(8.dp)); PhotoStatsRow(b, unit, compact = true) } else EmptySlot()
                }
            }
            if (a != null && b != null) {
                Spacer(Modifier.height(10.dp))
                PhotoDiffRow(a, b, unit)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.metrics_photo_drag_hint), style = HitbosssType.bodySmallRegular, color = Gray500)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEachIndexed { i, p ->
                    val sel = i == slotA || i == slotB
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                                .then(if (sel) Modifier.border(2.dp, Primary500, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable {
                                    when {
                                        slotA == i -> slotA = -1
                                        slotB == i -> slotB = -1
                                        slotA < 0 -> slotA = i
                                        slotB < 0 -> slotB = i
                                        else -> slotB = i
                                    }
                                },
                        ) {
                            coil.compose.AsyncImage(
                                model = p.photoUrl, contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(dateOf(p), style = HitbosssType.bodySmallRegular, color = Gray500)
                    }
                }
            }
        }
    }

    pendingDelete?.let { photo ->
        HitPopup(
            title = stringResource(R.string.metrics_photo_delete_title),
            message = stringResource(R.string.metrics_photo_delete_msg),
            confirmText = stringResource(R.string.common_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = { photo.id?.let(onDelete); pendingDelete = null },
            onCancel = { pendingDelete = null },
            onDismissRequest = { pendingDelete = null },
        )
    }

    // Foto a pantalla completa al tocarla (igual que la foto de perfil). Tap para cerrar.
    fullscreenPhoto?.let { url ->
        Dialog(onDismissRequest = { fullscreenPhoto = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.95f))
                    .clickable { fullscreenPhoto = null },
                contentAlignment = Alignment.Center,
            ) {
                coil.compose.AsyncImage(
                    model = url, contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProgressPhotoImage(url: String, dateLabel: String, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(10.dp)).background(Gray300)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
private fun PhotoStatsRow(photo: com.hitbosss.domain.model.ProgressPhoto, unit: String, compact: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
        PhotoStat(stringResource(R.string.metrics_weight), photo.weightKg?.let { "${formatNum(it)} $unit" }, Modifier.weight(1f))
        PhotoStat(stringResource(R.string.metrics_fat), photo.fatPercent?.let { "${formatNum(it)}%" }, Modifier.weight(1f))
        PhotoStat(stringResource(R.string.metrics_muscle), photo.muscleKg?.let { "${formatNum(it)} $unit" }, Modifier.weight(1f))
    }
}

@Composable
private fun PhotoStat(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Gray200).padding(8.dp)) {
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
        Text(value ?: "--", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
    }
}

/** Hueco vacío del modo Comparado (aún sin foto asignada). */
@Composable
private fun EmptySlot() {
    Box(
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(10.dp)).background(Gray200),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.Add, null, tint = Gray400, modifier = Modifier.size(32.dp)) }
}

/** Fila de diferencias entre dos fotos (B − A): peso/grasa/músculo, coloreada por sentido. */
@Composable
private fun PhotoDiffRow(a: com.hitbosss.domain.model.ProgressPhoto, b: com.hitbosss.domain.model.ProgressPhoto, unit: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DiffStat(stringResource(R.string.metrics_weight), photoDiff(a.weightKg, b.weightKg), unit, goodUp = true, Modifier.weight(1f))
        DiffStat(stringResource(R.string.metrics_fat), photoDiff(a.fatPercent, b.fatPercent), "%", goodUp = false, Modifier.weight(1f))
        DiffStat(stringResource(R.string.metrics_muscle), photoDiff(a.muscleKg, b.muscleKg), unit, goodUp = true, Modifier.weight(1f))
    }
}

private fun photoDiff(a: Double?, b: Double?): Double? = if (a != null && b != null) b - a else null

@Composable
private fun DiffStat(label: String, delta: Double?, unit: String, goodUp: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Gray200).padding(8.dp)) {
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
        if (delta == null) {
            Text("--", style = HitbosssType.bodyDefaultEmphasis, color = Gray500)
        } else {
            val neutral = kotlin.math.abs(delta) < 0.05
            val good = if (goodUp) delta > 0 else delta < 0
            val sign = if (delta > 0) "+" else ""
            Text(
                "$sign${formatNum(delta)} $unit",
                style = HitbosssType.bodyDefaultEmphasis,
                color = if (neutral) Gray800 else if (good) Success500 else Error400,
            )
        }
    }
}

/** Formato compacto: sin decimales si es entero, un decimal si no. */
fun formatNum(v: Double): String =
    if (v % 1.0 == 0.0) "${v.toInt()}" else String.format(Locale.US, "%.1f", v)
