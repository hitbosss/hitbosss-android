package com.hitbosss.presentation.feature.community

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.Member
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray700
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary100

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupDetailScreen(onBack: () -> Unit, viewModel: GroupDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLeave by remember { mutableStateOf(false) }
    var showAdminBlock by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }

    LaunchedEffect(state.left, state.deleted) { if (state.left || state.deleted) onBack() }

    val g = state.group

    Column(Modifier.fillMaxSize().background(Gray100)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Gray800, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            Text("Información del grupo", style = HitbosssType.titleSubsection, color = Gray800, modifier = Modifier.weight(1f))
            Box {
                Icon(Icons.Filled.MoreVert, "Opciones", tint = Gray800, modifier = Modifier.size(24.dp).clickable { showMenu = true })
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Compartir grupo") }, onClick = {
                        showMenu = false
                        g?.let { gr ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Únete al grupo \"${gr.name}\" en HitBoss.")
                            }
                            runCatching { context.startActivity(Intent.createChooser(intent, "Compartir grupo")) }
                        }
                    })
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray200))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary500) }
            g == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(state.error ?: "No se pudo cargar", style = HitbosssType.bodyDefaultRegular, color = Gray500)
            }
            else -> {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    // Header de grupo: cuadrado 90 con icono (SquareAsyncImage), no banner (igual que iOS).
                    Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = g.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                            placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                            modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)).background(Gray200),
                        )
                    }
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Section("Nombre del grupo") { Value(g.name) }
                        g.motto?.takeIf { it.isNotBlank() }?.let { Section("Lema") { Value(it) } }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Miembros", style = HitbosssType.titleBody, color = Gray800, modifier = Modifier.weight(1f))
                                Text("ver listado", style = HitbosssType.bodySmallEmphasis, color = Primary500, modifier = Modifier.clickable { showMembers = true })
                            }
                            Value("${g.stats.memberCount}")
                        }
                        Section("Descripción") { Value(g.description.orEmpty()) }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ejercicio", style = HitbosssType.titleBody, color = Gray800)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                g.exercises.forEach { ex ->
                                    Text(
                                        exerciseTitle(ex), style = HitbosssType.bodyDefaultRegular, color = Gray800,
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Secondary100).padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Salir: bloquea solo si es el único admin con otros miembros (igual que iOS).
                    // Si es admin, estilo secundario (oscuro); si no, destructivo (rojo).
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (state.isAdmin) Gray700 else Error500)
                            .clickable { if (state.canLeaveDirectly) showLeave = true else showAdminBlock = true }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Dejar el grupo", style = HitbosssType.bodyLargeEmphasis, color = Gray100) }
                    if (state.isAdmin) {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Error500)
                                .clickable { showDelete = true }.padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Eliminar grupo", style = HitbosssType.bodyLargeEmphasis, color = Gray100) }
                    }
                }
            }
        }
    }

    if (showLeave) {
        AlertDialog(
            onDismissRequest = { showLeave = false },
            title = { Text("¿Seguro que quieres dejar el grupo?", style = HitbosssType.titleBody) },
            text = { Text("Si dejas el grupo, no podrás deshacerlo.", style = HitbosssType.bodyDefaultRegular) },
            confirmButton = { TextButton(onClick = { showLeave = false; viewModel.leave() }) { Text("Salir del grupo", color = Error500) } },
            dismissButton = { TextButton(onClick = { showLeave = false }) { Text("Cancelar") } },
        )
    }
    if (showAdminBlock) {
        AlertDialog(
            onDismissRequest = { showAdminBlock = false },
            title = { Text("Antes de irte…", style = HitbosssType.titleBody) },
            text = { Text("Eres el administrador. Si quieres salir del grupo, primero debes pasar el rol de administrador a otro miembro.", style = HitbosssType.bodyDefaultRegular) },
            confirmButton = { TextButton(onClick = { showAdminBlock = false }) { Text("Aceptar") } },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("¿Seguro que quieres eliminar este grupo?", style = HitbosssType.titleBody) },
            text = { Text("Eliminar este grupo es una acción permanente: los miembros perderán el acceso y no podrás deshacerlo.", style = HitbosssType.bodyDefaultRegular) },
            confirmButton = { TextButton(onClick = { showDelete = false; viewModel.delete() }) { Text("Eliminar grupo", color = Error500) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } },
        )
    }
    if (showMembers) MembersDialog(g?.members.orEmpty()) { showMembers = false }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = HitbosssType.titleBody, color = Gray800)
        content()
    }
}

@Composable
private fun Value(text: String) = Text(text, style = HitbosssType.bodySmallRegular, color = Gray500)

@Composable
private fun MembersDialog(members: List<Member>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Miembros del grupo", style = HitbosssType.titleBody) },
        text = {
            LazyColumn {
                items(members) { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = m.profilePic, contentDescription = null, contentScale = ContentScale.Crop, placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(), modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Gray200))
                        Text(m.username, style = HitbosssType.bodyDefaultRegular, color = Gray800, modifier = Modifier.weight(1f))
                        if (m.isAdmin) Text("Admin", style = HitbosssType.bodySmallEmphasis, color = Primary500)
                    }
                }
            }
        },
    )
}

private fun exerciseTitle(apiKey: String): String =
    Exercise.entries.firstOrNull { it.apiValue.equals(apiKey, true) }?.title ?: apiKey.replaceFirstChar { it.uppercase() }
