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

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenCalculator: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onCommunityRules: () -> Unit = {},
    onTutorials: () -> Unit = {},
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
        HitTopBar(title = "Ajustes", onBack = onBack)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1 — perfil / normas / tutorial
            SettingsCard {
                SettingsRow(Icons.Filled.Person, Primary500, "Editar perfil", onEditProfile)
                RowDivider()
                SettingsRow(Icons.AutoMirrored.Filled.MenuBook, Primary500, "Normas de la comunidad", onCommunityRules)
                RowDivider()
                SettingsRow(Icons.Filled.Videocam, Primary500, "Tutorial ejercicios", onTutorials)
            }
            // 2 — calculadora
            SettingsCard {
                SettingsRow(Icons.Filled.Calculate, Secondary500, "Calculadora de Points", onOpenCalculator)
            }
            // 3 — feedback / privacidad / soporte
            SettingsCard {
                SettingsRow(Icons.Filled.Forum, Purple400, "Feedback y comunidad") {
                    sendMail("feedback@hitbosss.com", "Feedback y comunidad")
                }
                RowDivider()
                SettingsRow(Icons.Filled.Lock, Purple400, "Privacidad y seguridad", onPrivacyPolicy)
                RowDivider()
                SettingsRow(Icons.AutoMirrored.Filled.HelpOutline, Purple400, "Soporte y ayuda") {
                    sendMail("help@hitbosss.com", "Soporte y ayuda")
                }
            }
            // 4 — idioma
            SettingsCard {
                SettingsRow(Icons.Filled.Language, Orange300, "Idioma") { showLanguageDialog = true }
            }
            // 5 — compartir
            SettingsCard {
                SettingsRow(Icons.Filled.PersonAddAlt1, Color(0xFF34A853), "Compartir la app") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Únete a nuestra comunidad en HitBoss y compite en el ranking. " +
                                "Descarga la app aquí: https://play.google.com/store/apps/details?id=com.hitbosss",
                        )
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, "Compartir la app")) }
                }
            }
            // 6 — cerrar sesión / eliminar cuenta
            SettingsCard {
                SettingsRow(Icons.AutoMirrored.Filled.Logout, Gray600, "Cerrar sesión") { showLogoutDialog = true }
                RowDivider()
                SettingsRow(
                    Icons.Filled.Delete, Color(0xFFE0352B),
                    if (deleting) "Eliminando cuenta…" else "Eliminar cuenta",
                ) { if (!deleting) showDeleteDialog = true }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("¿Quieres cerrar sesión?", style = HitbosssType.titleBody) },
            text = { Text("Si sigues con la sesión iniciada, podrás consultar el ranking más rápido.", style = HitbosssType.bodyDefaultRegular) },
            confirmButton = { TextButton(onClick = { showLogoutDialog = false; viewModel.logout() }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") } },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Seguro que quieres eliminar tu cuenta?", style = HitbosssType.titleBody) },
            text = { Text("Una vez eliminada tu cuenta, no podrás recuperarla. Quizá prefieras tomarte un descanso.", style = HitbosssType.bodyDefaultRegular) },
            confirmButton = { TextButton(onClick = { showDeleteDialog = false; viewModel.deleteAccount() }) { Text("Eliminar", color = Color(0xFFE0352B)) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Cambia el idioma", style = HitbosssType.titleBody) },
            text = { Text("Configura el idioma de HitBosss en la siguiente pantalla y sigue compitiendo sin distracciones.", style = HitbosssType.bodyDefaultRegular) },
            confirmButton = {
                TextButton(onClick = {
                    showLanguageDialog = false
                    openAppLanguageSettings(context)
                }) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Cancelar") } },
        )
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("Aceptar") } },
            title = { Text("Error inesperado", style = HitbosssType.titleBody) },
            text = { Text(msg, style = HitbosssType.bodyDefaultRegular) },
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
