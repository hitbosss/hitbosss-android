package com.hitbosss.presentation.feature.metrics

import android.text.Layout
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import kotlin.math.absoluteValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Red500
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary600
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.designsystem.theme.Success500
import com.hitbosss.presentation.designsystem.theme.Warning400
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.hitbosss.R.string.metrics_current_week
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
    onOpenUserProfile: (String) -> Unit = {},
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
            MetricsTab.Strength -> StrengthContent(onOpenDetail = onOpenDetail, onOpenUserProfile = onOpenUserProfile)
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
    // iOS pone ambas cards a la MISMA altura (HStack = la más alta). Medimos las dos y fijamos las dos a
    // la máxima, con fondo blanco que rellena → mismo alto y sin huecos grises. ponytail: si los datos
    // cambiaran tras la carga inicial no re-expande (recarga recompone); es suficiente aquí.
    val heights = remember { mutableStateMapOf<Int, Int>() }
    val density = LocalDensity.current
    val maxH = if (heights.size == 2) heights.values.max() else null
    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.Top,
        modifier = if (maxH != null) Modifier.height(with(density) { maxH.toDp() }) else Modifier,
    ) { page ->
        val cardMod = if (maxH != null) Modifier.height(with(density) { maxH.toDp() }) else Modifier
        Box(Modifier.onSizeChanged { heights[page] = it.height }) {
            if (page == 0) BodyMeasuresCard(state, onEdit = onEditMeasures, modifier = cardMod)
            else CompositionCard(state.composition, state.weightUnit, onEdit = onEditComposition, modifier = cardMod)
        }
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
            onDelete = { goalId -> viewModel.onDeleteGoal(metric, goalId) },
        )
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PagerDots(count = PhysicalMetric.entries.size, current = pager.currentPage)
    }
}

/** Medidas corporales: altura + peso + IMC con slider y clasificación. */
@Composable
private fun BodyMeasuresCard(state: MetricsUiState, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val info = state.personalInfo
    val weightVal = state.composition?.weightKg ?: info?.weight?.value
    // SpaceBetween: cuando la card se estira a la altura de la otra, reparte el hueco entre los elementos.
    MetricsCard(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metrics_body_measures), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Edit, stringResource(R.string.common_edit), tint = Gray500, modifier = Modifier.size(18.dp).clickable(onClick = onEdit))
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeasureBox(stringResource(R.string.metrics_height), info?.height?.value, info?.height?.unit ?: "cm", Modifier.weight(1f))
            MeasureBox(stringResource(R.string.metrics_weight), weightVal, state.weightUnit, Modifier.weight(1f))
        }

        val heightCm = info?.height?.let { if (it.unit.equals("in", true)) it.value * 2.54 else it.value } ?: 0.0
        val weightKg = state.composition?.weightKg?.let { if (state.unitSystem == "imperial") it / 2.20462 else it }
            ?: info?.bodyWeightKg ?: 0.0
        val bmi = BmiCalculator.bmi(weightKg, heightCm)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metrics_bmi), style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
            if (bmi != null) Text(formatNum(bmi), style = HitbosssType.titleBody, color = Gray800) else NoDataBadge()
        }
        Spacer(Modifier.height(8.dp))
        BmiSlider(bmi)
        Spacer(Modifier.height(12.dp))
        if (bmi != null) {
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
            BmiInfoBox(iconColor = color, title = stringResource(titleRes), badge = stringResource(badgeRes), badgeColor = color, msg = stringResource(msgRes))
        } else {
            BmiInfoBox(iconColor = Secondary500, title = stringResource(R.string.metrics_bmi_nodata), badge = null, badgeColor = Secondary500, msg = stringResource(R.string.metrics_bmi_nodata_msg))
        }
    }
}

/** Caja informativa del IMC: icono de color + título + badge sólido (opcional) + descripción. */
@Composable
private fun BmiInfoBox(iconColor: Color, title: String, badge: String?, badgeColor: Color, msg: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray100).border(1.dp, Gray400, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
            if (badge != null) {
                Text(
                    badge,
                    style = HitbosssType.bodySmallEmphasis, color = Gray100,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(badgeColor).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(msg, style = HitbosssType.bodySmallRegular, color = Gray500)
    }
}

/** Composición corporal (M2): descripción + barra de proporción + filas Músculo/Grasa/Otras (kg y %). */
@Composable
private fun CompositionCard(composition: BodyComposition?, unit: String, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    var showOtherInfo by remember { mutableStateOf(false) }
    MetricsCard(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.metrics_composition), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Edit, stringResource(R.string.common_edit), tint = Gray500, modifier = Modifier.size(18.dp).clickable(onClick = onEdit))
        }
        Text(stringResource(R.string.metrics_composition_desc), style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        ProportionBar(composition?.muscleKg, composition?.fatKg, composition?.otherKg)
        Spacer(Modifier.height(12.dp))
        CompositionRow(Red500, stringResource(R.string.metrics_muscle), composition?.muscleKg, composition?.musclePercent, unit)
        Spacer(Modifier.height(8.dp))
        CompositionRow(Orange300, stringResource(R.string.metrics_fat), composition?.fatKg, composition?.fatPercent, unit)
        Spacer(Modifier.height(8.dp))
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
    // Colores fieles a iOS: músculo=error500, grasa=orange300, otras/vacío=gray400.
    Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(Gray400)) {
        if (m + f + o > 0f) {
            if (m > 0f) Box(Modifier.fillMaxHeight().weight(m).background(Red500))
            if (f > 0f) Box(Modifier.fillMaxHeight().weight(f).background(Orange300))
            if (o > 0f) Box(Modifier.fillMaxHeight().weight(o).background(Gray400))
        }
    }
}

@Composable
private fun CompositionRow(dot: Color, label: String, kg: Double?, pct: Double?, unit: String, onInfo: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray100).border(1.dp, Gray400, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
            Text(label, style = HitbosssType.bodyDefaultRegular, color = Gray500, modifier = Modifier.weight(1f))
            if (onInfo != null) InfoIcon(onInfo)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(kg?.let { formatNum(it) } ?: "-", style = HitbosssType.titleBody, color = Gray800)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(bottom = 2.dp))
            }
            if (pct != null) {
                Text("${formatNum(pct)}%", style = HitbosssType.bodyDefaultRegular, color = Gray500)
            } else {
                NoDataBadge()
            }
        }
    }
}

@Composable
private fun NoDataBadge() {
    Text(
        stringResource(R.string.metrics_no_data),
        style = HitbosssType.bodySmallEmphasis, color = Gray100,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Gray800).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun MeasureBox(label: String, value: Double?, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Gray100)
            .border(1.dp, Gray400, RoundedCornerShape(8.dp))
            .padding(12.dp)
        // No ponemos horizontalAlignment aquí para que el título se quede a la izquierda (default)
    ) {
        // 1. El título se mantiene alineado a la izquierda por defecto
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)

        Spacer(Modifier.height(6.dp))

        // 2. El Box ocupa todo el ancho (fillMaxWidth) y centra su contenido (Center)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value?.let { formatNum(it) } ?: "-", style = HitbosssType.titleBody, color = Gray800)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = HitbosssType.bodyLargeEmphasis, color = Gray500, modifier = Modifier.padding( top = 2.dp))
            }
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
        // Cabecera común
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.Add,
                stringResource(R.string.metrics_goal_add_title),
                tint = Gray500,
                modifier = Modifier.size(20.dp).clickable(onClick = onAdd)
            )
        }

        Spacer(Modifier.height(12.dp))

        // CONTENEDOR COMÚN CON BORDE Y ESTILO
        // Esto garantiza que la caja sea idéntica en ambos casos
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Gray400, RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (goal == null) {
                // --- CASO SIN OBJETIVO ---
                Text(
                    stringResource(R.string.metrics_goal_empty),
                    style = HitbosssType.bodyDefaultEmphasis,
                    color = Gray800,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.metrics_goal_empty_subtitle),
                    style = HitbosssType.bodySmallRegular,
                    color = Gray500,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp)) // Espacio interno ajustado

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.metrics_progress),
                        style = HitbosssType.bodySmallRegular,
                        color = Gray500,
                        modifier = Modifier.weight(1f)
                    )
                    Text("-%", style = HitbosssType.bodySmallEmphasis, color = Gray800)
                }

                Spacer(Modifier.height(6.dp))

                // Barra gris vacía (mismo alto y forma que la base del otro caso)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Gray300)
                )

            } else {
                // --- CASO CON OBJETIVO ---
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    Text(formatNum(goal.currentValue), style = HitbosssType.titleSection, color = Gray800)
                    Text(
                        " /${formatNum(goal.targetValue)} $unit",
                        style = HitbosssType.bodyDefaultRegular,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                val progress = goal.progress.toFloat().coerceIn(0f, 1f)

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.metrics_progress),
                        style = HitbosssType.bodySmallRegular,
                        color = Gray500,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(progress * 100).toInt()}%", style = HitbosssType.bodySmallEmphasis, color = Gray800)
                }

                Spacer(Modifier.height(6.dp))

                val reached = progress >= 1f
                // Barra de progreso con fondo gris y relleno de color
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Gray300)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (reached) Success500 else barColor)
                    )
                }

                val remaining = kotlin.math.abs(goal.targetValue - goal.currentValue)

                if (reached) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Success500, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.metrics_goal_reached),
                            style = HitbosssType.bodySmallEmphasis,
                            color = Success500,
                        )
                    }
                } else if (remaining > 0.01 && goal.currentValue > 0) {
                    Spacer(Modifier.height(8.dp))
                    val remainingRes = if (goal.targetValue > goal.currentValue)
                        R.string.metrics_goal_remaining_up
                    else R.string.metrics_goal_remaining_down

                    Text(
                        stringResource(remainingRes, "${formatNum(remaining)} $unit"),
                        style = HitbosssType.bodySmallRegular,
                        color = Success500,
                    )
                }
            }
        }

        // Historial de objetivos (fuera de la caja con borde, como elemento separado)
        if (showHistory) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded; if (expanded) onExpand() }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.metrics_goal_history),
                    style = HitbosssType.bodyDefaultEmphasis,
                    color = Gray800,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    null,
                    tint = Gray500,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (expanded) {
                if (history.isEmpty()) {
                    Text(
                        stringResource(R.string.metrics_goal_history_empty),
                        style = HitbosssType.bodySmallRegular,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    history.forEach { entry ->
                        GoalHistoryRow(entry, unit, onDelete = { pendingDelete = entry })
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

/** Fila del historial: check de cumplido + "Objetivo: X kg" + fecha. Menú ⋮ → Eliminar (con confirmación). */
@Composable
private fun GoalHistoryRow(entry: GoalHistoryEntry, unit: String, onDelete: () -> Unit) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es")) }
    val date = java.time.Instant.ofEpochSecond(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalCheck(entry.reached)
        Spacer(Modifier.width(10.dp))
        Text(
            "${stringResource(R.string.metrics_goal)}: ${formatNum(entry.targetValue)} $unit",
            style = HitbosssType.bodyDefaultRegular, color = Gray800, modifier = Modifier.weight(1f),
        )
        Text(date, style = HitbosssType.bodySmallRegular, color = Gray500)
        Spacer(Modifier.width(4.dp))
        Box {
            Icon(
                Icons.Filled.MoreVert, stringResource(R.string.common_options), tint = Gray500,
                modifier = Modifier.size(20.dp).clickable { menuOpen = true },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.common_delete), color = Error500) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
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
            onEditPoint = { id, value -> viewModel.onEditPoint(metric, id, value) },
            onDeletePoint = { id -> viewModel.onDeletePoint(metric, id) },
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

@Composable
private fun PhysicalMetric.legendName(): String = when (this) {
    PhysicalMetric.Weight -> stringResource(R.string.metrics_weight)
    PhysicalMetric.Fat -> stringResource(R.string.metrics_fat)
    PhysicalMetric.Muscle -> stringResource(R.string.metrics_muscle)
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
    onEditPoint: (Long, Double) -> Unit,
    onDeletePoint: (Long) -> Unit,
) {
    var monthly by rememberSaveable(metric) { mutableStateOf(false) }
    val timeframe = if (monthly) "month" else "week"
    LaunchedEffect(timeframe) { loadTrend(timeframe); loadHistory(metric, timeframe) }
    // Registros abiertos por el ⓘ de un tile: (esPeríodoActual, puntos).
    var registros by remember(metric) { mutableStateOf<Pair<Boolean, List<TrendPoint>>?>(null) }

    val zone = ZoneId.systemDefault()
    var selectedPoint by remember(metric, timeframe) { mutableStateOf<Int?>(null) }
    // Punto en edición (lista "Registros"): abre el sheet de editar/eliminar.
    var editingPoint by remember(metric, timeframe) { mutableStateOf<BodyHistoryPoint?>(null) }
    var recordsExpanded by rememberSaveable(metric) { mutableStateOf(false) }
    val pointFmt = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale("es")) }
    val weekDayLetters = stringResource(R.string.metrics_week_days).split(",")

    // Fuente única = endpoint trend, alineado a columnas (igual que iOS):
    //   Semana → 7 buckets diarios (L..D), cada uno = valor del día (último registro); punto en columna i/6.
    //   Mes    → buckets semanales (Sem 1..N), cada uno = media de la semana (con desglose diario en el tooltip).
    // Los buckets sin valor no pintan punto.
    val buckets = state.trend[timeframe]?.forMetric(metric.apiKey)?.current ?: emptyList()
    val bucketIdx = buckets.indices.filter { buckets[it].value != null }
    val bucketCount = buckets.size
    val chartPoints: List<Pair<Float, Double>> = bucketIdx.map { i ->
        (if (bucketCount <= 1) 0.5f else i.toFloat() / (bucketCount - 1)) to buckets[i].value!!
    }
    val chartEmpty = bucketIdx.isEmpty()
    val xLabels = if (monthly) List(bucketCount.coerceAtLeast(1)) { "Sem ${it + 1}" } else weekDayLetters

    val today = java.time.LocalDate.now(zone)
    // Caption bajo el segmented (igual que iOS): rango del período — semana Lun-Dom / mes actual.
    val caption = if (monthly) {
        today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))).replaceFirstChar { it.uppercase() }
    } else {
        val mon = today.with(java.time.DayOfWeek.MONDAY)
        val sun = mon.plusDays(6)
        val mFmt = DateTimeFormatter.ofPattern("MMM", Locale("es"))
        if (mon.month == sun.month) stringResource(R.string.metrics_week_of, "${mon.dayOfMonth}-${sun.dayOfMonth} ${sun.format(mFmt)}")
        else stringResource(R.string.metrics_week_of, "${mon.dayOfMonth} ${mon.format(mFmt)} - ${sun.dayOfMonth} ${sun.format(mFmt)}")
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
        Spacer(Modifier.height(10.dp))
        Text(caption, style = HitbosssType.bodySmallRegular, color = Gray500)
        Spacer(Modifier.height(12.dp))

        if (chartEmpty) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.metrics_empty_logs), style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
        } else {
            val series = buildList {
                if (chartPoints.isNotEmpty()) {
                    add(ChartSeries(chartPoints, color = metric.color(), marker = ChartMarker.Circle, fill = true))
                }
                goalValue?.let { add(ChartSeries(listOf(0f to it, 1f to it), color = Success500, dashed = true, marker = ChartMarker.None)) }
            }
            LineChart(
                series = series,
                xLabels = xLabels,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                selectedIndex = selectedPoint,
                onSelect = { selectedPoint = it },
                pointLabel = { i ->
                    val b = buckets[bucketIdx[i]]
                    if (monthly) {
                        // Desglose de la semana: día + valor por cada día con dato, y el Total (media).
                        val rows = b.days.mapIndexedNotNull { di, d ->
                            d.value?.let { "${weekDayLetters.getOrElse(di) { "" }}  ${formatNum(it)} $unit" }
                        }
                        val total = b.value?.let { "Total  ${formatNum(it)} $unit" }
                        (listOf("Sem ${bucketIdx[i] + 1}") + rows + listOfNotNull(total)).joinToString("\n")
                    } else {
                        val d = java.time.Instant.ofEpochSecond(b.date).atZone(zone).toLocalDate()
                            .format(pointFmt).replaceFirstChar { it.uppercase() }
                        "$d · ${formatNum(b.value ?: 0.0)} $unit"
                    }
                },
            )
        }

        // Leyenda: Peso (círculo hueco del color de la métrica) + Objetivo (línea discontinua verde).
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).border(2.dp, metric.color(), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(metric.legendName(), style = HitbosssType.bodySmallRegular, color = Gray500)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { Box(Modifier.size(width = 5.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(Success500)) }
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.metrics_goal), style = HitbosssType.bodySmallRegular, color = Gray500)
            }
        }

        // Tendencia: media del período actual vs anterior (ⓘ abre "Registros") + diferencia con flecha.
        val trend = state.trend[timeframe]?.forMetric(metric.apiKey)
        val currentVal = trend?.currentAvg
        val prevVal = trend?.previousAvg
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeekTile(
                label = stringResource(if (monthly) R.string.metrics_current_month else metrics_current_week),
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
                    (if (delta < 0) "↓ " else "↑ ") + stringResource(
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

        // Registros del período (puntos concretos con id): desplegable + scroll para no crecer sin fin.
        val records = (state.history[metric to timeframe] ?: emptyList()).sortedByDescending { it.measuredAt }
        if (records.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { recordsExpanded = !recordsExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.metrics_records), style = HitbosssType.titleGroup, color = Gray800, modifier = Modifier.weight(1f))
                Text("${records.size}", style = HitbosssType.bodySmallRegular, color = Gray500)
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (recordsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null, tint = Gray500, modifier = Modifier.size(22.dp),
                )
            }
            if (recordsExpanded) {
                Spacer(Modifier.height(4.dp))
                Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    records.forEach { p ->
                        val label = java.time.Instant.ofEpochSecond(p.measuredAt).atZone(zone).toLocalDate()
                            .format(pointFmt).replaceFirstChar { it.uppercase() }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { editingPoint = p }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, style = HitbosssType.bodyDefaultRegular, color = Gray500, modifier = Modifier.weight(1f))
                            Text("${formatNum(p.value)} $unit", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                            Spacer(Modifier.width(12.dp))
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.common_edit), tint = Secondary500, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    editingPoint?.let { p ->
        EditPointSheet(
            initialValue = p.value,
            unit = unit,
            isSaving = state.isSaving,
            onDismiss = { editingPoint = null },
            onSave = { v -> editingPoint = null; onEditPoint(p.id, v) },
            onDelete = { editingPoint = null; onDeletePoint(p.id) },
        )
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

/** Sheet de editar/eliminar un punto de la gráfica de físico (calco del de fuerza pero para body_log). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPointSheet(
    initialValue: Double,
    unit: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    var value by remember { mutableStateOf(formatNum(initialValue)) }
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.metrics_edit_record), style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.metrics_edit_record_desc), style = HitbosssType.bodySmallRegular, color = Gray500)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.metrics_record_value), style = HitbosssType.bodySmallEmphasis, color = Gray800, modifier = Modifier.padding(bottom = 6.dp))
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
            title = stringResource(R.string.metrics_delete_record_title),
            message = stringResource(R.string.metrics_delete_record_msg),
            confirmText = stringResource(R.string.common_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = { confirmDelete = false; onDelete() },
            onCancel = { confirmDelete = false },
            onDismissRequest = { confirmDelete = false },
        )
    }
}

@Composable
private fun WeekTile(label: String, value: Double?, unit: String, modifier: Modifier = Modifier, onInfo: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Gray100)
            .border(1.dp, Gray400, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // 1. Primera fila: Se mantiene igual (Título a la izquierda con peso, icono a la derecha)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.weight(1f))
            if (onInfo != null) InfoIcon(onInfo)
        }

        Spacer(Modifier.height(4.dp))

        // 2. Segunda fila: Envuelta en un Box para centrarla horizontalmente
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
            NumberField(weight, { weight = it }, stringResource(R.string.metrics_weight), if (unitSystem == "imperial") "lbs" else "kg", max = if (unitSystem == "imperial") 880.0 else 400.0)
            Spacer(Modifier.height(12.dp))
            NumberField(height, { height = it }, stringResource(R.string.metrics_height), if (unitSystem == "imperial") "in" else "cm", max = if (unitSystem == "imperial") 100.0 else 250.0)
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
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, unit: String, max: Double? = null) {
    Column {
        Text(label, style = HitbosssType.bodySmallEmphasis, color = Gray800, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray200)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    val norm = new.replace(',', '.')
                    val parsed = norm.toDoubleOrNull()
                    // ponytail: rechaza en el input valores absurdos (500 cm / 500 kg); no puedes ni teclearlos
                    if (new.isEmpty() || (parsed != null && (max == null || parsed <= max))) onChange(norm)
                },
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
            // Carrusel tipo slider: la foto central va grande y las adyacentes asoman a los lados,
            // más pequeñas y atenuadas. Los datos (Peso/Grasa/Músculo) van debajo, de la foto actual.
            val pager = rememberPagerState { photos.size }
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 40.dp),
                pageSpacing = 12.dp,
                beyondViewportPageCount = 1,
            ) { page ->
                val photo = photos[page]
                val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                Box(
                    Modifier.graphicsLayer {
                        val s = androidx.compose.ui.util.lerp(0.86f, 1f, 1f - off)
                        scaleX = s; scaleY = s
                        alpha = androidx.compose.ui.util.lerp(0.5f, 1f, 1f - off)
                    },
                ) {
                    ProgressPhotoImage(photo.photoUrl, dateOf(photo), onClick = { fullscreenPhoto = photo.photoUrl }, heightDp = 380)
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
            }
            if (photos.size > 1) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PagerDots(count = photos.size, current = pager.currentPage)
                }
            }
            photos.getOrNull(pager.currentPage)?.let {
                Spacer(Modifier.height(12.dp))
                PhotoStatsRow(it, unit)
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
                    if (a != null) ProgressPhotoImage(a.photoUrl, dateOf(a), heightDp = 300, overlayStats = a, unit = unit) else EmptySlot()
                }
                Column(Modifier.weight(1f)) {
                    if (b != null) ProgressPhotoImage(b.photoUrl, dateOf(b), heightDp = 300, overlayStats = b, unit = unit) else EmptySlot()
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.metrics_photo_drag_hint), style = HitbosssType.bodySmallRegular, color = Gray500)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEachIndexed { i, p ->
                    val sel = i == slotA || i == slotB
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
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
                        Text(
                            dateOf(p),
                            style = HitbosssType.bodySmallRegular, color = Gray100, maxLines = 1,
                            modifier = Modifier.align(Alignment.BottomStart).padding(3.dp)
                                .clip(RoundedCornerShape(4.dp)).background(Gray800.copy(alpha = 0.6f))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PhotoDiffRow(a, b, unit)
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
private fun ProgressPhotoImage(
    url: String,
    dateLabel: String,
    onClick: (() -> Unit)? = null,
    heightDp: Int = 280,
    overlayStats: com.hitbosss.domain.model.ProgressPhoto? = null,
    unit: String = "",
) {
    Box(
        Modifier.fillMaxWidth().height(heightDp.dp).clip(RoundedCornerShape(10.dp)).background(Gray300)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        coil.compose.AsyncImage(
            model = url, contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            dateLabel,
            style = HitbosssType.bodySmallEmphasis, color = Gray100,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .clip(RoundedCornerShape(6.dp)).background(Gray800.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        // Medidas superpuestas (modo Comparado): panel azul semitransparente abajo, igual que iOS
        // (secondary600 @ 60%), con margen respecto a los bordes de la foto.
        if (overlayStats != null) {
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(8.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Secondary600.copy(alpha = 0.6f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OverlayStatRow(stringResource(R.string.metrics_weight), overlayStats.weightKg?.let { "${formatNum(it)} $unit" })
                OverlayStatRow(stringResource(R.string.metrics_fat), overlayStats.fatPercent?.let { "${formatNum(it)}%" })
                OverlayStatRow(stringResource(R.string.metrics_muscle), overlayStats.muscleKg?.let { "${formatNum(it)} $unit" })
            }
        }
    }
}

@Composable
private fun OverlayStatRow(label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray100.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
        Text(value ?: "--", style = HitbosssType.bodySmallEmphasis, color = Gray100)
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Gray100)
            .border(1.dp, Gray400, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // 1. Etiqueta: Se mantiene a la izquierda (default)
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)

        Spacer(Modifier.height(4.dp)) // Opcional: un poco de espacio si lo necesitas

        // 2. Valor: Centrado horizontalmente gracias al Box
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(value ?: "--", style = HitbosssType.bodyLargeEmphasis, color = Gray800)
        }
    }
}

/** Hueco vacío del modo Comparado (aún sin foto asignada): placeholder con icono de imagen tachada. */
@Composable
private fun EmptySlot() {
    Box(
        Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(10.dp)).background(Gray200),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Gray300),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.HideImage, null, tint = Gray500, modifier = Modifier.size(24.dp)) }
    }
}

/**
 * Fila de diferencias entre dos fotos (B − A): peso/grasa/músculo, coloreada por sentido.
 * Si falta alguna de las dos fotos → "SIN DATOS" en cada caja (paridad con iOS).
 */
@Composable
private fun PhotoDiffRow(a: com.hitbosss.domain.model.ProgressPhoto?, b: com.hitbosss.domain.model.ProgressPhoto?, unit: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DiffStat(stringResource(R.string.metrics_weight), photoDiff(a?.weightKg, b?.weightKg), unit, goodUp = true, Modifier.weight(1f))
        DiffStat(stringResource(R.string.metrics_fat), photoDiff(a?.fatPercent, b?.fatPercent), "%", goodUp = false, Modifier.weight(1f))
        DiffStat(stringResource(R.string.metrics_muscle), photoDiff(a?.muscleKg, b?.muscleKg), unit, goodUp = true, Modifier.weight(1f))
    }
}

private fun photoDiff(a: Double?, b: Double?): Double? = if (a != null && b != null) b - a else null

@Composable
private fun DiffStat(label: String, delta: Double?, unit: String, goodUp: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(8.dp)).background(Gray100).border(1.dp, Gray400, RoundedCornerShape(8.dp)).padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
        Spacer(Modifier.height(6.dp))
        if (delta == null) {
            Text(
                stringResource(R.string.metrics_no_data),
                style = HitbosssType.bodySmallEmphasis, color = Gray100,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Gray800).padding(horizontal = 8.dp, vertical = 3.dp),
            )
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
