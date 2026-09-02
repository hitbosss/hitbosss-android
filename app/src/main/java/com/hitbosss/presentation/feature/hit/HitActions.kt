package com.hitbosss.presentation.feature.hit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import kotlin.coroutines.resume
import com.hitbosss.domain.repository.HitRepository

/** Acciones del visor de un HIT: denunciar y exportar/compartir el vídeo con marca (como iOS). */
@HiltViewModel
class HitActionsViewModel @Inject constructor(
    private val hitRepository: HitRepository,
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    fun report(hitId: Int, comment: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(hitRepository.reportHit(hitId, comment).isSuccess)
        }
    }

    /**
     * Exporta el vídeo quemando la tarjeta de datos + outro de marca (1:1 con ExerciseVideoExporter
     * de iOS) y abre la hoja de compartir nativa (Instagram/Reels, guardar, etc.).
     */
    fun shareVideo(context: Context, hit: HitVideoData, onError: () -> Unit) {
        viewModelScope.launch {
            _exporting.value = true
            val source = withContext(Dispatchers.IO) { runCatching { downloadToCache(context, hit.videoUrl) }.getOrNull() }
            if (source == null) {
                _exporting.value = false
                onError()
                return@launch
            }
            val exported = runCatching { exportBranded(context, source, hit) }.getOrNull()
            _exporting.value = false
            val toShare = exported ?: source
            shareFile(context, toShare)
        }
    }

    /**
     * Composición Media3: [vídeo + overlay header] -> [outro 3s con zoom lento + glitch] a 720x1280.
     * Si el export con efectos falla (p. ej. el shader del glitch), reintenta sin ellos antes de
     * rendirse (el llamador comparte entonces el vídeo crudo).
     */
    @OptIn(UnstableApi::class)
    private suspend fun exportBranded(context: Context, source: File, hit: HitVideoData): File? {
        val outW = 720
        val outH = 1280

        val headerBmp = HitVideoBranding.renderHeaderOverlay(context, hit, outW, outH)
        val outroBmp = HitVideoBranding.renderOutro(context, outW, outH)
        val outroFile = File(context.cacheDir, "shared/outro_${System.currentTimeMillis()}.png").apply { parentFile?.mkdirs() }
        outroFile.outputStream().use { outroBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val out = File(context.cacheDir, "shared/Hitbosss-Export-${System.currentTimeMillis()}.mp4")

        // El HIT compartido empieza donde empieza el ejercicio (el pin performedAt), no en el segundo 0.
        // Se acota por debajo de la duración para no generar un clip vacío (pin cerca del final o dato malo),
        // que haría fallar el export y acabar compartiendo el vídeo crudo sin branding.
        val startMs = run {
            val retriever = android.media.MediaMetadataRetriever()
            val durationMs = try {
                retriever.setDataSource(source.absolutePath)
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: Long.MAX_VALUE
            } catch (_: Exception) { Long.MAX_VALUE } finally { retriever.release() }
            (hit.seekSeconds * 1000).toLong().coerceIn(0L, (durationMs - 500L).coerceAtLeast(0L))
        }

        fun composition(fancyOutro: Boolean): Composition {
            val overlay: androidx.media3.effect.TextureOverlay = BitmapOverlay.createStaticBitmapOverlay(headerBmp)
            val clippedSource = MediaItem.fromUri(Uri.fromFile(source)).buildUpon()
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startMs).build(),
                )
                .build()
            val videoItem = EditedMediaItem.Builder(clippedSource)
                .setEffects(Effects(emptyList(), listOf(scaleTo(outW, outH), OverlayEffect(ImmutableList.of(overlay)))))
                .build()

            val outroMedia = MediaItem.Builder()
                .setUri(Uri.fromFile(outroFile))
                .setMimeType(androidx.media3.common.MimeTypes.IMAGE_PNG)
                .build()
            val outroEffects: List<androidx.media3.common.Effect> =
                if (fancyOutro) listOf(scaleTo(outW, outH), slowZoom(), GlitchEffect())
                else listOf(scaleTo(outW, outH))
            val outroItem = EditedMediaItem.Builder(outroMedia)
                .setDurationUs(3_000_000L)
                .setFrameRate(30)
                .setEffects(Effects(emptyList(), outroEffects))
                .build()

            val sequence = EditedMediaItemSequence(listOf(videoItem, outroItem))
            return Composition.Builder(ImmutableList.of(sequence)).build()
        }

        if (runTransform(context, composition(fancyOutro = true), out)) return out
        runCatching { out.delete() }
        if (runTransform(context, composition(fancyOutro = false), out)) return out
        return null
    }

    @OptIn(UnstableApi::class)
    private fun scaleTo(w: Int, h: Int): Presentation =
        Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT)

    @OptIn(UnstableApi::class)
    private suspend fun runTransform(context: Context, composition: Composition, out: File): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(true)
                        }

                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            if (cont.isActive) cont.resume(false)
                        }
                    })
                    .build()
                runCatching { transformer.start(composition, out.absolutePath) }
                    .onFailure { if (cont.isActive) cont.resume(false) }
                cont.invokeOnCancellation { runCatching { transformer.cancel() } }
            }
        }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir HIT").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun downloadToCache(context: Context, videoUrl: String): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val out = File(dir, "Hitbosss-Source-${System.currentTimeMillis()}.mp4")
        URL(videoUrl).openStream().use { input -> out.outputStream().use { input.copyTo(it) } }
        return out
    }
}
