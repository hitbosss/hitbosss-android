package com.hitbosss.presentation.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary100
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.feature.ranking.VideoPlayer
import androidx.compose.ui.res.stringResource
import com.hitbosss.R

@Composable
fun TutorialScreen(initialApiKey: String = "officialPowerlifting", onBack: () -> Unit) {
    val tutorialData = rememberTutorialData()
    var sport by remember { mutableStateOf(tutorialData.firstOrNull { it.apiKey == initialApiKey }?.sport ?: "powerlifting") }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    val exercises = tutorialData.filter { it.sport == sport }
    val current = tutorialData.firstOrNull { it.apiKey == apiKey } ?: exercises.first()
    val sportColor = if (sport == "powerlifting") Secondary500 else Error500

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.settings_exercise_tutorial), onBack = onBack)

        // Selector de deporte
        SportDropdown(sport) { newSport ->
            sport = newSport
            apiKey = tutorialData.first { it.sport == newSport }.apiKey
        }

        // Tabs de ejercicio
        LazyRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(exercises) { ex ->
                val sel = ex.apiKey == apiKey
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { apiKey = ex.apiKey }) {
                    Text(
                        ex.title, style = HitbosssType.titleBody,
                        color = if (sel) Gray800 else Gray500,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Box(Modifier.height(2.dp).width(if (sel) 40.dp else 0.dp).background(sportColor))
                }
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Info box
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Secondary100).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Secondary500, modifier = Modifier.size(20.dp))
                Text(boldMarkdown(current.info), style = HitbosssType.bodyDefaultRegular, color = Gray800)
            }

            // Vídeo del tutorial
            current.videoUrl?.let { url ->
                Box(Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                    VideoPlayer(url = url, seekSeconds = 0.0, isActive = true)
                }
            }

            // Secciones (acordeón)
            current.sections.forEach { TutorialAccordion(it) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SportDropdown(sport: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray100)
                .border(1.dp, Gray400, RoundedCornerShape(8.dp)).clickable { expanded = true }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (sport == "powerlifting") "Powerlifting" else "CrossHIT", style = HitbosssType.bodyLargeRegular, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Gray800)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("powerlifting" to "Powerlifting", "crossfit" to "CrossHIT").forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label, style = HitbosssType.bodyLargeRegular) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

@Composable
private fun TutorialAccordion(section: TutorialSection) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(section.title, style = HitbosssType.bodyLargeEmphasis, color = Gray600, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Gray600, modifier = Modifier.rotate(if (expanded) 0f else 180f))
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray400))
        if (expanded) {
            Text(boldMarkdown(section.body), style = HitbosssType.bodyDefaultRegular, color = Gray500, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Convierte **negrita** de markdown a texto con estilo. */
private fun boldMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val regex = Regex("""\*\*(.+?)\*\*""")
    var last = 0
    regex.findAll(text).forEach { m ->
        append(text.substring(last, m.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(m.groupValues[1]) }
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}
