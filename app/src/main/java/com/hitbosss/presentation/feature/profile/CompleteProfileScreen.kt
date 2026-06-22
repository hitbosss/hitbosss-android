package com.hitbosss.presentation.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.WheelPicker
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary100
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Success100
import com.hitbosss.presentation.designsystem.theme.Success500
import com.hitbosss.presentation.feature.ranking.countryFlag
import com.hitbosss.presentation.feature.settings.CountryData
import java.util.Calendar
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.hitbosss.presentation.designsystem.components.HitPopup

@Composable
fun CompleteProfileScreen(
    onRegistered: () -> Unit,
    onCancel: () -> Unit = {},
    viewModel: CompleteProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(1) }
    val total = 6

    LaunchedEffect(state.success) { if (state.success) onRegistered() }
    LaunchedEffect(state.usernameTaken) { if (state.usernameTaken) step = 6 }

    fun back() { if (step == 1) onCancel() else step-- }
    BackHandler { back() }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        // Cabecera
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Gray800, modifier = Modifier.size(24.dp).clickable { back() })
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.onboarding_title), style = HitbosssType.titleSubsection, color = Gray800)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))

        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
            // Barra de progreso
            Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(total) { i ->
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i < step) Primary500 else Gray200))
                }
            }
            Text(stringResource(R.string.onboarding_step, step, total), style = HitbosssType.bodyDefaultRegular, color = Gray500, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    1 -> GenderStep(state.gender, viewModel::onGender)
                    2 -> CountryStep(state.countryCode, viewModel::onCountry)
                    3 -> HeightStep(state, viewModel)
                    4 -> WeightStep(state, viewModel)
                    5 -> BirthStep(state, viewModel)
                    6 -> UsernameStep(state, viewModel::onUsername)
                }
            }

            // Continuar (1:1 con iOS: RectangleButton tipo .secondary, tamaño large).
            HitButton(
                text = stringResource(R.string.common_continue),
                onClick = { if (step < total) step++ else viewModel.submit() },
                type = HitButtonType.Secondary,
                enabled = state.validateStep(step) && !state.isLoading,
                loading = state.isLoading,
                modifier = Modifier.padding(bottom = 32.dp, top = 8.dp),
            )
        }
    }

    // Popup de error al guardar el perfil (1:1 con iOS: texto fijo "Error inesperado").
    state.error?.let {
        HitPopup(
            title = stringResource(R.string.common_unexpected_error),
            message = stringResource(R.string.common_unexpected_error_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearError,
            onDismissRequest = viewModel::clearError,
        )
    }
}

@Composable
private fun StepTitle(text: String) =
    Text(text, style = HitbosssType.titleSection, color = Gray800, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

@Composable
private fun GenderStep(gender: String, onGender: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
        StepTitle(stringResource(R.string.onboarding_gender))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GenderCard(R.drawable.ic_gender_male, stringResource(R.string.common_male), gender == "male", Modifier.weight(1f)) { onGender("male") }
            GenderCard(R.drawable.ic_gender_female, stringResource(R.string.common_female), gender == "female", Modifier.weight(1f)) { onGender("female") }
        }
    }
}

@Composable
private fun GenderCard(res: Int, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, if (selected) Gray500 else Gray300, RoundedCornerShape(8.dp)).clickable { onClick() }.padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painterResource(res), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(168f / 230f).clip(RoundedCornerShape(8.dp)).alpha(if (selected) 1f else 0.5f),
        )
        Text(label, style = HitbosssType.bodyLargeRegular, color = Gray500)
    }
}

@Composable
private fun CountryStep(code: String, onCountry: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
        StepTitle(stringResource(R.string.onboarding_country))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(1.dp, Gray300, RoundedCornerShape(8.dp)).clickable { showDialog = true }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(countryFlag(code), style = HitbosssType.titleSubsection)
            Text(CountryData.label(code), style = HitbosssType.bodyLargeRegular, color = Gray800, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Gray800)
        }
    }
    if (showDialog) CountryDialog(onPick = { onCountry(it); showDialog = false }, onDismiss = { showDialog = false })
}

@Composable
private fun CountryDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val list = CountryData.all.filter { query.isBlank() || it.name.contains(query, true) || it.code.contains(query, true) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gray100).padding(16.dp).height(520.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray200).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Gray500)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.onboarding_search_country), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                    BasicTextField(query, { query = it }, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(list) { c ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(c.code) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(countryFlag(c.code), style = HitbosssType.titleSubsection)
                        Text(CountryData.label(c.code), style = HitbosssType.bodyLargeRegular, color = Gray800)
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemBadge(state: CompleteProfileUiState) {
    val metric = state.system == MeasurementSystem.Metric
    Text(
        if (metric) stringResource(R.string.system_metric) else stringResource(R.string.system_imperial),
        style = HitbosssType.bodySmallEmphasis, color = if (metric) Secondary500 else Success500,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (metric) Secondary100 else Success100).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun HeightStep(state: CompleteProfileUiState, vm: CompleteProfileViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_height))
        SystemBadge(state)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // key() estable por rueda: en métrico hay 1 rueda y en imperial 2, así que sin clave
            // Compose reutilizaría posicionalmente el LazyListState y la rueda de unidad quedaba
            // "atascada" al cambiar de sistema (no dejaba volver de FT/IN a CM).
            if (state.system == MeasurementSystem.Metric) {
                val cms = (130..230).toList()
                key("h_cm") { WheelPicker(cms.map { it.toString() }, cms.indexOf(state.heightCm).coerceAtLeast(0), { vm.onHeightCm(cms[it]) }, Modifier.weight(1f)) }
            } else {
                val feet = (4..7).toList(); val inches = (0..11).toList()
                key("h_feet") { WheelPicker(feet.map { it.toString() }, feet.indexOf(state.heightFeet).coerceAtLeast(0), { vm.onHeightFeet(feet[it]) }, Modifier.weight(1f)) }
                key("h_inches") { WheelPicker(inches.map { it.toString() }, inches.indexOf(state.heightInches).coerceAtLeast(0), { vm.onHeightInches(inches[it]) }, Modifier.weight(1f)) }
            }
            key("h_unit") { UnitWheel(state.system, listOf("CM", "FT/IN"), vm) }
        }
    }
}

@Composable
private fun WeightStep(state: CompleteProfileUiState, vm: CompleteProfileViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(stringResource(R.string.onboarding_weight))
        SystemBadge(state)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val ints = if (state.system == MeasurementSystem.Metric) (45..200).toList() else (100..440).toList()
            val decs = (0..9).toList()
            key("w_int") { WheelPicker(ints.map { it.toString() }, ints.indexOf(state.weightInteger).coerceAtLeast(0), { vm.onWeightInteger(ints[it]) }, Modifier.weight(1f)) }
            key("w_dec") { WheelPicker(decs.map { ".$it" }, decs.indexOf(state.weightDecimal).coerceAtLeast(0), { vm.onWeightDecimal(decs[it]) }, Modifier.weight(1f)) }
            key("w_unit") { UnitWheel(state.system, listOf("KG", "LB"), vm) }
        }
    }
}

@Composable
private fun UnitWheel(system: MeasurementSystem, labels: List<String>, vm: CompleteProfileViewModel) {
    WheelPicker(
        labels, if (system == MeasurementSystem.Metric) 0 else 1,
        { vm.onSystem(if (it == 0) MeasurementSystem.Metric else MeasurementSystem.Imperial) },
        Modifier.width(90.dp), highlight = false,
    )
}

@Composable
private fun BirthStep(state: CompleteProfileUiState, vm: CompleteProfileViewModel) {
    val nowYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (nowYear - 90..nowYear - 18).toList()
    val days = (1..31).toList()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
        StepTitle(stringResource(R.string.onboarding_birth))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            key("b_day") { WheelPicker(days.map { it.toString() }, days.indexOf(state.birthDay).coerceAtLeast(0), { vm.onBirth(days[it], state.birthMonth, state.birthYear) }, Modifier.weight(0.8f)) }
            key("b_month") { WheelPicker(stringArrayResource(R.array.months).toList(), state.birthMonth - 1, { vm.onBirth(state.birthDay, it + 1, state.birthYear) }, Modifier.weight(1.4f)) }
            key("b_year") { WheelPicker(years.map { it.toString() }, years.indexOf(state.birthYear).coerceAtLeast(0), { vm.onBirth(state.birthDay, state.birthMonth, years[it]) }, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun UsernameStep(state: CompleteProfileUiState, onUsername: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
        StepTitle(stringResource(R.string.onboarding_username_q))
        // Campo + hueco fijo de validación debajo (no desplaza el layout al aparecer el texto rojo).
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray200).padding(16.dp)) {
                if (state.username.isEmpty()) Text(stringResource(R.string.onboarding_username_ph), style = HitbosssType.bodyLargeRegular, color = Gray500)
                BasicTextField(state.username, onUsername, singleLine = true, textStyle = HitbosssType.bodyLargeRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
            }
            Box(Modifier.fillMaxWidth().height(18.dp).padding(start = 4.dp, top = 4.dp)) {
                if (state.usernameTaken) {
                    Text(stringResource(R.string.onboarding_username_taken), style = HitbosssType.bodySmallRegular, color = Error500, maxLines = 1)
                } else if (state.username.isNotEmpty() && !state.isUsernameValid) {
                    Text(stringResource(R.string.onboarding_username_invalid), style = HitbosssType.bodySmallRegular, color = Error500, maxLines = 1)
                }
            }
        }
    }
}
