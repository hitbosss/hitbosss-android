package com.hitbosss.presentation.feature.hit

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitPopup
import com.hitbosss.presentation.designsystem.theme.Blue300
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.designsystem.theme.Yellow300
import kotlinx.coroutines.delay
import java.io.File
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.ArrowBack

private fun fmt(sec: Double): String {
    val s = sec.coerceAtLeast(0.0).toInt()
    return "%02d:%02d".format(s / 60, s % 60)
}

/**
 * "Divide el vídeo en dos partes" + "Revisa tu HIT antes de subir" (fase 1, 1:1 con EditVideoView de
 * iOS sin recorte real ni miniaturas todavía): el pin separa Peso/Ejercicio (= performedAt), y al
 * subir se muestra el progreso % con el popup de éxito.
 */
@OptIn(UnstableApi::class)
@Composable
fun EditVideoScreen(
    onClose: () -> Unit,
    onUploaded: () -> Unit,
    viewModel: EditVideoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val videoPath = viewModel.videoPath

    // No navegamos al tener éxito: se muestra el popup y se vuelve al ranking al pulsar "Aceptar".

    val durationSec = remember(videoPath) {
        runCatching {
            val r = MediaMetadataRetriever()
            r.setDataSource(videoPath)
            val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            r.release()
            d / 1000.0
        }.getOrDefault(0.0)
    }

    val player = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }

    var reviewMode by remember { mutableStateOf(false) }
    // Confirmación al salir de la edición (se perdería el recorte/edición del HIT).
    var showExitConfirm by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = !reviewMode) { showExitConfirm = true }
    // Recorte [start, end] + pin (división Peso/Ejercicio), en segundos.
    var startSec by remember(durationSec) { mutableStateOf(0.0) }
    var endSec by remember(durationSec) { mutableStateOf(durationSec) }
    // Al editar un HIT ya subido, el pin arranca en su performedAt actual (y ya cuenta como "movido").
    var pinSec by remember(durationSec) {
        mutableStateOf(if (viewModel.editMode) viewModel.initialPinSec.coerceIn(0.0, durationSec) else durationSec / 2)
    }
    var pinMoved by remember { mutableStateOf(viewModel.editMode) }

    Column(Modifier.fillMaxSize().background(Gray800)) {
        // Cabecera: atrás/(Vista previa) + descargar (la descarga llega en una fase posterior).
        Row(
            Modifier.fillMaxWidth().background(Gray100).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (reviewMode) {
                Text(stringResource(R.string.hit_preview), style = HitbosssType.titleSubsection, color = Gray800, modifier = Modifier.weight(1f))
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Gray800,
                    modifier = Modifier.size(24.dp).clickable { showExitConfirm = true },
                )
                Spacer(Modifier.weight(1f))
            }
            Icon(
                Icons.Filled.FileDownload, contentDescription = stringResource(R.string.hit_save_gallery), tint = Gray800,
                modifier = Modifier.size(24.dp).clickable { viewModel.download(startSec, endSec) },
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray300))

        // Reproductor.
        Box(
            Modifier.fillMaxWidth().background(Gray800).padding(vertical = 24.dp, horizontal = if (reviewMode) 74.dp else 109.dp),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(14.dp)),
            )
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(Gray100.copy(alpha = 0.5f))
                    .clickable { if (isPlaying) player.pause() else player.play() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.hit_pause) else stringResource(R.string.hit_play),
                    tint = Gray600, modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (reviewMode) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Gray100)
                    .padding(horizontal = 16.dp).padding(top = 24.dp),
            ) {
                Text(stringResource(R.string.hit_review_title), style = HitbosssType.titleSubsection, color = Gray800)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.hit_review_desc),
                    style = HitbosssType.bodySmallRegular, color = Gray500,
                )
                Spacer(Modifier.height(16.dp))
                HitButton(stringResource(R.string.common_back), onClick = { reviewMode = false }, type = HitButtonType.Tertiary)
                Spacer(Modifier.height(12.dp))
                HitButton(
                    if (viewModel.editMode) stringResource(R.string.edit_save) else stringResource(R.string.hit_upload),
                    onClick = {
                        if (viewModel.editMode) viewModel.editHit(startSec, endSec, pinSec, durationSec)
                        else viewModel.upload(startSec, endSec, pinSec)
                    },
                    type = HitButtonType.Primary,
                )
                Spacer(Modifier.height(40.dp))
            }
        } else {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Gray100)
                    .padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 40.dp),
            ) {
                Text(stringResource(R.string.hit_split_title), style = HitbosssType.titleSection, color = Gray800)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.hit_split_desc),
                    style = HitbosssType.bodySmallRegular, color = Gray500,
                )
                Spacer(Modifier.height(16.dp))
                PartRow(R.drawable.im_ico_weight, stringResource(R.string.hit_weight_label), "${fmt(startSec)} - ${fmt(pinSec)}")
                PartRow(R.drawable.im_ico_exercise, stringResource(R.string.hit_exercise_label), "${fmt(pinSec)} - ${fmt(endSec)}")
                Spacer(Modifier.height(16.dp))

                // Filmstrip con miniaturas reales + handles de recorte + pin (ThumbnailsSliderView de iOS).
                ThumbnailsSlider(
                    frames = state.frames,
                    durationSec = durationSec,
                    startSec = startSec,
                    endSec = endSec,
                    pinSec = pinSec,
                    onStart = { startSec = it.coerceIn(0.0, pinSec) },
                    onEnd = { endSec = it.coerceIn(pinSec, durationSec) },
                    onPin = {
                        pinSec = it.coerceIn(startSec, endSec)
                        pinMoved = true
                        player.seekTo((pinSec * 1000).toLong())
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text("${fmt(pinSec)} / ${fmt(endSec)}", style = HitbosssType.bodySmallRegular, color = Gray500)
                Spacer(Modifier.height(20.dp))
                HitButton(
                    stringResource(R.string.common_continue),
                    onClick = {
                        reviewMode = true
                        player.seekTo((pinSec * 1000).toLong())
                        player.pause()
                    },
                    type = HitButtonType.Secondary,
                    enabled = pinMoved,
                )
            }
        }
    }

    // Overlay "Preparando HIT…" mientras se recorta/exporta (igual que isDownloading de iOS).
    if (state.isPreparing) {
        Box(
            Modifier.fillMaxSize().background(Secondary800.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.CircularProgressIndicator(color = Gray100)
                Text(stringResource(R.string.hit_preparing), style = HitbosssType.bodySmallRegular, color = Gray100)
            }
        }
    }

    // Overlay "Subiendo HIT" con progreso %.
    if (state.isUploading) {
        UploadingOverlay(progress = state.progress, onCancel = viewModel::cancelUpload)
    }

    if (state.downloadSuccess) {
        HitPopup(
            title = stringResource(R.string.hit_downloaded_title),
            message = stringResource(R.string.hit_downloaded_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearDownloadSuccess,
            onDismissRequest = viewModel::clearDownloadSuccess,
        )
    }

    if (state.downloadError) {
        HitPopup(
            title = stringResource(R.string.hit_perms_save_title),
            message = stringResource(R.string.hit_perms_save_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearDownloadError,
            onDismissRequest = viewModel::clearDownloadError,
        )
    }

    if (state.editSuccess) {
        HitPopup(
            title = stringResource(R.string.hit_updated_title),
            message = stringResource(R.string.hit_updated_msg),
            icon = painterResource(R.drawable.im_icon_success),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = onUploaded,
            onDismissRequest = onUploaded,
        )
    }

    if (state.success) {
        HitPopup(
            title = stringResource(R.string.hit_success_title),
            message = stringResource(R.string.hit_success_msg),
            icon = painterResource(R.drawable.im_icon_success),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = onUploaded,
            onDismissRequest = onUploaded,
        )
    }

    // Cancelado → guardado en HITS guardados.
    if (state.savedLater) {
        HitPopup(
            title = stringResource(R.string.hit_saved_title),
            message = stringResource(R.string.hit_saved_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = onUploaded,
            onDismissRequest = onUploaded,
        )
    }

    // Falló la subida → guardado en HITS guardados.
    if (state.failedSaved) {
        HitPopup(
            title = stringResource(R.string.hit_failed_title),
            message = stringResource(R.string.hit_failed_msg),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = onUploaded,
            onDismissRequest = onUploaded,
        )
    }

    state.error?.let { error ->
        HitPopup(
            title = "Algo salió mal",
            message = error,
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearError,
            onDismissRequest = viewModel::clearError,
        )
    }

    if (showExitConfirm) {
        // HIT nuevo: al salir se guarda en "HITS guardados" (recortado, listo para subir).
        // Editar un HIT ya subido: solo se descartan los cambios.
        val isEdit = viewModel.editMode
        HitPopup(
            title = stringResource(R.string.hit_exit_edit_title),
            message = stringResource(if (isEdit) R.string.hit_exit_edit_msg else R.string.hit_exit_edit_msg_save),
            confirmText = stringResource(R.string.common_exit),
            confirmType = if (isEdit) com.hitbosss.presentation.designsystem.components.HitButtonType.Destructive
            else com.hitbosss.presentation.designsystem.components.HitButtonType.Secondary,
            onConfirm = {
                showExitConfirm = false
                if (isEdit) onClose() else viewModel.saveForLater(startSec, endSec, pinSec)
            },
            cancelText = stringResource(R.string.common_cancel),
            onCancel = { showExitConfirm = false },
            onDismissRequest = { showExitConfirm = false },
        )
    }
}

@Composable
private fun PartRow(icon: Int, label: String, range: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Text(label, style = HitbosssType.bodySmallEmphasis, color = Gray800)
        Text(range, style = HitbosssType.bodySmallRegular, color = Gray800)
    }
}

@Composable
private fun UploadingOverlay(progress: Float, onCancel: () -> Unit) {
    // El % refleja los bytes enviados; se mapea a 0–90% para reservar el final al procesado del
    // servidor (S3 + crear el hit), así no se queda "clavado" en 100%. Animado 0.2s como iOS.
    val shown by animateFloatAsState(
        targetValue = (progress * 0.9f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "uploadProgress",
    )
    Box(
        Modifier.fillMaxSize().background(Secondary800.copy(alpha = 0.9f)).padding(horizontal = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gray100),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.hit_uploading), style = HitbosssType.titleSubsection, color = Gray800,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            UploadingRotatingText()
            Spacer(Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(120.dp)) {
                    val s = 8.dp.toPx()
                    val inset = s / 2
                    val arcSize = Size(size.width - s, size.height - s)
                    drawArc(Gray400, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(s))
                    drawArc(
                        Primary500, -90f, 360f * shown, false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(s, cap = StrokeCap.Round),
                    )
                }
                Text("${(shown * 100).toInt()}%", style = HitbosssType.titleSection, color = Gray800)
            }
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp))
                    .background(Gray800).clickable { onCancel() }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.common_cancel), style = HitbosssType.bodyDefaultRegular, color = Gray100)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UploadingRotatingText() {
    val texts = listOf(
        stringResource(R.string.hit_rot1),
        stringResource(R.string.hit_rot2),
        stringResource(R.string.hit_rot3),
    )
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2500); i = (i + 1) % texts.size } }
    Text(
        texts[i], style = HitbosssType.bodySmallRegular, color = Gray500,
        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/**
 * Filmstrip de miniaturas con recorte (2 handles) + pin de división (ThumbnailsSliderView de iOS):
 * zona Peso (amarilla, start→pin) y zona Ejercicio (azul, pin→end); fuera del recorte se atenúa.
 */
@Composable
private fun ThumbnailsSlider(
    frames: List<Bitmap>,
    durationSec: Double,
    startSec: Double,
    endSec: Double,
    pinSec: Double,
    onStart: (Double) -> Unit,
    onEnd: (Double) -> Unit,
    onPin: (Double) -> Unit,
) {
    val dur = durationSec.coerceAtLeast(0.001)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val handleWpx = with(density) { 14.dp.toPx() }
    val curStart by rememberUpdatedState(startSec)
    val curEnd by rememberUpdatedState(endSec)
    val curPin by rememberUpdatedState(pinSec)

    Box(
        Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(8.dp))
            .onSizeChanged { widthPx = it.width.toFloat() },
    ) {
        // Miniaturas (o gris mientras cargan).
        if (frames.isNotEmpty()) {
            Row(Modifier.fillMaxSize()) {
                frames.forEach { bmp ->
                    Image(bmp.asImageBitmap(), null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                }
            }
        } else {
            Box(Modifier.fillMaxSize().background(Gray300))
        }

        val startX = (startSec / dur * widthPx).toFloat()
        val endX = (endSec / dur * widthPx).toFloat()
        val pinX = (pinSec / dur * widthPx).toFloat().coerceIn(startX + handleWpx, (endX - handleWpx).coerceAtLeast(startX + handleWpx))

        // Atenuado fuera del recorte + zonas Peso (amarillo) / Ejercicio (azul).
        Canvas(Modifier.fillMaxSize()) {
            val h = size.height
            drawRect(Gray600.copy(alpha = 0.75f), size = Size(startX, h))
            drawRect(Gray600.copy(alpha = 0.75f), topLeft = Offset(endX, 0f), size = Size((size.width - endX).coerceAtLeast(0f), h))
            drawRect(Yellow300.copy(alpha = 0.2f), topLeft = Offset(startX, 0f), size = Size((pinX - startX).coerceAtLeast(0f), h))
            drawRect(Yellow300, topLeft = Offset(startX, 0f), size = Size((pinX - startX).coerceAtLeast(0f), h), style = Stroke(3.dp.toPx()))
            drawRect(Blue300.copy(alpha = 0.2f), topLeft = Offset(pinX, 0f), size = Size((endX - pinX).coerceAtLeast(0f), h))
            drawRect(Blue300, topLeft = Offset(pinX, 0f), size = Size((endX - pinX).coerceAtLeast(0f), h), style = Stroke(3.dp.toPx()))
        }

        // Handle izquierdo (inicio del recorte).
        Box(
            Modifier.offset { IntOffset(startX.roundToInt(), 0) }.width(14.dp).fillMaxHeight()
                .background(Gray800)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dx -> onStart(curStart + dx / widthPx * dur) }
                },
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.width(2.dp).height(12.dp).background(Gray100)) }

        // Handle derecho (fin del recorte).
        Box(
            Modifier.offset { IntOffset((endX - handleWpx).roundToInt(), 0) }.width(14.dp).fillMaxHeight()
                .background(Gray800)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dx -> onEnd(curEnd + dx / widthPx * dur) }
                },
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.width(2.dp).height(12.dp).background(Gray100)) }

        // Pin (división Peso/Ejercicio).
        Box(
            Modifier.offset { IntOffset((pinX - with(density) { 9.dp.toPx() }).roundToInt(), 0) }
                .width(18.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Gray100)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dx -> onPin(curPin + dx / widthPx * dur) }
                },
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.width(2.dp).height(12.dp).clip(RoundedCornerShape(8.dp)).background(Gray800)) }
    }
}
