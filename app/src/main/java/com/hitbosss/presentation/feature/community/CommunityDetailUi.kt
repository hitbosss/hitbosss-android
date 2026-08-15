package com.hitbosss.presentation.feature.community

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray600
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hitbosss.domain.model.Member
import androidx.compose.ui.graphics.Color
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary100
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Primary700
import com.hitbosss.presentation.designsystem.theme.Secondary100
import androidx.compose.ui.res.stringResource
import com.hitbosss.R

@Composable
fun DetailCover(url: String?) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(170.dp).background(Gray400),
    )
}

@Composable
fun ExerciseChips(exercises: List<String>) {
    if (exercises.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        exercises.take(6).forEach { ex ->
            Text(
                ex.replaceFirstChar { it.uppercase() },
                style = HitbosssType.bodySmallEmphasis,
                color = Primary700,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary100)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun MembersSection(members: List<Member>) {
    Text(stringResource(R.string.community_members_count, members.size), style = HitbosssType.titleBody, color = Gray800)
    members.forEach { member ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = member.profilePic,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Gray200),
            )
            Text(member.username, style = HitbosssType.bodyLargeRegular, color = Gray800, modifier = Modifier.weight(1f))
            if (member.isAdmin) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Primary100).padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(stringResource(R.string.common_admin), style = HitbosssType.bodySmallEmphasis, color = Primary700)
                }
            }
        }
    }
    if (members.isEmpty()) {
        Text(stringResource(R.string.community_no_members), style = HitbosssType.bodySmallRegular, color = Gray500)
    }
}

/**
 * Administradores a mostrar en la pestaña Información: miembros admin + el creador (aunque no figure
 * como admin), con el creador primero y el resto alfabético (fix iOS #645).
 */
fun communityAdmins(members: List<Member>, creatorId: String?): List<Member> =
    members.filter { it.isAdmin || it.userId == creatorId }
        .sortedWith(compareByDescending<Member> { it.userId == creatorId }.thenBy { it.username.lowercase() })

/** Chip seleccionable (ejercicio/disciplina): fondo blanco, borde del color del deporte si está activo. */
@Composable
fun SelectChip(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Text(
        text, style = HitbosssType.bodyDefaultRegular, color = Gray800,
        modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(Gray100)
            .border(1.dp, if (selected) color else Gray300, RoundedCornerShape(32.dp))
            .clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Pestañas del detalle de comunidad (grupo/evento), 1:1 con CommunityTabs de iOS. */
enum class CommunityTabs(@StringRes val label: Int) {
    Ranking(R.string.comm_tab_ranking),
    Information(R.string.comm_tab_information),
}

/** Transición de barrido horizontal entre Ranking/Información según la dirección del cambio. */
fun androidx.compose.animation.AnimatedContentTransitionScope<CommunityTabs>.communityTabTransition(): androidx.compose.animation.ContentTransform {
    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
    val sp = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(250)
    return (androidx.compose.animation.slideInHorizontally(sp) { w -> dir * w } + androidx.compose.animation.fadeIn()) togetherWith
        (androidx.compose.animation.slideOutHorizontally(sp) { w -> -dir * w } + androidx.compose.animation.fadeOut())
}

/** Segmentado de pestañas Ranking / Información (cabecera del detalle). */
@Composable
fun CommunityTabsBar(selected: CommunityTabs, onSelect: (CommunityTabs) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(6.dp)).background(Gray200).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CommunityTabs.entries.forEach { tab ->
            val sel = tab == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Gray100 else Color.Transparent).clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(tab.label),
                    style = if (sel) HitbosssType.bodyDefaultEmphasis else HitbosssType.bodyDefaultRegular,
                    color = if (sel) Gray800 else Gray500,
                )
            }
        }
    }
}

enum class CommunityInfoContextType { Group, Event }

/** Fila de detalle (icono en caja gris + título + valor), 1:1 con CommunityDetailRow de iOS. */
data class CommunityDetailRowData(val icon: androidx.compose.ui.graphics.vector.ImageVector, @StringRes val titleRes: Int, val value: String)

/**
 * Pestaña "Información" del detalle de grupo/evento (1:1 con CommunityInformationTabView de iOS):
 * tarjeta por sección con icono rosa. Orden grupo: descripción, lema, miembros, dinámica, detalles,
 * compartir. Orden evento: descripción, detalles, miembros, dinámica, compartir.
 */
@Composable
fun CommunityInformationTab(
    contextType: CommunityInfoContextType,
    description: String?,
    motto: String?,
    detailsTitle: String,
    detailRows: List<CommunityDetailRowData>,
    memberCount: Int,
    admins: List<Member>,
    creatorId: String?,
    extraInfoIcon: androidx.compose.ui.graphics.vector.ImageVector,
    extraInfoTitle: String,
    extraInfoCount: Int?,
    isExpandable: Boolean,
    shareTitle: String,
    shareSubtitle: String,
    onSeeMembers: () -> Unit,
    onShare: () -> Unit,
    extraContent: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val descriptionSection: @Composable () -> Unit = {
            if (!description.isNullOrBlank()) InfoCard {
                InfoSectionHeader(Icons.Filled.Description, stringResource(R.string.common_description))
                Text(description, style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
        }
        val mottoSection: @Composable () -> Unit = {
            if (!motto.isNullOrBlank()) InfoCard {
                InfoSectionHeader(Icons.Filled.LocalFireDepartment, stringResource(R.string.community_motto_title))
                Text(motto, style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
        }
        val detailsSection: @Composable () -> Unit = {
            InfoCard {
                InfoSectionHeader(Icons.Filled.Info, detailsTitle)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detailRows.forEach { row -> CommunityDetailRow(row) }
                }
            }
        }
        val memberSection: @Composable () -> Unit = {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Primary500, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.common_members), style = HitbosssType.bodyLargeEmphasis, color = Gray800, modifier = Modifier.weight(1f))
                    Text("$memberCount", style = HitbosssType.bodyDefaultEmphasis, color = Gray500)
                }
                if (admins.isNotEmpty()) {
                    Text(stringResource(R.string.community_administrators), style = HitbosssType.bodySmallEmphasis, color = Gray500)
                    admins.take(3).forEach { member -> AdminRow(member, isCreator = member.userId == creatorId) }
                }
                Box(
                    Modifier.fillMaxWidth().padding(top = 24.dp).clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Gray500, RoundedCornerShape(8.dp)).clickable { onSeeMembers() }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.community_see_all_members), style = HitbosssType.bodyDefaultRegular, color = Gray600)
                }
            }
        }
        val dynamicSection: @Composable () -> Unit = {
            var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            InfoCard {
                Row(
                    Modifier.fillMaxWidth().let { if (isExpandable) it.clickable { expanded = !expanded } else it },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(extraInfoIcon, contentDescription = null, tint = Primary500, modifier = Modifier.size(16.dp))
                    Text(extraInfoTitle, style = HitbosssType.bodyLargeEmphasis, color = Gray800, modifier = Modifier.weight(1f))
                    if (extraInfoCount != null) {
                        Text("$extraInfoCount", style = HitbosssType.bodyDefaultEmphasis, color = Gray500)
                    } else if (isExpandable) {
                        Icon(
                            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null, tint = Gray500, modifier = Modifier.size(24.dp),
                        )
                    }
                }
                if (!isExpandable || expanded) extraContent()
            }
        }
        val shareSection: @Composable () -> Unit = {
            InfoCard(onClick = onShare) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Primary100), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.IosShare, contentDescription = null, tint = Primary500, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(shareTitle, style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                            Text(shareSubtitle, style = HitbosssType.bodySmallRegular, color = Gray500)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Gray500, modifier = Modifier.size(24.dp))
                }
            }
        }

        if (contextType == CommunityInfoContextType.Event) {
            descriptionSection(); detailsSection(); memberSection(); dynamicSection(); shareSection()
        } else {
            descriptionSection(); mottoSection(); memberSection(); dynamicSection(); detailsSection(); shareSection()
        }
    }
}

/** Tarjeta blanca de sección (gray100, rounded 12, padding 16). */
@Composable
private fun InfoCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100)
            .let { if (onClick != null) it.clickable { onClick() } else it }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/** Cabecera de sección: icono rosa (Primary500) + título. */
@Composable
private fun InfoSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = Primary500, modifier = Modifier.size(16.dp))
        Text(title, style = HitbosssType.bodyLargeEmphasis, color = Gray800)
    }
}

/** Fila de detalle: icono en caja gris (36dp) + título/valor apilados. */
@Composable
private fun CommunityDetailRow(row: CommunityDetailRowData) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Gray200), contentAlignment = Alignment.Center) {
            Icon(row.icon, contentDescription = null, tint = Gray800, modifier = Modifier.size(24.dp))
        }
        Column {
            Text(stringResource(row.titleRes), style = HitbosssType.bodySmallRegular, color = Gray500)
            Text(row.value, style = HitbosssType.bodyDefaultRegular, color = Gray800)
        }
    }
}

/** Fila de administrador: avatar + (nombre / rol Creador-Admin como subtítulo). */
@Composable
private fun AdminRow(member: Member, isCreator: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = member.profilePic, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Gray200),
        )
        Column {
            Text(member.username, style = HitbosssType.bodyDefaultRegular, color = Gray800)
            Text(
                stringResource(if (isCreator) R.string.common_creator else R.string.common_admin),
                style = HitbosssType.bodySmallRegular, color = Gray500,
            )
        }
    }
}

/**
 * Ejercicios agrupados por deporte para la pestaña Información del grupo (1:1 iOS): muestra TODOS los
 * ejercicios; los del grupo en color y el resto en gris.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupExercisesContent(availableApiKeys: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        com.hitbosss.domain.model.Sport.entries.forEach { sport ->
            val isPl = sport == com.hitbosss.domain.model.Sport.Powerlifting
            val sportColor = if (isPl) com.hitbosss.presentation.designsystem.theme.Secondary500 else com.hitbosss.presentation.designsystem.theme.Error500
            val chipBg = if (isPl) Secondary100 else com.hitbosss.presentation.designsystem.theme.Error100
            val officialKey = if (isPl) "officialPowerlifting" else "officialCrossfit"
            val officialLabel = if (isPl) "Powerlifting" else "CrossHIT"
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(sport.brandTitle.uppercase(), style = HitbosssType.bodySmallEmphasis, color = sportColor)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val items = listOf(officialKey to officialLabel) +
                        com.hitbosss.domain.model.Exercise.forSport(sport).map { it.apiValue to null }
                    items.forEach { (apiKey, fixedLabel) ->
                        val available = availableApiKeys.any { it.equals(apiKey, true) }
                        Text(
                            fixedLabel ?: infoExerciseLabel(apiKey),
                            style = HitbosssType.bodyDefaultRegular, color = if (available) Gray800 else Gray400,
                            modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(if (available) chipBg else Gray100)
                                .border(1.dp, if (available) sportColor else com.hitbosss.presentation.designsystem.theme.Gray300, RoundedCornerShape(32.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun infoExerciseLabel(apiKey: String): String =
    com.hitbosss.presentation.feature.ranking.exerciseTitleResByApi(apiKey)?.let { stringResource(it) }
        ?: apiKey.replaceFirstChar { it.uppercase() }

/** Selector segmentado Público/Privado (1:1 con CommunityVisibility de iOS). */
@Composable
fun VisibilitySegment(isPublic: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Gray200).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(true to R.string.visibility_public, false to R.string.visibility_private).forEach { (value, label) ->
            val sel = value == isPublic
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Gray100 else Color.Transparent).clickable { onChange(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = if (sel) HitbosssType.bodyDefaultEmphasis else HitbosssType.bodyDefaultRegular,
                    color = if (sel) Gray800 else Gray500,
                )
            }
        }
    }
}
