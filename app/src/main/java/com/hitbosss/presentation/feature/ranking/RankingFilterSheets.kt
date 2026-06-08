package com.hitbosss.presentation.feature.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary800

// MARK: - Hoja "Ordenar por"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingSortSheet(current: RankingOrder, onSelect: (RankingOrder) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Gray100) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Ordenar por", style = HitbosssType.titleBody, color = Gray800)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300).padding(top = 0.dp))
            Spacer(Modifier.height(8.dp))
            RankingOrder.entries.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(option); onDismiss() }
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (current == option) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null, tint = Gray800, modifier = Modifier.size(22.dp),
                    )
                    Text(option.label, style = HitbosssType.bodyLargeRegular, color = Gray800)
                }
            }
        }
    }
}

// MARK: - Hoja "Configurar ranking"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingFilterSheet(
    location: RankingLocation,
    selectedLevels: Set<String>,
    selectedAges: Set<AgeCategory>,
    gender: RankingGender,
    onLocation: (RankingLocation) -> Unit,
    onToggleLevel: (String) -> Unit,
    onToggleAge: (AgeCategory) -> Unit,
    onGender: (RankingGender) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Gray100) {
        Column(Modifier.fillMaxWidth()) {
            Text("Configurar ranking", style = HitbosssType.titleBody, color = Gray800, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300))

            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionTitle("Localización")
                Segmented(
                    options = RankingLocation.entries, selected = location, label = { it.label }, onSelect = onLocation,
                )

                SectionTitle("Niveles")
                // 2 columnas de checkboxes
                val rows = levelFilterOptions.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { pair ->
                        Row(Modifier.fillMaxWidth()) {
                            pair.forEach { (key, label) ->
                                Box(Modifier.weight(1f)) {
                                    CheckboxRow(label, key in selectedLevels) { onToggleLevel(key) }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                SectionTitle("Edad")
                Column {
                    AgeCategory.entries.forEach { age ->
                        CheckboxRow(age.label, age in selectedAges) { onToggleAge(age) }
                    }
                }

                SectionTitle("Género")
                Segmented(
                    options = RankingGender.entries, selected = gender, label = { it.label }, onSelect = onGender,
                )
            }

            // Guardar Cambios
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 40.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Secondary800).clickable { onSave(); onDismiss() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Guardar Cambios", style = HitbosssType.bodyLargeEmphasis, color = Gray100)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = HitbosssType.titleBody, color = Gray800)
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = null, tint = Gray800, modifier = Modifier.size(24.dp),
        )
        Text(label, style = HitbosssType.bodyLargeRegular, color = Gray800)
    }
}

/** Selector segmentado (pill blanca = activo), igual que SelectionButton de iOS. */
@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Gray200).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val sel = option == selected
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp))
                    .background(if (sel) Gray100 else Color.Transparent).clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = if (sel) HitbosssType.bodyDefaultEmphasis else HitbosssType.bodyDefaultRegular,
                    color = if (sel) Gray800 else Gray500,
                )
            }
        }
    }
}
