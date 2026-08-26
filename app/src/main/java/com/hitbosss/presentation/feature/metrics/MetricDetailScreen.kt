package com.hitbosss.presentation.feature.metrics

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.domain.model.BodyHistoryPoint
import com.hitbosss.domain.usecase.GetBodyHistoryUseCase
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetMetricGoalUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import com.hitbosss.presentation.designsystem.components.ChartMarker
import com.hitbosss.presentation.designsystem.components.ChartSeries
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.components.LineChart
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Success500
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MetricDetailUiState(
    val isLoading: Boolean = false,
    val unitSystem: String = "metric",
    val series: Map<Pair<PhysicalMetric, String>, List<BodyHistoryPoint>> = emptyMap(),
    val goals: Map<PhysicalMetric, Double?> = emptyMap(), // objetivo (targetValue) por métrica; para la línea del gráfico
)

@HiltViewModel
class MetricDetailViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getHistory: GetBodyHistoryUseCase,
    private val getGoal: GetMetricGoalUseCase,
    private val getPersonalInfo: GetPersonalInfoUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val initialMetric: PhysicalMetric = when (savedStateHandle.get<String>("type")) {
        "fat" -> PhysicalMetric.Fat
        "muscle" -> PhysicalMetric.Muscle
        else -> PhysicalMetric.Weight
    }

    private val _state = MutableStateFlow(MetricDetailUiState())
    val state: StateFlow<MetricDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUser()?.uid ?: return@launch
            getPersonalInfo(getCurrentUser()!!.uid).onSuccess { info ->
                _state.update { it.copy(unitSystem = info.measurementSystem) }
            }
        }
    }

    fun load(metric: PhysicalMetric, range: String) {
        // El objetivo (línea verde) se carga una vez por métrica.
        if (!_state.value.goals.containsKey(metric)) {
            viewModelScope.launch {
                getGoal(metric.apiKey, _state.value.unitSystem).onSuccess { g ->
                    _state.update { it.copy(goals = it.goals + (metric to g?.targetValue)) }
                }
            }
        }
        val key = metric to range
        if (_state.value.series.containsKey(key)) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getHistory(metric.apiKey, range, _state.value.unitSystem).onSuccess { pts ->
                _state.update { it.copy(isLoading = false, series = it.series + (key to pts)) }
            }.onFailure { _state.update { it.copy(isLoading = false) } }
        }
    }
}

/** Rangos del detalle (chips 1M/3M/6M/1A/Total → range del servidor). Compartido Físico/Fuerza. */
enum class DetailRange(val apiRange: String) {
    M1("month"), M3("3m"), M6("6m"), Y1("1y"), Total("all")
}

@Composable
fun DetailRange.label(): String = when (this) {
    DetailRange.M1 -> stringResource(R.string.metrics_range_1m)
    DetailRange.M3 -> stringResource(R.string.metrics_range_3m)
    DetailRange.M6 -> stringResource(R.string.metrics_range_6m)
    DetailRange.Y1 -> stringResource(R.string.metrics_range_1y)
    DetailRange.Total -> stringResource(R.string.metrics_range_total)
}

/** Detalle de evolución física: segmented Peso/Grasa/Músculo + rango + gráfica grande. */
@Composable
fun MetricDetailScreen(
    onBack: () -> Unit,
    viewModel: MetricDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var metric by rememberSaveable { mutableStateOf(viewModel.initialMetric) }
    var rangeIndex by rememberSaveable { mutableIntStateOf(DetailRange.entries.lastIndex) }
    val range = DetailRange.entries[rangeIndex]

    LaunchedEffect(metric, range) { viewModel.load(metric, range.apiRange) }
    val points = state.series[metric to range.apiRange] ?: emptyList()
    val unit = if (state.unitSystem == "imperial") "lbs" else "kg"

    var showInfo by remember { mutableStateOf(false) }
    val metricName = when (metric) {
        PhysicalMetric.Weight -> stringResource(R.string.metrics_weight)
        PhysicalMetric.Fat -> stringResource(R.string.metrics_fat)
        PhysicalMetric.Muscle -> stringResource(R.string.metrics_muscle)
    }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.metrics_evolution), onBack = onBack)

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            MetricsSegmented(
                options = listOf(
                    stringResource(R.string.metrics_weight),
                    stringResource(R.string.metrics_fat),
                    stringResource(R.string.metrics_muscle),
                ),
                selectedIndex = metric.ordinal,
                onSelect = { metric = PhysicalMetric.entries[it] },
            )
            Spacer(Modifier.height(16.dp))

            // Chips de rango: seleccionado = contorno azul + texto azul; resto = contorno gris.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRange.entries.forEachIndexed { i, r ->
                    val sel = i == rangeIndex
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .border(1.dp, if (sel) Secondary500 else Gray400, RoundedCornerShape(20.dp))
                            .clickable { rangeIndex = i }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text(r.label(), style = HitbosssType.bodySmallEmphasis, color = if (sel) Secondary500 else Gray500)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Título del eje Y (unidad) + ⓘ. Grasa ahora es masa (kg) → todas usan la unidad de peso.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.metrics_axis_weight, unit), style = HitbosssType.titleBody, color = Gray500, modifier = Modifier.weight(1f))
                InfoIcon(onClick = { showInfo = true })
            }
            Spacer(Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.isLoading && points.isEmpty() ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
                    points.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.metrics_empty_logs), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                        }
                    else -> {
                        val zone = ZoneId.systemDefault()
                        val minT = points.minOf { it.measuredAt }
                        val maxT = points.maxOf { it.measuredAt }
                        val span = (maxT - minT).coerceAtLeast(1).toFloat()
                        val chartPoints = points.map { (it.measuredAt - minT) / span to it.value }
                        val fmt = remember(range) { DateTimeFormatter.ofPattern(if (range == DetailRange.M1) "dd MMM" else "MMM yy") }
                        val labels = listOf(0f, 0.5f, 1f).map { frac ->
                            Instant.ofEpochSecond((minT + (span * frac).toLong())).atZone(zone).toLocalDate().format(fmt)
                        }
                        LineChart(
                            series = listOf(
                                ChartSeries(chartPoints, color = metricColor(metric.apiKey), marker = ChartMarker.Circle, fill = true),
                            ),
                            xLabels = labels,
                            modifier = Modifier.fillMaxSize(),
                            goalLine = state.goals[metric],
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Leyenda: métrica (círculo hueco de su color) + Objetivo (línea discontinua verde).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).border(3.dp, metricColor(metric.apiKey), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(metricName, style = HitbosssType.bodySmallEmphasis, color = Gray500)
                Spacer(Modifier.width(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { Box(Modifier.size(width = 5.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(Success500)) }
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.metrics_goal), style = HitbosssType.bodySmallEmphasis, color = Gray500)
            }
        }
    }

    if (showInfo) {
        HitPopup(
            title = stringResource(R.string.metrics_evolution),
            message = stringResource(R.string.metrics_detail_info),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = { showInfo = false },
            onDismissRequest = { showInfo = false },
        )
    }
}
