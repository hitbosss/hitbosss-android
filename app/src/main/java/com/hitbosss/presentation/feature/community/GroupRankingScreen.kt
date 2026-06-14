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
import com.hitbosss.presentation.feature.hit.UploadHitFab
import com.hitbosss.presentation.feature.ranking.CurrentUserRow
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
    onInfo: (Int) -> Unit,
    onRecordHit: (String, Double) -> Unit = { _, _ -> },
    onSavedHits: () -> Unit = {},
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val g = state.group
    Box(Modifier.fillMaxSize().background(Gray100)) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
            g == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.error ?: stringResource(R.string.community_load_failed), color = Gray500, style = HitbosssType.bodyDefaultRegular) }
            else -> GroupRankingContent(g, state.currentUserId, state.currentUserCountry, state.currentUserPicUrl, onBack, { onInfo(g.id) }, onRecordHit, onSavedHits)
        }
    }
}

@Composable
private fun GroupRankingContent(
    g: GroupDetail,
    currentUserId: String?,
    currentUserCountry: String?,
    currentUserPicUrl: String?,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onRecordHit: (String, Double) -> Unit,
    onSavedHits: () -> Unit,
) {
    val sportKey = g.officialSports.firstOrNull()
        ?: g.exercises.firstNotNullOfOrNull { ex -> Exercise.entries.firstOrNull { it.apiValue.equals(ex, true) }?.sport?.apiValue }
        ?: "powerlifting"
    val sportEnum = Sport.entries.firstOrNull { it.apiValue == sportKey } ?: Sport.Powerlifting
    val isPl = sportEnum == Sport.Powerlifting
    val ranking = g.ranking[sportKey]
    val officialCat = if (isPl) RankingCategory.PlOfficial else RankingCategory.CfOfficial
    val exerciseCats = RankingCategory.forSport(sportEnum)
        .filter { it != RankingCategory.PlOfficial && it != RankingCategory.CfOfficial }
        .filter { cat -> g.exercises.any { it.equals(cat.apiKey, true) } }
    val categories = listOf(officialCat) + exerciseCats

    var selected by rememberSaveable { mutableStateOf(officialCat) }
    var search by rememberSaveable { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var order by rememberSaveable { mutableStateOf(RankingOrder.Lift) }
    var selectedLevels by rememberSaveable(stateSaver = levelSetSaver) { mutableStateOf(emptySet<String>()) }
    var showMenu by remember { mutableStateOf(false) }

    val sportColor = if (isPl) Secondary500 else Error500
    val isOfficial = selected == officialCat
    val usePoints = order == RankingOrder.Points

    val entries = ranking?.byCategory?.get(selected).orEmpty().filter { e ->
        (search.isBlank() || e.username.contains(search, true)) &&
            (selectedLevels.isEmpty() || (if (usePoints) e.levelWilks else e.levelWeight).orEmpty() in selectedLevels)
    }
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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Cabecera grupo (avatar + nombre + menú)
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Gray800, modifier = Modifier.size(24.dp).clickable { onBack() })
                Spacer(Modifier.width(12.dp))
                AsyncImage(model = g.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(), modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Gray300))
                Spacer(Modifier.width(12.dp))
                Text(g.name, style = HitbosssType.titleSubsection, color = Gray800, modifier = Modifier.weight(1f))
                Box {
                    Box(Modifier.size(32.dp).clip(CircleShape).border(1.dp, Gray300, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.common_options), tint = Gray800, modifier = Modifier.size(20.dp).clickable { showMenu = true })
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.group_info_title)) }, onClick = { showMenu = false; onInfo() })
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))

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
            LazyRow(Modifier.fillMaxWidth().padding(top = 8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                items(categories) { cat ->
                    val sel = cat == selected
                    val label = if (cat == officialCat) sportEnum.title else stringResource(cat.titleRes())
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
                items(entries) { entry -> RankingRow(entry, usePoints) {} }
            }

            currentUserId?.let { CurrentUserRow(currentUserEntry, currentUserCountry, usePoints, currentUserPicUrl) }
        }

        UploadHitFab(
            sport = selected.sport,
            isOfficial = isOfficial,
            exerciseKey = selected.uploadExerciseKey,
            onRecordHit = onRecordHit,
            onSavedHits = onSavedHits,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 88.dp),
        )
    }

    if (showSort) RankingSortSheet(current = order, onSelect = { order = it }, onDismiss = { showSort = false })
}

/** Saver para conservar los niveles seleccionados (Set<String>) en rememberSaveable. */
private val levelSetSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() },
)
