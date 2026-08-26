package com.hitbosss.presentation.feature.metrics

import androidx.compose.foundation.Canvas
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
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.presentation.feature.hit.HitVideoData
import com.hitbosss.presentation.feature.hit.HitVideoDialog
import com.hitbosss.presentation.feature.hit.formatPointsText
import com.hitbosss.presentation.feature.ranking.titleRes
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.domain.model.StrengthMark
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import com.hitbosss.domain.usecase.GetStrengthEvolutionUseCase
import com.hitbosss.domain.usecase.GetStrengthGoalUseCase
import com.hitbosss.presentation.designsystem.components.ChartMarker
import com.hitbosss.presentation.designsystem.components.ChartSeries
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.components.LineChart
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
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

/** (performedAt Unix seg, valor en unidad del usuario). */
private typealias Mark = Pair<Long, Double>

data class StrengthDetailUiState(
    val unitSystem: String = "metric",
    val isLoading: Boolean = false,
    val marks: Map<String, List<StrengthMark>> = emptyMap(), // evolución por "range-unit" (entrenos ∪ HITs, del servidor)
    val goal: Double? = null, // objetivo del ejercicio (targetValue); para la línea verde del gráfico
)

@HiltViewModel
class StrengthDetailViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getStrengthEvolution: GetStrengthEvolutionUseCase,
    private val getStrengthGoal: GetStrengthGoalUseCase,
    private val getPersonalInfo: GetPersonalInfoUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val exercise: String = savedStateHandle.get<String>("type") ?: "squat"

    private val _state = MutableStateFlow(StrengthDetailUiState())
    val state: StateFlow<StrengthDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = getCurrentUser()?.uid ?: return@launch
            getPersonalInfo(uid).onSuccess { info -> _state.update { it.copy(unitSystem = info.measurementSystem) } }
            getStrengthGoal(exercise, _state.value.unitSystem).onSuccess { g -> _state.update { it.copy(goal = g?.targetValue) } }
        }
    }

    fun load(range: String) {
        val key = "$range-${_state.value.unitSystem}"
        if (_state.value.marks.containsKey(key)) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getStrengthEvolution(exercise, range, _state.value.unitSystem)
                .onSuccess { list -> _state.update { it.copy(isLoading = false, marks = it.marks + (key to list)) } }
                .onFailure { _state.update { it.copy(isLoading = false) } }
        }
    }
}

/** Detalle de evolución de FUERZA de un ejercicio: rango 1M/3M/6M/1A/Todo + gráfica (entreno + HIT). */
@Composable
fun StrengthDetailScreen(
    onBack: () -> Unit,
    viewModel: StrengthDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var rangeIndex by rememberSaveable { mutableIntStateOf(DetailRange.entries.lastIndex) }
    val range = DetailRange.entries[rangeIndex]
    val unit = if (state.unitSystem == "imperial") "lbs" else "kg"

    LaunchedEffect(range, state.unitSystem) { viewModel.load(range.apiRange) }
    // El servidor ya devuelve entrenos + HITs filtrados por rango; aquí solo separamos por tipo.
    val marks = state.marks["${range.apiRange}-${state.unitSystem}"] ?: emptyList()
    val trainings = marks.filter { !it.isHit }.map { it.performedAt to it.weightKg }
    val hitMarks = marks.filter { it.isHit }.sortedBy { it.performedAt }   // se mantiene el mark para abrir el HIT
    val hits = hitMarks.map { it.performedAt to it.weightKg }

    var showInfo by remember { mutableStateOf(false) }
    var viewerHit by remember { mutableStateOf<HitVideoData?>(null) }
    val exTitle = RankingCategory.entries.firstOrNull { it.apiKey == viewModel.exercise }?.titleRes()?.let { stringResource(it) } ?: ""

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.metrics_evolution), onBack = onBack)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // Chips de rango: seleccionado = contorno azul; resto = gris.
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.metrics_axis_weight, unit), style = HitbosssType.titleBody, color = Gray500, modifier = Modifier.weight(1f))
                InfoIcon(onClick = { showInfo = true })
            }
            Spacer(Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth().weight(1f)) {
                val all = trainings + hits
                when {
                    state.isLoading && all.isEmpty() ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
                    all.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.metrics_empty_logs), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                        }
                    else -> {
                        val zone = ZoneId.systemDefault()
                        val minT = all.minOf { it.first }
                        val maxT = all.maxOf { it.first }
                        val span = (maxT - minT).coerceAtLeast(1).toFloat()
                        fun norm(list: List<Mark>) = list.sortedBy { it.first }.map { (it.first - minT) / span to it.second }
                        val fmt = remember(range) { DateTimeFormatter.ofPattern(if (range == DetailRange.M1) "dd MMM" else "MMM yy") }
                        val labels = listOf(0f, 0.5f, 1f).map { frac ->
                            Instant.ofEpochSecond(minT + (span * frac).toLong()).atZone(zone).toLocalDate().format(fmt)
                        }
                        var hitSeriesIndex = -1
                        LineChart(
                            series = buildList {
                                if (trainings.isNotEmpty()) add(ChartSeries(norm(trainings), color = Primary500, marker = ChartMarker.Circle, fill = true))
                                if (hits.isNotEmpty()) { hitSeriesIndex = size; add(ChartSeries(norm(hits), color = Primary500, marker = ChartMarker.Diamond, showLine = false)) }
                            },
                            xLabels = labels,
                            modifier = Modifier.fillMaxSize(),
                            goalLine = state.goal,
                            // Tocar un HIT (rombo) abre su vídeo, con datos autocontenidos del propio punto.
                            tapSeriesIndex = hitSeriesIndex.takeIf { it >= 0 },
                            onSelect = if (hits.isNotEmpty()) {
                                { i -> i?.let { hitMarks.getOrNull(it) }?.let { m ->
                                    m.videoUrl?.let { url ->
                                        viewerHit = HitVideoData(
                                            videoUrl = url,
                                            seekSeconds = m.videoSecond ?: 0.0,
                                            exerciseTitle = exTitle,
                                            dateText = if (m.performedAt > 0) java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault()).format(java.util.Date(m.performedAt * 1000)) else "",
                                            weightText = "${formatNum(m.weightKg)} ${unit.uppercase()}",
                                            levelWeight = m.levelWeight,
                                            rankText = "",
                                            pointsText = formatPointsText(m.wilksScore),
                                            hitId = m.hitId,
                                        )
                                    }
                                } }
                            } else null,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // Leyenda: Entrenamiento (círculo hueco) + HIT oficial (rombo relleno).
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendMark(stringResource(R.string.metrics_legend_training), diamond = false)
                LegendMark(stringResource(R.string.metrics_legend_hit), diamond = true)
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
    viewerHit?.let { HitVideoDialog(hits = listOf(it)) { viewerHit = null } }
}

/** Replica el marker de la gráfica: círculo hueco (entreno) / rombo relleno (HIT). */
@Composable
private fun LegendMark(label: String, diamond: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(12.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            if (diamond) {
                val r = 5.dp.toPx()
                drawPath(Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close() }, Primary500)
            } else {
                val r = 4.dp.toPx()
                drawCircle(Color.White, r, c)
                drawCircle(Primary500, r, c, style = Stroke(2.dp.toPx()))
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = HitbosssType.bodySmallRegular, color = Gray500)
    }
}
