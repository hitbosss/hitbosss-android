package com.hitbosss.presentation.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hitbosss.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray700
import com.hitbosss.presentation.designsystem.theme.Secondary100
import com.hitbosss.presentation.designsystem.theme.Secondary200
import com.hitbosss.presentation.designsystem.theme.Secondary500
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Info

@Composable
fun CalculatorScreen(
    onBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.calculator_title), onBack = onBack)

        // Tus Points (fijo bajo la cabecera).
        Column(
            Modifier.fillMaxWidth().padding(vertical = 21.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.calculator_your_points), style = HitbosssType.titleBody, color = Gray800)
            Text(
                if (state.score == 0.0) "0" else "%.2f".format(state.score),
                style = HitbosssType.titleScreen.copy(fontSize = 64.sp, lineHeight = 72.sp),
                color = Gray800,
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray400))

        // Cuerpo desplazable.
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            InfoCard()

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.calculator_gender), style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                GenderSegmented(state.gender, viewModel::onGender)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.calculator_body_weight), style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                    MeasurementField(state.bodyWeight, viewModel::onBodyWeight, state.unit.uppercase())
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.calculator_lifted_weight), style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                    MeasurementField(state.lift, viewModel::onLift, state.unit.uppercase())
                }
            }
        }

        // Botón fijo abajo.
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray400))
        HitButton(
            text = stringResource(R.string.calculator_calculate),
            onClick = viewModel::calculate,
            type = HitButtonType.Secondary,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
        )
    }
}

@Composable
private fun InfoCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Secondary100)
            .border(1.dp, Secondary200, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = Secondary500, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.calculator_what_are_title), style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
            Text(
                stringResource(R.string.calculator_what_are_desc),
                style = HitbosssType.bodySmallRegular,
                color = Gray800,
            )
        }
    }
}

@Composable
private fun GenderSegmented(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray200).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GenderSegment(stringResource(R.string.common_male), selected == "male", Modifier.weight(1f)) { onSelect("male") }
        GenderSegment(stringResource(R.string.common_female), selected == "female", Modifier.weight(1f)) { onSelect("female") }
    }
}

@Composable
private fun GenderSegment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        label,
        style = HitbosssType.bodyDefaultEmphasis,
        color = if (selected) Gray700 else Gray500,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Gray100 else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun MeasurementField(value: String, onValueChange: (String) -> Unit, unit: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray200)
                .border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Gray300, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(unit, style = HitbosssType.bodyLargeEmphasis, color = Gray500)
        }
    }
}
