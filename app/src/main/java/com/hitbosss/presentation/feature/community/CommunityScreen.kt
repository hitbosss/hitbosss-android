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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.domain.model.EventSummary
import com.hitbosss.domain.model.GroupSummary
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Primary600
import com.hitbosss.presentation.designsystem.theme.Purple300
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.hitbosss.R
import androidx.compose.ui.res.stringResource

private enum class SubTab { Mine, Community }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onOpenGroup: (Int) -> Unit = {},
    onOpenEvent: (Int) -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Gray100)) {
        // Tabs superiores subrayadas (Grupos / Eventos)
        TopTabs(state.selected, onSelect = viewModel::select)

        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f),
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Primary500)
                }

                state.selected == CommunityTab.Groups -> GroupsTab(
                    myGroups = state.groups?.myGroups.orEmpty(),
                    communityGroups = state.groups?.communityGroups.orEmpty(),
                    onOpenGroup = onOpenGroup,
                    onCreate = onCreateGroup,
                )

                else -> EventsTab(
                    myEvents = state.events?.myEvents.orEmpty(),
                    communityEvents = state.events?.communityEvents.orEmpty(),
                    onOpenEvent = onOpenEvent,
                    onCreate = onCreateEvent,
                )
            }
        }
    }
}

// MARK: - Tabs superiores

@Composable
private fun TopTabs(selected: CommunityTab, onSelect: (CommunityTab) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
        listOf(CommunityTab.Groups to R.string.community_tab_groups, CommunityTab.Events to R.string.community_tab_events).forEach { (tab, label) ->
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(label), style = HitbosssType.bodyLargeRegular, color = Gray800)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(2.dp)
                        .background(if (selected == tab) Secondary500 else Color.Transparent),
                )
            }
        }
    }
}

// MARK: - Pestaña Grupos

@Composable
private fun GroupsTab(
    myGroups: List<GroupSummary>,
    communityGroups: List<GroupSummary>,
    onOpenGroup: (Int) -> Unit,
    onCreate: () -> Unit,
) {
    var sub by rememberSaveable { mutableStateOf(SubTab.Mine) }
    var search by rememberSaveable { mutableStateOf("") }

    val source = if (sub == SubTab.Mine) myGroups else communityGroups
    val displayed = source.filter { it.name.contains(search.trim(), ignoreCase = true) }

    val title = stringResource(if (sub == SubTab.Mine) R.string.community_my_groups else R.string.community_title)
    val desc = stringResource(
        if (sub == SubTab.Mine) R.string.community_groups_mine_desc else R.string.community_groups_discover_desc,
    )
    val emptyMsg = stringResource(
        if (sub == SubTab.Mine) R.string.community_groups_empty_mine else R.string.community_groups_empty_discover,
    )

    Column(Modifier.fillMaxSize()) {
        SearchAndCreate(search, { search = it }, stringResource(R.string.community_search_group), onCreate)
        SubTabs(sub, listOf(SubTab.Mine to R.string.community_my_groups, SubTab.Community to R.string.community_title)) { sub = it }
        SectionList(title, desc, displayed.size, emptyMsg, R.drawable.im_empty_group) {
            items(displayed) { g -> GroupCard(g) { onOpenGroup(g.id) } }
        }
    }
}

// MARK: - Pestaña Eventos

private enum class EventTab(@androidx.annotation.StringRes val label: Int) {
    Mine(R.string.community_my_events), Community(R.string.community_title), Past(R.string.community_past_events)
}

private fun EventSummary.isPast(): Boolean = endTime * 1000 < System.currentTimeMillis()

@Composable
private fun EventsTab(
    myEvents: List<EventSummary>,
    communityEvents: List<EventSummary>,
    onOpenEvent: (Int) -> Unit,
    onCreate: () -> Unit,
) {
    var sub by rememberSaveable { mutableStateOf(EventTab.Mine) }
    var search by rememberSaveable { mutableStateOf("") }

    val source = when (sub) {
        EventTab.Mine -> myEvents.filter { !it.isPast() }
        EventTab.Community -> communityEvents.filter { !it.isPast() }
        EventTab.Past -> myEvents.filter { it.isPast() }
    }
    val displayed = source.filter { it.name.contains(search.trim(), ignoreCase = true) }

    val title = stringResource(sub.label)
    val desc = stringResource(
        when (sub) {
            EventTab.Mine -> R.string.community_events_mine_desc
            EventTab.Community -> R.string.community_events_discover_desc
            EventTab.Past -> R.string.community_events_past_desc
        },
    )
    val emptyMsg = stringResource(
        when (sub) {
            EventTab.Mine -> R.string.community_events_empty_mine
            EventTab.Community -> R.string.community_events_empty_discover
            EventTab.Past -> R.string.community_events_empty_past
        },
    )

    Column(Modifier.fillMaxSize()) {
        SearchAndCreate(search, { search = it }, stringResource(R.string.community_search_event), onCreate)
        // Sub-toggle de 3 (cápsulas)
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EventTab.entries.forEach { tab ->
                val sel = tab == sub
                Text(
                    stringResource(tab.label), style = HitbosssType.bodyDefaultRegular, color = Gray800,
                    modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(Gray100)
                        .border(1.dp, if (sel) Secondary500 else Gray300, RoundedCornerShape(32.dp))
                        .clickable { sub = tab }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        SectionList(title, desc, displayed.size, emptyMsg, R.drawable.im_empty_event) {
            items(displayed) { e -> EventCardBig(e, sub == EventTab.Past) { onOpenEvent(e.id) } }
        }
    }
}

/** Card de evento (portada + badge deporte + estado + nombre + descripción + footer). */
@Composable
private fun EventCardBig(e: EventSummary, isPast: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray100).clickable { onClick() },
    ) {
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            AsyncImage(
                model = e.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(Gray400),
            )
            // Badge deporte (arriba-izq)
            Text(
                e.sport.uppercase(), style = HitbosssType.bodySmallEmphasis, color = Gray100,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(8.dp))
                    .background(Gray800.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 4.dp),
            )
            // Estado / cuenta atrás (arriba-der)
            Text(
                eventStatusText(e, isPast, androidx.compose.ui.platform.LocalContext.current), style = HitbosssType.bodySmallEmphasis, color = Gray100,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (isPast) Gray600 else Primary600).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Text(e.name, style = HitbosssType.titleSubsection, color = Gray800, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        e.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = HitbosssType.bodySmallRegular, color = Gray500, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Box(Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp).height(1.dp).background(Gray200))
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = Secondary800, modifier = Modifier.size(14.dp))
                Text("${e.stats.memberCount}", style = HitbosssType.bodyDefaultRegular, color = Gray800)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Purple300, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.community_rank_none), style = HitbosssType.bodyDefaultRegular, color = Secondary800)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(34.dp).clip(CircleShape).background(Gray800), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.common_open), tint = Gray100, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** "Faltan N días" / "Último día" / fecha si pasado (igual que EventStatusView de iOS). */
private fun eventStatusText(e: EventSummary, isPast: Boolean, context: android.content.Context): String {
    if (isPast) return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(e.endTime * 1000))
    val days = ((e.endTime * 1000 - System.currentTimeMillis()) / 86_400_000L).toInt()
    return when {
        days > 1 -> context.getString(R.string.event_days_left, days)
        days == 1 -> context.getString(R.string.event_one_day_left)
        else -> context.getString(R.string.event_last_day)
    }
}

// MARK: - Buscador + botón crear

@Composable
private fun SearchAndCreate(value: String, onValueChange: (String) -> Unit, placeholder: String, onCreate: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Gray200)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Gray500, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = HitbosssType.bodyDefaultRegular, color = Gray500)
                BasicTextField(
                    value = value, onValueChange = onValueChange, singleLine = true,
                    textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800),
                )
            }
        }
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Gray100)
                .border(1.dp, Gray300, CircleShape).clickable { onCreate() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.common_create), tint = Gray600, modifier = Modifier.size(22.dp))
        }
    }
}

// MARK: - Sub-toggle (cápsulas)

@Composable
private fun SubTabs(selected: SubTab, options: List<Pair<SubTab, Int>>, onSelect: (SubTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (tab, label) ->
            val sel = tab == selected
            Text(
                stringResource(label), style = HitbosssType.bodyDefaultRegular, color = Gray800,
                modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(Gray100)
                    .border(1.dp, if (sel) Secondary500 else Gray300, RoundedCornerShape(32.dp))
                    .clickable { onSelect(tab) }.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

// MARK: - Cabecera de sección + lista

@Composable
private fun SectionList(
    title: String,
    description: String,
    count: Int,
    emptyMessage: String,
    emptyRes: Int,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Gray200)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = HitbosssType.titleBody, color = Gray800)
            Text(
                "$count", style = HitbosssType.bodySmallEmphasis, color = Gray100,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Secondary500)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Text(description, style = HitbosssType.bodySmallRegular, color = Gray500, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        if (count == 0) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(painterResource(emptyRes), contentDescription = null, modifier = Modifier.size(187.dp))
                Spacer(Modifier.height(16.dp))
                Text(emptyMessage, style = HitbosssType.titleBody, color = Gray500, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 24.dp),
                content = content,
            )
        }
    }
}

// MARK: - Cards

@Composable
private fun GroupCard(g: GroupSummary, onClick: () -> Unit) {
    CommunityCard(g.coverImageUrl, g.name, g.description, onClick) {
        CardStats(g.stats.memberCount, g.stats.exerciseCount)
    }
}

@Composable
private fun EventCard(e: EventSummary, onClick: () -> Unit) {
    CommunityCard(e.coverImageUrl, e.name, e.description, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(e.sport.replaceFirstChar { it.uppercase() }, style = HitbosssType.bodySmallRegular, color = Secondary500)
            CardStats(e.stats.memberCount, e.stats.exerciseCount)
        }
    }
}

@Composable
private fun CommunityCard(
    coverUrl: String?,
    name: String,
    description: String?,
    onClick: () -> Unit,
    footer: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)).background(Gray100)
            .border(1.dp, Gray300, RoundedCornerShape(8.dp))
            .clickable { onClick() }.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
            placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Gray200),
        )
        Column(Modifier.weight(1f).height(80.dp)) {
            Text(name, style = HitbosssType.bodyLargeRegular, color = Gray800, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(description.orEmpty(), style = HitbosssType.bodySmallRegular, color = Gray500, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            footer()
        }
    }
}

@Composable
private fun CardStats(members: Int, exercises: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.Group, contentDescription = null, tint = Secondary500, modifier = Modifier.size(15.dp))
            Text("$members", style = HitbosssType.bodySmallRegular, color = Gray800)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Purple300, modifier = Modifier.size(13.dp))
            Text("Ejercicios: $exercises", style = HitbosssType.bodySmallRegular, color = Secondary800)
        }
    }
}
