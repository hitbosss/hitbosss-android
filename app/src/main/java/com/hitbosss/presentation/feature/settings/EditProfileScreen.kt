package com.hitbosss.presentation.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray700
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.feature.ranking.countryFlag
import java.util.Calendar
import androidx.compose.ui.res.stringResource
import com.hitbosss.presentation.designsystem.components.HitPopup
import kotlinx.coroutines.launch
import com.hitbosss.presentation.designsystem.components.Counter
import com.hitbosss.presentation.designsystem.components.FieldLabel
import com.hitbosss.presentation.designsystem.components.formatMillisDate

@Composable
fun EditProfileScreen(onBack: () -> Unit, onSaved: () -> Unit = onBack, viewModel: EditProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickProfile = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(viewModel::onProfilePicked) }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(viewModel::onCoverPicked) }

    var showDate by remember { mutableStateOf(false) }
    var showCountry by remember { mutableStateOf(false) }

    // Al guardar se vuelve al perfil (recargado); el botón atrás vuelve a Ajustes.
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.settings_edit_profile), onBack = onBack)
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
            return@Column
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            LabelEditRow(stringResource(R.string.edit_main_photo)) { pickProfile.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            AsyncImage(
                model = state.profilePicUri ?: state.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop,
                placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                // Centrada como en iOS (la foto va dentro de un VStack, que centra en horizontal).
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp).size(90.dp).clip(RoundedCornerShape(10.dp)).background(Gray200),
            )
            LabelEditRow(stringResource(R.string.edit_cover)) { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            AsyncImage(
                model = state.coverPicUri ?: state.coverPicUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Gray400),
            )

            FieldLabel(stringResource(R.string.edit_name))
            ProfileTextField(state.fullName, viewModel::onFullName)
            Counter(state.fullName.length, viewModel.maxNameLength)
            Spacer(Modifier.height(8.dp))

            FieldLabel(stringResource(R.string.edit_username))
            ProfileTextField(state.username, viewModel::onUsername)
            Counter(state.username.length, viewModel.maxUsernameLength)
            Spacer(Modifier.height(8.dp))

            FieldLabel(stringResource(R.string.common_description))
            DescriptionField(state.description, viewModel::onDescription)
            Counter(state.description.length, viewModel.maxDescriptionLength)
            Spacer(Modifier.height(8.dp))

            FieldLabel(stringResource(R.string.edit_social))
            SocialField(R.drawable.im_icon_facebook, stringResource(R.string.edit_facebook), state.facebook, viewModel::onFacebook)
            Spacer(Modifier.height(8.dp))
            SocialField(R.drawable.im_icon_instagram, stringResource(R.string.edit_instagram), state.instagram, viewModel::onInstagram)
            Spacer(Modifier.height(8.dp))
            SocialField(R.drawable.im_icon_tik_tok, stringResource(R.string.edit_tiktok), state.tiktok, viewModel::onTiktok)
            Spacer(Modifier.height(8.dp))
            SocialField(R.drawable.im_icon_x, stringResource(R.string.edit_x), state.x, viewModel::onX)
            Spacer(Modifier.height(24.dp))

            FieldLabel(stringResource(R.string.edit_gender))
            GenderSegmented(state.gender, viewModel::onGender)
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_birth), style = HitbosssType.bodyDefaultEmphasis, color = Gray500, modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Gray200).clickable { showDate = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(state.birthDate?.let { formatMillisDate(it, "d MMM yyyy") } ?: stringResource(R.string.common_select), style = HitbosssType.bodyDefaultEmphasis, color = Gray800)
                }
            }
            Spacer(Modifier.height(24.dp))

            FieldLabel(stringResource(R.string.edit_country))
            DropdownBox(
                content = {
                    if (state.countryCode.isNotBlank()) {
                        Text(countryFlag(state.countryCode), style = HitbosssType.bodyLargeRegular)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(CountryData.label(state.countryCode).ifBlank { stringResource(R.string.edit_select_country) }, style = HitbosssType.bodyDefaultRegular, color = Gray800)
                },
                onClick = { showCountry = true },
            )
            Spacer(Modifier.height(24.dp))

            FieldLabel(stringResource(R.string.edit_measurement_system))
            SystemDropdown(state.system, viewModel::onSystem)
            Spacer(Modifier.height(24.dp))

            FieldLabel(stringResource(R.string.edit_weight))
            UnitField(state.weightText, viewModel::onWeight, state.weightUnit)
            Spacer(Modifier.height(24.dp))

            FieldLabel(stringResource(R.string.edit_height))
            if (state.system == MeasureSystem.Metric) {
                UnitField(state.heightCmText, viewModel::onHeightCm, "CM")
            } else {
                UnitField(state.heightFeetText, viewModel::onHeightFeet, "FT")
                Spacer(Modifier.height(8.dp))
                UnitField(state.heightInchesText, viewModel::onHeightInches, "IN")
            }
        }

        val enabled = state.canSave
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp)).background(if (enabled) Secondary800 else Gray300)
                .clickable(enabled = enabled) { viewModel.save() }.padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state.saving) CircularProgressIndicator(color = Gray100, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.edit_save), style = HitbosssType.bodyLargeEmphasis, color = if (enabled) Gray100 else Gray500)
        }
    }

    if (showDate) BirthDatePicker(state.birthDate, onPick = { viewModel.onBirthDate(it); showDate = false }, onDismiss = { showDate = false })
    if (showCountry) CountryPickerDialog(onPick = { viewModel.onCountry(it); showCountry = false }, onDismiss = { showCountry = false })

    state.error?.let {
        HitPopup(
            title = stringResource(R.string.common_something_wrong),
            message = stringResource(R.string.common_unexpected_error_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearError,
            onDismissRequest = viewModel::clearError,
        )
    }
}

// MARK: - Campos

@Composable
private fun LabelEditRow(label: String, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = HitbosssType.bodySmallEmphasis, color = Gray500, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.common_edit), style = HitbosssType.bodySmallEmphasis, color = Secondary500, modifier = Modifier.clickable { onEdit() })
    }
}

@Composable
private fun ProfileTextField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray200)
            .border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(value, onChange, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DescriptionField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(8.dp)).background(Gray200)
            .border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(16.dp),
    ) {
        BasicTextField(value, onChange, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SocialField(@DrawableRes icon: Int, placeholder: String, value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray200)
            .border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icono de marca de iOS (iconInstagram/iconX/iconTikTok/iconFacebook), 24x24.
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = HitbosssType.bodyDefaultRegular, color = Gray500)
            BasicTextField(value, onChange, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun GenderSegmented(gender: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray200).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("male" to stringResource(R.string.common_male), "female" to stringResource(R.string.common_female)).forEach { (key, label) ->
            val sel = gender == key
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(if (sel) Gray100 else Color.Transparent)
                    .clickable { onSelect(key) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = HitbosssType.bodyDefaultEmphasis, color = if (sel) Gray700 else Gray500)
            }
        }
    }
}

@Composable
private fun DropdownBox(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray100)
            .border(1.dp, Gray300, RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        Spacer(Modifier.weight(1f))
        androidx.compose.material3.Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Gray800)
    }
}

@Composable
private fun SystemDropdown(system: MeasureSystem, onSelect: (MeasureSystem) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        DropdownBox(content = { Text(system.display, style = HitbosssType.bodyDefaultRegular, color = Gray800) }, onClick = { expanded = true })
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MeasureSystem.entries.forEach { s ->
                androidx.compose.material3.DropdownMenuItem(text = { Text(s.display, style = HitbosssType.bodyLargeRegular) }, onClick = { onSelect(s); expanded = false })
            }
        }
    }
}

@Composable
private fun UnitField(value: String, onChange: (String) -> Unit, unit: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(8.dp)).background(Gray200)
                .border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value, onChange, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Gray300, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(unit, style = HitbosssType.bodyLargeEmphasis, color = Gray500)
        }
    }
}

// MARK: - Pickers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePicker(current: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val cal = Calendar.getInstance()
    val maxMillis = cal.apply { add(Calendar.YEAR, -18) }.timeInMillis
    val minMillis = Calendar.getInstance().apply { add(Calendar.YEAR, -90) }.timeInMillis
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = current ?: maxMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in minMillis..maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { pickerState.selectedDateMillis?.let(onPick) }) { Text(stringResource(R.string.common_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    ) { DatePicker(state = pickerState) }
}

@Composable
private fun CountryPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) CountryData.all
        else CountryData.all.filter { it.name.contains(query, true) || it.code.contains(query, true) }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gray100).padding(16.dp)) {
            Text(stringResource(R.string.edit_country), style = HitbosssType.titleBody, color = Gray800)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(Gray200).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) Text(stringResource(R.string.onboarding_search_country), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                BasicTextField(query, { query = it }, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(filtered) { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(c.code) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(countryFlag(c.code), style = HitbosssType.bodyLargeRegular)
                        Text(c.name, style = HitbosssType.bodyDefaultRegular, color = Gray800)
                    }
                }
            }
        }
    }
}

