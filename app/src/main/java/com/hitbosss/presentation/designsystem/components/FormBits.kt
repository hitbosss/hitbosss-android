package com.hitbosss.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Piezas de formulario compartidas por crear/editar grupo, evento y perfil. */

@Composable
fun FieldLabel(text: String) =
    Text(text, style = HitbosssType.bodyDefaultEmphasis, color = Gray500, modifier = Modifier.padding(bottom = 8.dp))

@Composable
fun Counter(count: Int, max: Int) = Text(
    "$count/$max", style = HitbosssType.bodySmallRegular, color = Gray500,
    textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
)

@Composable
fun FormField(value: String, onChange: (String) -> Unit, height: Dp, single: Boolean = true) {
    Box(
        Modifier.fillMaxWidth().let { if (single) it.height(height) else it.heightIn(min = height) }
            .clip(RoundedCornerShape(8.dp)).background(Gray200).border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(16.dp),
        contentAlignment = if (single) Alignment.CenterStart else Alignment.TopStart,
    ) {
        BasicTextField(value, onChange, singleLine = single, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun DateRow(label: String, millis: Long?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = HitbosssType.bodyDefaultEmphasis, color = Gray500, modifier = Modifier.weight(1f))
        Box(
            Modifier.clip(RoundedCornerShape(8.dp)).background(Gray200).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(millis?.let { formatMillisDate(it) } ?: stringResource(R.string.common_select), style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDatePicker(initial: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial ?: System.currentTimeMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { pickerState.selectedDateMillis?.let(onPick) }) { Text(stringResource(R.string.common_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    ) { DatePicker(state = pickerState) }
}

/** Fecha desde epoch en SEGUNDOS (lo que da la API, donde <=0 significa "sin fecha" → `blank`). */
fun formatEpochDate(epochSec: Long, pattern: String = "dd/MM/yyyy", blank: String = ""): String =
    if (epochSec <= 0) blank
    else SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epochSec * 1000))

/** Fecha desde MILISEGUNDOS (los DatePicker de Material). Sin guarda: una fecha de nacimiento
 *  anterior a 1970 es negativa y hay que pintarla igual. */
fun formatMillisDate(millis: Long, pattern: String = "dd/MM/yyyy"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
