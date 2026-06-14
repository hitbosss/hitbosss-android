package com.hitbosss.presentation.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hitbosss.R
import com.hitbosss.domain.model.Member
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500

/**
 * Lista de miembros con gestión de admin (1:1 con CommunityMembersView de iOS).
 * Al tocar un miembro se abre un action sheet "Opciones para X": Visitar perfil, y si el usuario
 * actual es admin y el miembro no lo es → Convertir en administrador / Expulsar miembro (con popups).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersManageDialog(
    members: List<Member>,
    titleRes: Int,
    isCurrentUserAdmin: Boolean,
    onOpenProfile: (String) -> Unit,
    onMakeAdmin: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf<Member?>(null) }
    var showMakeAdmin by remember { mutableStateOf(false) }
    var showRemove by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
        title = { Text(stringResource(titleRes), style = HitbosssType.titleBody) },
        text = {
            LazyColumn {
                items(members) { m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { selected = m }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(model = m.profilePic, contentDescription = null, contentScale = ContentScale.Crop, placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(), modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Gray200))
                        Text(m.username, style = HitbosssType.bodyDefaultRegular, color = Gray800, modifier = Modifier.weight(1f))
                        if (m.isAdmin) Text(stringResource(R.string.common_admin), style = HitbosssType.bodySmallEmphasis, color = Primary500)
                    }
                }
            }
        },
    )

    selected?.let { m ->
        val showAdminOptions = isCurrentUserAdmin && !m.isAdmin
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Gray100,
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
                Text(
                    stringResource(R.string.member_options_title, m.username),
                    style = HitbosssType.bodySmallRegular, color = Gray500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
                SheetButton(stringResource(R.string.member_view_profile), Gray800) { onOpenProfile(m.userId); selected = null; onDismiss() }
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
            onConfirm = { showMakeAdmin = false; m?.let { onMakeAdmin(it.userId) }; selected = null; onDismiss() },
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
            onConfirm = { showRemove = false; m?.let { onRemove(it.userId) }; selected = null; onDismiss() },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showRemove = false },
            onDismissRequest = { showRemove = false },
        )
    }
}

@Composable
private fun SheetButton(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        text,
        style = HitbosssType.bodyLargeEmphasis, color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
    )
}
