package com.hitbosss.presentation.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Orange300
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Purple400
import com.hitbosss.presentation.designsystem.theme.Secondary500
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenCalculator: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onCommunityRules: () -> Unit = {},
    onTutorials: () -> Unit = {},
    onHiddenHits: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loggedOut.collect { onLoggedOut() } }

    fun sendMail(recipient: String, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, subject)) }
    }

    Column(Modifier.fillMaxSize().background(Gray200)) {
        HitTopBar(title = stringResource(R.string.settings_title), onBack = onBack)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1 — perfil / normas / tutorial
            SettingsCard {
                SettingsRow(Icons.Filled.Person, Primary500, stringResource(R.string.settings_edit_profile), onEditProfile)
                RowDivider()
                SettingsRow(Icons.AutoMirrored.Filled.MenuBook, Primary500, stringResource(R.string.settings_community_rules), onCommunityRules)
                RowDivider()
                SettingsRow(Icons.Filled.Videocam, Primary500, stringResource(R.string.settings_exercise_tutorial), onTutorials)
                RowDivider()
                SettingsRow(Icons.Filled.VisibilityOff, Primary500, stringResource(R.string.settings_hidden_hits), onHiddenHits)
            }
            // 2 — calculadora
            SettingsCard {
                SettingsRow(Icons.Filled.Calculate, Secondary500, stringResource(R.string.settings_points_calculator), onOpenCalculator)
            }
            // 3 — feedback / privacidad / soporte
            SettingsCard {
                val feedbackLabel = stringResource(R.string.settings_feedback)
                val supportLabel = stringResource(R.string.settings_support)
                SettingsRow(Icons.Filled.Forum, Purple400, feedbackLabel) {
                    sendMail("feedback@hitbosss.com", feedbackLabel)
                }
                RowDivider()
                SettingsRow(Icons.Filled.Lock, Purple400, stringResource(R.string.settings_privacy), onPrivacyPolicy)
                RowDivider()
                SettingsRow(Icons.AutoMirrored.Filled.HelpOutline, Purple400, supportLabel) {
                    sendMail("help@hitbosss.com", supportLabel)
                }
            }
            // 4 — idioma
            SettingsCard {
                SettingsRow(Icons.Filled.Language, Orange300, stringResource(R.string.settings_language)) { showLanguageDialog = true }
            }
            // 5 — compartir
            SettingsCard {
                val shareLabel = stringResource(R.string.settings_share_app)
                val shareText = stringResource(R.string.settings_share_text)
                SettingsRow(Icons.Filled.PersonAddAlt1, Color(0xFF34A853), shareLabel) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, shareLabel)) }
                }
            }
            // 6 — cerrar sesión / eliminar cuenta
            SettingsCard {
                SettingsRow(Icons.AutoMirrored.Filled.Logout, Gray600, stringResource(R.string.settings_logout)) { showLogoutDialog = true }
                RowDivider()
                SettingsRow(
                    Icons.Filled.Delete, Color(0xFFE0352B),
                    if (deleting) stringResource(R.string.settings_deleting_account) else stringResource(R.string.settings_delete_account),
                ) { if (!deleting) showDeleteDialog = true }
            }
        }
    }

    if (showLogoutDialog) {
        HitPopup(
            title = stringResource(R.string.settings_logout_title),
            message = stringResource(R.string.settings_logout_message),
            icon = painterResource(R.drawable.im_ico_close_session),
            confirmText = stringResource(R.string.common_confirm),
            onConfirm = { showLogoutDialog = false; viewModel.logout() },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showLogoutDialog = false },
            onDismissRequest = { showLogoutDialog = false },
        )
    }

    if (showDeleteDialog) {
        HitPopup(
            title = stringResource(R.string.settings_delete_title),
            message = stringResource(R.string.settings_delete_message),
            icon = painterResource(R.drawable.im_ico_trash),
            confirmText = stringResource(R.string.common_delete),
            confirmType = HitButtonType.Destructive,
            onConfirm = { showDeleteDialog = false; viewModel.deleteAccount() },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showDeleteDialog = false },
            onDismissRequest = { showDeleteDialog = false },
        )
    }

    if (showLanguageDialog) {
        HitPopup(
            title = stringResource(R.string.settings_language_title),
            message = stringResource(R.string.settings_language_message),
            icon = painterResource(R.drawable.im_ico_language),
            confirmText = stringResource(R.string.common_continue),
            onConfirm = { showLanguageDialog = false; openAppLanguageSettings(context) },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showLanguageDialog = false },
            onDismissRequest = { showLanguageDialog = false },
        )
    }

    error?.let { msg ->
        HitPopup(
            title = stringResource(R.string.common_something_wrong),
            message = msg,
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearError,
            onDismissRequest = viewModel::clearError,
        )
    }
}

private fun openAppLanguageSettings(context: android.content.Context) {
    val pkg = context.packageName
    // Android 13+: ajustes de idioma por app; si no, ficha de la app.
    val locale = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", pkg, null))
    val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
    runCatching { context.startActivity(locale) }.recoverCatching { context.startActivity(details) }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gray100)) {
        content()
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(Gray200))
}

@Composable
private fun SettingsRow(icon: ImageVector, iconBg: Color, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(16.dp))
        Text(label, style = HitbosssType.bodyLargeRegular, color = Gray800, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Gray500)
    }
}
