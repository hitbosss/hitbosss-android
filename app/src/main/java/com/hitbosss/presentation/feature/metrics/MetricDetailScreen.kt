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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.hitbosss.domain.model.BodyLog
import com.hitbosss.domain.usecase.GetBodyLogsUseCase
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.presentation.designsystem.components.ChartMarker
import com.hitbosss.presentation.designsystem.components.ChartSeries
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.components.LineChart
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Success400
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MetricDetailUiState(
    val isLoading: Boolean = true,
    val logs: List<BodyLog> = emptyList(),
)

@HiltViewModel
class MetricDetailViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getBodyLogs: GetBodyLogsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val initialMetric: String = savedStateHandle.get<String>("type") ?: "weight"

    private val _state = MutableStateFlow(MetricDetailUiState())
    val state: StateFlow<MetricDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = getCurrentUser()?.uid ?: return@launch
            getBodyLogs(uid).onSuccess { logs -> _state.update { it.copy(isLoading = false, logs = logs) } }
                .onFailure { _state.update { it.copy(isLoading = false) } }
        }
    }
}

/** Rangos del detalle (chips 1M/3M/6M/1A/Total). */
private enum class DetailRange(val months: Long?) { M1(1), M3(3), M6(6), Y1(12), Total(null) }

@Composable
private fun DetailRange.label(): String = when (this) {
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
    var metric by rememberSaveable {
        mutableStateOf(
            when (viewModel.initialMetric) {
                "fat" -> PhysicalMetric.Fat
                "muscle" -> PhysicalMetric.Muscle
                else -> PhysicalMetric.Weight
            },
        )
    }
    var rangeIndex by rememberSaveable { mutableIntStateOf(DetailRange.entries.lastIndex) }
    val range = DetailRange.entries[rangeIndex]

    Column(Modifier.fillMaxSize().background(Gray200)) {
        HitTopBar(title = stringResource(R.string.metrics_evolution), onBack = onBack)

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
            return@Column
        }

        Column(Modifier.padding(16.dp)) {
            MetricsSegmented(
                options = listOf(
                    stringResource(R.string.metrics_weight),
                    stringResource(R.string.metrics_fat),
                    stringResource(R.string.metrics_muscle),
                ),
                selectedIndex = metric.ordinal,
                onSelect = { metric = PhysicalMetric.entries[it] },
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRange.entries.forEachIndexed { i, r ->
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (i == rangeIndex) Gray800 else Gray100)
                            .clickable { rangeIndex = i }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            r.label(),
                            style = HitbosssType.bodySmallEmphasis,
                            color = if (i == rangeIndex) Gray100 else Gray500,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            val zone = ZoneId.systemDefault()
            val now = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toEpochSecond()
            val fromSec = range.months?.let { LocalDate.now(zone).minusMonths(it).atStartOfDay(zone).toEpochSecond() }

            val visible = state.logs.filter { fromSec == null || it.loggedAt >= fromSec }
            val values = visible.mapNotNull { log ->
                val v = when (metric) {
                    PhysicalMetric.Weight -> log.weight.value
                    PhysicalMetric.Fat -> log.bodyFatPct
                    PhysicalMetric.Muscle -> log.muscleMass?.value
                } ?: return@mapNotNull null
                log.loggedAt to v
            }

            val unitLabel = when (metric) {
                PhysicalMetric.Fat -> stringResource(R.string.metrics_axis_pct)
                else -> stringResource(
                    R.string.metrics_axis_weight,
                    visible.firstOrNull()?.weight?.unit ?: "kg",
                )
            }
            Text(unitLabel, style = HitbosssType.bodySmallRegular, color = Gray500)
            Spacer(Modifier.height(8.dp))

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100).padding(16.dp)) {
                if (values.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.metrics_empty_logs), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                    }
                } else {
                    val minT = fromSec ?: values.minOf { it.first }
                    val span = (now - minT).coerceAtLeast(1).toFloat()
                    val points = values.map { (t, v) -> (t - minT) / span to v }
                    val fmt = remember(range) { DateTimeFormatter.ofPattern(if (range == DetailRange.M1) "dd MMM" else "MMM yy") }
                    val labels = listOf(0f, 0.5f, 1f).map { frac ->
                        java.time.Instant.ofEpochSecond((minT + (span * frac).toLong()))
                            .atZone(zone).toLocalDate().format(fmt)
                    }
                    LineChart(
                        series = listOf(
                            ChartSeries(
                                points,
                                color = if (metric == PhysicalMetric.Weight) Secondary500 else Success400,
                                marker = if (points.size <= 30) ChartMarker.Circle else ChartMarker.None,
                                fill = true,
                            ),
                        ),
                        xLabels = labels,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    )
                }
            }
        }
    }
}
