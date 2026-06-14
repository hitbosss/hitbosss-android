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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.domain.model.EventDetail
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.Sport
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.feature.hit.UploadHitFab
import com.hitbosss.presentation.feature.ranking.CurrentUserRow
import com.hitbosss.presentation.feature.ranking.NoParticipaCard
import com.hitbosss.presentation.feature.ranking.RankingRow
import com.hitbosss.presentation.feature.ranking.RankingSortSheet
import com.hitbosss.presentation.feature.ranking.RankingOrder
import com.hitbosss.presentation.feature.ranking.RequiredExercise
import com.hitbosss.presentation.feature.ranking.countryFlag
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.presentation.feature.ranking.titleRes

@Composable
fun EventRankingScreen(
    onBack: () -> Unit,
    onInfo: (Int) -> Unit,
    onRecordHit: (String, Double) -> Unit = { _, _ -> },
    onSavedHits: () -> Unit = {},
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val e = state.event

    Box(Modifier.fillMaxSize().background(Gray100)) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
            e == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.error ?: stringResource(R.string.community_load_failed), color = Gray500, style = HitbosssType.bodyDefaultRegular) }
            else -> EventRankingContent(e, state.currentUserId, state.currentUserCountry, state.currentUserPicUrl, onBack, { onInfo(e.id) }, onRecordHit, onSavedHits)
        }
    }
}

@Composable
private fun EventRankingContent(
    e: EventDetail,
    currentUserId: String?,
    currentUserCountry: String?,
    currentUserPicUrl: String?,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onRecordHit: (String, Double) -> Unit,
    onSavedHits: () -> Unit,
) {
    val sportEnum = Sport.entries.firstOrNull { it.apiValue == e.sport } ?: Sport.Powerlifting
    val isPl = sportEnum == Sport.Powerlifting
    val ranking = e.ranking[e.sport]
    val officialCat = if (isPl) RankingCategory.PlOfficial else RankingCategory.CfOfficial
    val exerciseCats = RankingCategory.forSport(sportEnum)
        .filter { it != RankingCategory.PlOfficial && it != RankingCategory.CfOfficial }
        .filter { cat -> e.exercises.any { it.equals(cat.apiKey, true) } }
    val categories = listOf(officialCat) + exerciseCats

    var selected by rememberSaveable { mutableStateOf(officialCat) }
    var search by rememberSaveable { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var order by rememberSaveable { mutableStateOf(RankingOrder.Lift) }
    var showMenu by remember { mutableStateOf(false) }

    val sportColor = if (isPl) Secondary500 else Error500
    val isOfficial = selected == officialCat

    val allEntries = ranking?.byCategory?.get(selected).orEmpty()
    val entries = allEntries.filter { search.isBlank() || it.username.contains(search, true) }
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
            // Cabecera con portada
            EventHeader(e, sportEnum.title, onBack, onInfo, showMenu, { showMenu = it })
            // Buscador + orden
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF2F2F2)).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (search.isEmpty()) Text(stringResource(R.string.ranking_search), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                        BasicTextField(search, { search = it }, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
                    }
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Gray500)
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Gray100).border(1.dp, Gray300, CircleShape).clickable { showSort = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.ranking_sort), tint = Gray800, modifier = Modifier.size(22.dp)) }
            }
            // Tabs ejercicio
            LazyRow(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(categories) { cat ->
                    val sel = cat == selected
                    val label = if (cat == officialCat) sportEnum.title else stringResource(cat.titleRes())
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selected = cat }) {
                        Text(label, style = HitbosssType.titleBody, color = if (sel) Gray800 else Gray500, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(vertical = 8.dp))
                        Box(Modifier.height(2.dp).width(if (sel) 40.dp else 0.dp).background(sportColor))
                    }
                }
            }

            LazyColumn(
                Modifier.weight(1f).background(Gray300),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isOfficial && notParticipating) {
                    item { NoParticipaCard(sportEnum.title, required) }
                }
                if (entries.isEmpty() && !(isOfficial && notParticipating)) {
                    item { com.hitbosss.presentation.feature.ranking.RankingEmptyState() }
                }
                items(entries) { entry -> RankingRow(entry, order == RankingOrder.Points) {} }
            }

            currentUserId?.let { CurrentUserRow(currentUserEntry, currentUserCountry, order == RankingOrder.Points, currentUserPicUrl) }
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

@Composable
private fun EventHeader(e: EventDetail, sportTitle: String, onBack: () -> Unit, onInfo: () -> Unit, showMenu: Boolean, setMenu: (Boolean) -> Unit) {
    Box(Modifier.fillMaxWidth().height(200.dp)) {
        AsyncImage(model = e.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(Gray400))
        // Degradado para legibilidad
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))

        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Color.White, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.weight(1f))
            Box {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Gray100), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.common_options), tint = Gray800, modifier = Modifier.size(20.dp).clickable { setMenu(true) })
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { setMenu(false) }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.event_info_title)) }, onClick = { setMenu(false); onInfo() })
                }
            }
        }

        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            // Badge estado
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Secondary500).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(stringResource(eventStatus(e)), style = HitbosssType.bodySmallEmphasis, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            // Fechas
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Gray100).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Gray500, modifier = Modifier.size(16.dp))
                Text("${formatDate(e.startTime)} — ${formatDate(e.endTime)}", style = HitbosssType.bodySmallRegular, color = Gray800)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = HitbosssType.titleSection, color = Color.White)
                    Text(sportTitle.uppercase(), style = HitbosssType.bodySmallEmphasis, color = Color.White)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Gray100).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Gray800, modifier = Modifier.size(16.dp))
                    Text("${e.stats.memberCount}", style = HitbosssType.bodyDefaultRegular, color = Gray800)
                }
            }
        }
    }
}

@androidx.annotation.StringRes
private fun eventStatus(e: EventDetail): Int {
    val now = System.currentTimeMillis()
    return when {
        e.endTime * 1000 < now -> R.string.event_state_finished
        e.startTime * 1000 > now -> R.string.event_state_upcoming
        else -> R.string.event_state_active
    }
}

private fun formatDate(s: Long): String =
    if (s <= 0) "—" else java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(s * 1000))
