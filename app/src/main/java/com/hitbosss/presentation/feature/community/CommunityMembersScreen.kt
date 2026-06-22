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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.R
import com.hitbosss.domain.model.Member
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary100
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Primary800

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityMembersScreen(
    titleRes: Int,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    viewModel: CommunityMembersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Member?>(null) }
    var showMakeAdmin by remember { mutableStateOf(false) }
    var showRemove by remember { mutableStateOf(false) }

    val filtered = state.members.filter { it.username.contains(search.trim(), ignoreCase = true) }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = Gray800, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Text(stringResource(titleRes), style = HitbosssType.titleSubsection, color = Gray800)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300))

        // Buscador
        Row(
            Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(10.dp)).background(Gray200).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Gray500, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f)) {
                if (search.isEmpty()) Text(stringResource(R.string.common_search), style = HitbosssType.bodyDefaultRegular, color = Gray500)
                BasicTextField(value = search, onValueChange = { search = it }, singleLine = true, textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800), modifier = Modifier.fillMaxWidth())
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300))

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(filtered) { index, m ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = m }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AsyncImage(model = m.profilePic, contentDescription = null, contentScale = ContentScale.Crop, placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(), modifier = Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)).background(Gray200))
                        Text(m.username, style = HitbosssType.bodyDefaultRegular, color = Gray800, modifier = Modifier.weight(1f))
                        val isCreator = m.userId == state.creatorId
                        if (isCreator || m.isAdmin) {
                            Text(
                                stringResource(if (isCreator) R.string.common_creator else R.string.common_admin).uppercase(),
                                style = HitbosssType.bodySmallRegular, color = Primary800,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Primary100).padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (index < filtered.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300))
                }
            }
        }
    }

    selected?.let { m ->
        val showAdminOptions = state.isCurrentUserAdmin && !m.isAdmin
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Gray100,
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
                Text(
                    stringResource(R.string.member_options_title, m.username),
                    style = HitbosssType.bodySmallRegular, color = Gray500, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
                SheetButton(stringResource(R.string.member_view_profile), Gray800) { val id = m.userId; selected = null; onOpenUserProfile(id) }
                if (showAdminOptions) {
                    SheetButton(stringResource(R.string.member_make_admin), Gray800) { showMakeAdmin = true }
                    SheetButton(stringResource(R.string.member_remove), Error500) { showRemove = true }
                }
                SheetButton(stringResource(R.string.common_cancel), Gray500) { selected = null }
            }
        }
    }

    if (showMakeAdmin) {
        val m = selected
        HitPopup(
            title = stringResource(R.string.member_make_admin_title),
            message = stringResource(R.string.member_make_admin_msg, m?.username ?: ""),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = { showMakeAdmin = false; m?.let { viewModel.makeAdmin(it.userId) }; selected = null },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showMakeAdmin = false },
            onDismissRequest = { showMakeAdmin = false },
        )
    }
    if (showRemove) {
        val m = selected
        HitPopup(
            title = stringResource(R.string.member_remove_title),
            message = stringResource(R.string.member_remove_msg, m?.username ?: ""),
            confirmText = stringResource(R.string.common_accept),
            confirmType = HitButtonType.Destructive,
            onConfirm = { showRemove = false; m?.let { viewModel.removeMember(it.userId) }; selected = null },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showRemove = false },
            onDismissRequest = { showRemove = false },
        )
    }
    state.error?.let { error ->
        HitPopup(
            title = stringResource(R.string.common_something_wrong),
            message = error,
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearError,
            onDismissRequest = viewModel::clearError,
        )
    }
}

@Composable
private fun SheetButton(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text, style = HitbosssType.bodyLargeEmphasis, color = color, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
    )
}
