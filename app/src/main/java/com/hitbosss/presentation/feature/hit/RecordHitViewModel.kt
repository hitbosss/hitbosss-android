package com.hitbosss.presentation.feature.hit

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.GetPersonalInfoUseCase
import com.hitbosss.domain.usecase.UploadHitParams
import com.hitbosss.domain.usecase.UploadHitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import com.hitbosss.R
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import okio.buffer

data class RecordHitUiState(
    val isUploading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RecordHitViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getPersonalInfo: GetPersonalInfoUseCase,
    private val uploadHit: UploadHitUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val exercise: Exercise = Exercise.entries
        .firstOrNull { it.apiValue.equals(savedStateHandle.get<String>("exercise"), true) } ?: Exercise.Squat
    private val weight: Double = savedStateHandle.get<String>("weight")?.toDoubleOrNull() ?: 0.0

    private var personalInfo: PersonalInfo? = null
    private var pendingFile: File? = null

    private val _state = MutableStateFlow(RecordHitUiState())
    val state: StateFlow<RecordHitUiState> = _state.asStateFlow()

    init {
        val uid = getCurrentUser()?.uid
        if (uid == null) {
            _state.update { it.copy(error = context.getString(R.string.err_identify_user)) }
        } else {
            viewModelScope.launch {
                getPersonalInfo(uid)
                    .onSuccess { personalInfo = it; pendingFile?.let(::doUpload) }
                    .onFailure { e -> if (pendingFile != null) _state.update { it.copy(isUploading = false, error = e.message ?: context.getString(R.string.err_load_data)) } }
            }
        }
    }

    /** El vídeo ya grabado: sube ya si hay datos, o queda pendiente hasta que carguen. */
    fun upload(videoFile: File) {
        if (_state.value.isUploading || _state.value.success) return
        pendingFile = videoFile
        if (personalInfo != null) doUpload(videoFile) else _state.update { it.copy(isUploading = true, error = null) }
    }

    private fun doUpload(videoFile: File) {
        val info = personalInfo ?: return
        val uid = getCurrentUser()?.uid ?: return
        pendingFile = null
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, error = null) }
            val unit = if (info.measurementSystem.equals("imperial", true)) "lbs" else "kg"
            // iOS no reproduce audio AMR (lo que genera el emulador): remuxamos a pistas compatibles.
            val compatible = withContext(Dispatchers.IO) { ensureIosCompatible(videoFile) }
            uploadHit(
                UploadHitParams(
                    userId = uid,
                    sport = exercise.sport.apiValue,
                    exercise = exercise.apiValue,
                    lift = weight,
                    unit = unit,
                    bodyWeightKg = info.bodyWeightKg,
                    gender = info.gender,
                    videoFile = compatible,
                    performedAt = videoMidpointSeconds(compatible),
                ),
            ).onSuccess { _state.update { it.copy(isUploading = false, success = true) } }
                .onFailure { e -> _state.update { it.copy(isUploading = false, error = e.message ?: context.getString(R.string.err_upload_hit)) } }
        }
    }

    /**
     * Garantiza un MP4 reproducible en iOS: deja la pista de vídeo y solo audio AAC, descartando
     * códecs que iOS no soporta (p. ej. AMR-NB que produce el emulador). Si ya es compatible, no toca.
     */
    private fun ensureIosCompatible(src: File): File {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply { setDataSource(src.absolutePath) }
            val keep = mutableListOf<Int>()
            var hasIncompatible = false
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
                when {
                    mime.startsWith("video/") -> keep.add(i)
                    mime == MediaFormat.MIMETYPE_AUDIO_AAC -> keep.add(i)
                    mime.startsWith("audio/") -> hasIncompatible = true // AMR u otros: descartar
                }
            }
            if (!hasIncompatible) return src // ya es compatible (vídeo + AAC o sin audio)

            val out = File(src.parentFile, "ios_${src.nameWithoutExtension}.mp4")
            muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            // Conserva la orientación del vídeo: el remux NO copia la metadata de rotación, hay que
            // reaplicarla con setOrientationHint o el vídeo sale girado.
            val rotation = MediaMetadataRetriever().run {
                runCatching { setDataSource(src.absolutePath) }
                val r = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                runCatching { release() }
                r
            }
            if (rotation != 0) muxer.setOrientationHint(rotation)
            val indexMap = HashMap<Int, Int>()
            for (i in keep) {
                val format = extractor.getTrackFormat(i)
                indexMap[i] = muxer.addTrack(format)
                extractor.selectTrack(i)
            }
            muxer.start()
            val buffer = java.nio.ByteBuffer.allocate(1 shl 20)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val track = extractor.sampleTrackIndex
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                indexMap[track]?.let { muxer.writeSampleData(it, buffer, info) }
                extractor.advance()
            }
            muxer.stop()
            return out
        } catch (e: Exception) {
            return src
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
        }
    }

    /** Punto medio del vídeo (s) para el frame de preview, como el pinnedTime de iOS. */
    private fun videoMidpointSeconds(file: File): Double {
        return runCatching {
            val r = MediaMetadataRetriever()
            r.setDataSource(file.absolutePath)
            val durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            r.release()
            (durationMs / 2.0) / 1000.0
        }.getOrDefault(0.0)
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
