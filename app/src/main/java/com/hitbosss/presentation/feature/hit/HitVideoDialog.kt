package com.hitbosss.presentation.feature.hit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.feature.ranking.VideoPlayer
import com.hitbosss.presentation.feature.ranking.levelStyle

/** Datos mínimos para mostrar el vídeo de un HIT con su tarjeta (perfil/grupo/evento). */
data class HitVideoData(
    val videoUrl: String,
    val seekSeconds: Double,
    val exerciseTitle: String,
    val dateText: String,
    val weightText: String,
    val levelWeight: String?,
    val rankText: String,
    val hitId: Int? = null,
)

/**
 * Visor a pantalla completa de los HITs (equivale al overlay con TabView de iOS):
 * paginador horizontal con puntos para deslizar entre los hits del contexto + tarjeta + acciones.
 */
@Composable
fun HitVideoDialog(
    hits: List<HitVideoData>,
    initialPage: Int = 0,
    actions: HitActionsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onDismiss: () -> Unit,
) {
    if (hits.isEmpty()) {
        onDismiss()
        return
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val exporting by actions.exporting.collectAsStateWithLifecycle()
    var showReport by remember { mutableStateOf(false) }
    val pager = rememberPagerState(initialPage = initialPage.coerceIn(0, hits.lastIndex), pageCount = { hits.size })
    val current = hits[pager.currentPage]

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                HorizontalPager(state = pager, modifier = Modifier.weight(1f, fill = false)) { page ->
                    HitVideoPage(
                        hit = hits[page],
                        isActive = pager.currentPage == page,
                        onShare = { actions.shareVideo(context, hits[page].videoUrl) {} },
                        onReport = { showReport = true },
                    )
                }
                // Puntos (solo si hay más de un hit)
                if (hits.size > 1) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(hits.size) { i ->
                            Box(
                                Modifier.padding(horizontal = 4.dp).size(8.dp).clip(RoundedCornerShape(4.dp))
                                    .background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.4f)),
                            )
                        }
                    }
                }
            }

            Box(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).size(36.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            if (exporting) ExportingOverlay()
        }
    }

    if (showReport) {
        ReportHitDialog(
            onConfirm = { comment ->
                current.hitId?.let { id -> actions.report(id, comment) {} }
                showReport = false
            },
            onDismiss = { showReport = false },
        )
    }
}

/** Una página del visor: vídeo 9:16 + tarjeta de datos + botones de acción. */
@Composable
private fun HitVideoPage(hit: HitVideoData, isActive: Boolean, onShare: () -> Unit, onReport: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(9f / 16f).padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp)).background(Color.Black),
    ) {
        VideoPlayer(url = hit.videoUrl, seekSeconds = hit.seekSeconds, isActive = isActive)

        // Tarjeta superior con los datos del hit
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp)
                .clip(RoundedCornerShape(12.dp)).background(Gray100.copy(alpha = 0.95f)).padding(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                levelStyle(hit.levelWeight)?.let { lvl ->
                    Text(
                        lvl.label, style = HitbosssType.bodyDefaultEmphasis, color = lvl.text,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(lvl.bg)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (hit.rankText.isNotEmpty()) {
                    Text(hit.rankText, style = HitbosssType.titleBody, color = Gray800)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(hit.exerciseTitle, style = HitbosssType.titleBody, color = Gray800)
                    if (hit.dateText.isNotEmpty()) {
                        Text(hit.dateText, style = HitbosssType.bodyDefaultRegular, color = Gray500)
                    }
                }
                Text(hit.weightText, style = HitbosssType.titleSubsection, color = Gray800)
            }
        }

        // Botones compartir / denunciar (abajo-derecha)
        HitActionButtons(onShare = onShare, onReport = onReport, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}
