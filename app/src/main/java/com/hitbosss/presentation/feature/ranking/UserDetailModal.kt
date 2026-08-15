package com.hitbosss.presentation.feature.ranking

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.hitbosss.presentation.designsystem.components.placeholderPainter
import com.hitbosss.R
import com.hitbosss.domain.model.RankingCategory
import com.hitbosss.domain.model.RankingEntry
import com.hitbosss.domain.model.SportRanking
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import androidx.compose.foundation.border
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import okio.buffer

/**
 * Modal con los hits del usuario (paginador) — equivale a UserDetailModal de iOS, que se presenta
 * como `.sheet([.large]).presentationDragIndicator(.visible)`: hoja inferior que sube desde abajo,
 * no llega al tope (deja hueco arriba) y se baja para cerrar.
 */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailModal(userId: String, ranking: SportRanking, onDismiss: () -> Unit, onVisitProfile: () -> Unit = {}) {
    val hits: List<Pair<RankingCategory, RankingEntry>> = remember(userId, ranking) {
        ranking.byCategory.entries
            .filter { it.key != RankingCategory.PlOfficial && it.key != RankingCategory.CfOfficial }
            .mapNotNull { (cat, list) -> list.firstOrNull { it.userId == userId }?.let { cat to it } }
    }
    if (hits.isEmpty()) {
        onDismiss()
        return
    }
    val header = hits.first().second

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Gray100,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        // fillMaxHeight para que la hoja ocupe el detent grande y el paginador tenga altura.
        Column(Modifier.fillMaxWidth().fillMaxHeight().navigationBarsPadding()) {
            // Cabecera: foto + nombre + bandera + "Visitar perfil"
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = header.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    placeholder = placeholderPainter(), error = placeholderPainter(), fallback = placeholderPainter(),
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(Gray200),
                )
                Column(Modifier.weight(1f)) {
                    Text(header.username, style = HitbosssType.titleBody, color = Gray800)
                    Text("${countryFlag(header.countryCode)} ${header.countryCode ?: ""}", style = HitbosssType.bodyDefaultRegular, color = Gray500)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(com.hitbosss.presentation.designsystem.theme.Secondary800)
                        .clickable { onVisitProfile() }.padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(R.string.user_view_profile), style = HitbosssType.bodyDefaultEmphasis, color = Gray100)
                }
            }

            val pager = rememberPagerState(pageCount = { hits.size })
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                HitPage(hits[page].first, hits[page].second, isActive = pager.currentPage == page)
            }

            // Dots
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(hits.size) { i ->
                    Box(
                        Modifier.padding(horizontal = 4.dp).size(8.dp).clip(CircleShape)
                            .background(if (i == pager.currentPage) Gray800 else Gray400),
                    )
                }
            }
        }
    }
}

/**
 * Página de un hit con su vídeo. Replica ExercisePageView de iOS:
 * caja 9:16 redondeada, seek a `performedAt`, placeholder + spinner con crossfade,
 * reproduce solo cuando es la página activa, sin loop (igual que AVPlayer).
 */
@OptIn(UnstableApi::class)
@Composable
private fun HitPage(
    category: RankingCategory,
    entry: RankingEntry,
    isActive: Boolean,
    actions: com.hitbosss.presentation.feature.hit.HitActionsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val exporting by actions.exporting.collectAsStateWithLifecycle()
    var showReport by remember { mutableStateOf(false) }
    // null = sin resultado; true = enviada; false = error. Popup de confirmación tras denunciar.
    var reportResult by remember { mutableStateOf<Boolean?>(null) }

    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(8.dp)).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            entry.videoUrl?.takeIf { it.isNotBlank() }?.let { url ->
                VideoPlayer(url = url, seekSeconds = entry.performedAt, isActive = isActive)
            }

            // Cabecera del hit superpuesta (HitVideoHeaderView de iOS): textos pequeños y repartidos.
            Column(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Gray100.copy(alpha = 0.8f))
                    .border(1.dp, Gray300, RoundedCornerShape(8.dp)),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    PositionBadge(entry.rank)
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    levelStyle(entry.levelWeight)?.let { lvl ->
                        Text(
                            stringResource(lvl.labelRes).uppercase(), style = HitbosssType.bodyDefaultEmphasis, color = lvl.text,
                            modifier = Modifier.padding(end = 8.dp, top = 4.dp).clip(RoundedCornerShape(6.dp)).background(lvl.bg).padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                // Fila ejercicio | peso (bodyDefaultEmphasis, igual que iOS)
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(category.title, style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
                    entry.lift?.let { Text("${it.value.toInt()} ${it.unit.uppercase()}", style = HitbosssType.bodyDefaultEmphasis, color = Gray800) }
                }
                // Fila fecha | points (bodySmallRegular)
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatHitDate(entry.createdAt), style = HitbosssType.bodySmallRegular, color = Gray800, modifier = Modifier.weight(1f))
                    Text("${formatPointsModal(entry.score)} POINTS", style = HitbosssType.bodySmallRegular, color = Gray500)
                }
            }

            // Botones compartir / denunciar (abajo-derecha)
            entry.videoUrl?.takeIf { it.isNotBlank() }?.let { url ->
                com.hitbosss.presentation.feature.hit.HitActionButtons(
                    onShare = {
                        actions.shareVideo(
                            context,
                            com.hitbosss.presentation.feature.hit.HitVideoData(
                                videoUrl = url,
                                seekSeconds = entry.performedAt,
                                exerciseTitle = category.title,
                                dateText = formatHitDate(entry.createdAt),
                                weightText = entry.lift?.let { "${it.value.toInt()} ${it.unit.uppercase()}" } ?: "",
                                levelWeight = entry.levelWeight,
                                rankText = "#${entry.rank}",
                                pointsText = com.hitbosss.presentation.feature.hit.formatPointsText(entry.score),
                                hitId = entry.hitId,
                            ),
                        ) {}
                    },
                    onReport = { showReport = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }

            if (exporting) com.hitbosss.presentation.feature.hit.ExportingOverlay()
        }
    }

    if (showReport) {
        com.hitbosss.presentation.feature.hit.ReportHitDialog(
            onConfirm = { comment ->
                entry.hitId?.let { id -> actions.report(id, comment) { ok -> reportResult = ok } }
                showReport = false
            },
            onDismiss = { showReport = false },
        )
    }
    when (reportResult) {
        true -> com.hitbosss.presentation.designsystem.components.HitPopup(
            title = stringResource(R.string.report_sent_title),
            message = stringResource(R.string.report_sent_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = { reportResult = null },
            onDismissRequest = { reportResult = null },
        )
        false -> com.hitbosss.presentation.designsystem.components.HitPopup(
            title = stringResource(R.string.common_unexpected_error),
            message = stringResource(R.string.common_unexpected_error_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = { reportResult = null },
            onDismissRequest = { reportResult = null },
        )
        null -> Unit
    }
}

/** ExoPlayer envuelto en PlayerView — equivalente a AVPlayer + AVPlayerViewController de iOS. */
@OptIn(UnstableApi::class)
@Composable
internal fun VideoPlayer(url: String, seekSeconds: Double, isActive: Boolean) {
    val context = LocalContext.current
    // Solo se considera "listo" cuando el PRIMER FOTOGRAMA está renderizado (no solo STATE_READY).
    // Así no se reproduce de forma invisible mientras decodifica (sobre todo HEVC de iPhone), que
    // hacía que el vídeo apareciera "ya empezado". Mirroring de iOS: seek al lift y mostrar/reproducir
    // cuando hay imagen real.
    var firstFrame by remember(url) { mutableStateOf(false) }

    val player = remember(url) {
        // Renderers con fallback de decodificador: si el decodificador (p. ej. HEVC de iPhone) falla
        // al inicializarse, intenta otro en vez de romper la reproducción ("a veces no cargan").
        val renderers = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        // MediaSource con caché en disco compartida → reapertura instantánea y arranque más rápido.
        ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(com.hitbosss.presentation.feature.hit.VideoCache.mediaSourceFactory(context))
            .build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_OFF // iOS no hace loop
            playWhenReady = false
            // Seek al instante del lift (performedAt en segundos) ANTES de preparar → el primer fotograma
            // ya sale en el lift, no al principio (mirroring de iOS). El caso "performedAt fuera del recorte"
            // se corrige abajo cuando ya se conoce la duración.
            seekTo((seekSeconds * 1000).toLong().coerceAtLeast(0L))
            prepare()
        }
    }

    // Si el lift cae al final/fuera del clip (recorte), arrancar desde el principio en vez de quedar "ya acabado".
    var fixedOvershoot by remember(url) { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && !fixedOvershoot) {
                    fixedOvershoot = true
                    val dur = player.duration
                    if (dur > 0 && player.currentPosition >= dur - 300) player.seekTo(0)
                }
            }
            // El primer fotograma decodificado y mostrado en la superficie (también en pausa tras el seek).
            override fun onRenderedFirstFrame() { firstFrame = true }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Reproduce solo cuando hay imagen y la página está activa (iOS: play() tras isPlaybackLikelyToKeepUp).
    LaunchedEffect(isActive, firstFrame) {
        player.playWhenReady = isActive && firstFrame
        if (!isActive) player.pause()
    }

    // Crossfade del vídeo cuando aparece el primer fotograma (iOS: opacity 0->1 easeIn 0.2s)
    val videoAlpha by animateFloatAsState(targetValue = if (firstFrame) 1f else 0f, animationSpec = tween(200), label = "videoAlpha")

    AndroidView(
        factory = { ctx ->
            // PlayerView con TextureView (layout XML) para que el alpha/crossfade funcione.
            (android.view.LayoutInflater.from(ctx).inflate(R.layout.view_hit_player, null) as PlayerView).apply {
                this.player = player
                // Controles ocultos durante la reproducción; aparecen al tocar el vídeo (no auto-show).
                controllerAutoShow = false
                hideController()
            }
        },
        modifier = Modifier.fillMaxSize().alpha(videoAlpha),
    )

    // Placeholder mientras carga (iOS: rect gris + ProgressView)
    if (!firstFrame) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

private fun formatHitDate(unixSeconds: Long): String =
    if (unixSeconds <= 0) "" else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(unixSeconds * 1000))

private fun formatPointsModal(points: Double): String =
    if (points % 1.0 == 0.0) points.toInt().toString() else "%.2f".format(points)
