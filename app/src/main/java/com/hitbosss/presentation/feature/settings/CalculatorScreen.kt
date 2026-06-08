package com.hitbosss.presentation.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hitbosss.domain.util.WilksCalculator
import com.hitbosss.presentation.designsystem.components.HitTextField
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary100
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Primary700

@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var bodyWeight by remember { mutableStateOf("") }
    var lift by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var unit by remember { mutableStateOf("kg") }

    val wilks: Double? = run {
        val bw = bodyWeight.toDoubleOrNull()
        val lf = lift.toDoubleOrNull()
        if (bw == null || lf == null || bw <= 0 || lf <= 0) return@run null
        val factor = if (unit == "lbs") WilksCalculator.KG_TO_LBS else 1.0
        WilksCalculator.calculate(bw / factor, lf / factor, gender)
    }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = "Calculadora de puntos", onBack = onBack)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Género", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
            Toggle(listOf("male" to "Hombre", "female" to "Mujer"), gender) { gender = it }

            Text("Unidad", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
            Toggle(listOf("kg" to "kg", "lbs" to "lbs"), unit) { unit = it }

            Text("Peso corporal ($unit)", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
            HitTextField(bodyWeight, { bodyWeight = it.filter { c -> c.isDigit() || c == '.' } }, "0", keyboardType = KeyboardType.Decimal)

            Text("Peso levantado ($unit)", style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
            HitTextField(lift, { lift = it.filter { c -> c.isDigit() || c == '.' } }, "0", keyboardType = KeyboardType.Decimal)

            Column(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Primary100).padding(16.dp),
            ) {
                Text("Puntos Wilks", style = HitbosssType.bodyDefaultRegular, color = Gray500)
                Text(
                    wilks?.let { "%.2f".format(it) } ?: "—",
                    style = HitbosssType.titleSection,
                    color = Primary700,
                )
            }
        }
    }
}

@Composable
private fun Toggle(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val sel = value == selected
            Text(
                label,
                style = HitbosssType.bodyDefaultEmphasis,
                color = if (sel) Gray100 else Gray500,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) Primary500 else Gray200)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
