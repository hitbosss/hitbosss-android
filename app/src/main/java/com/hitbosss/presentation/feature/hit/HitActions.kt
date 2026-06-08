package com.hitbosss.presentation.feature.hit

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.usecase.ReportHitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject

/** Acciones del visor de un HIT: denunciar y exportar/compartir el vídeo. */
@HiltViewModel
class HitActionsViewModel @Inject constructor(
    private val reportHit: ReportHitUseCase,
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    fun report(hitId: Int, comment: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(reportHit(hitId, comment).isSuccess)
        }
    }

    /** Descarga el vídeo a caché y abre la hoja de compartir nativa (equivale al export+share de iOS). */
    fun shareVideo(context: Context, videoUrl: String, onError: () -> Unit) {
        viewModelScope.launch {
            _exporting.value = true
            val file = withContext(Dispatchers.IO) { runCatching { downloadToCache(context, videoUrl) }.getOrNull() }
            _exporting.value = false
            if (file == null) {
                onError()
                return@launch
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir HIT").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun downloadToCache(context: Context, videoUrl: String): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val out = File(dir, "Hitbosss-Export-${System.currentTimeMillis()}.mp4")
        URL(videoUrl).openStream().use { input -> out.outputStream().use { input.copyTo(it) } }
        return out
    }
}
