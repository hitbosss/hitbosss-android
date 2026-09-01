package com.hitbosss.presentation.feature.hit

import com.hitbosss.R
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.hitbosss.data.local.SavedHitStore
import com.hitbosss.domain.model.Exercise
import com.hitbosss.domain.model.PersonalInfo
import com.hitbosss.domain.model.SavedHit
import com.hitbosss.domain.repository.UserRepository
import java.util.UUID
import com.hitbosss.domain.usecase.EditHitUseCase
import com.hitbosss.domain.usecase.GetCurrentUserUseCase
import com.hitbosss.domain.usecase.UploadHitParams
import com.hitbosss.domain.usecase.UploadHitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import okio.buffer

data class EditVideoUiState(
    val frames: List<Bitmap> = emptyList(),
    val isPreparing: Boolean = false,   // recortando/exportando (overlay "Preparando HIT…")
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val success: Boolean = false,
    val editSuccess: Boolean = false,   // edición de un HIT ya subido completada
    val error: String? = null,
    val savedLater: Boolean = false,    // cancelado → guardado en HITS guardados
    val failedSaved: Boolean = false,   // falló la subida → guardado en HITS guardados
    val downloadSuccess: Boolean = false,
    val downloadError: Boolean = false,
    val leftToUpload: Boolean = false,  // subida delegada al HitUploadManager → salir de la pantalla
)

/**
 * "Divide el vídeo en dos partes" (equivale a EditVideoViewModel de iOS): genera las miniaturas del
 * filmstrip, recorta el vídeo al rango elegido (Media3 Transformer = AVAssetExportSession) y sube con
 * progreso. El pin separa Peso/Ejercicio y se envía como performedAt relativo al recorte.
 */
@HiltViewModel
class EditVideoViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val uploadHit: UploadHitUseCase,
    private val editHitUseCase: EditHitUseCase,
    private val savedHitStore: SavedHitStore,
    private val refreshCoordinator: com.hitbosss.core.RefreshCoordinator,
    private val uploadManager: com.hitbosss.core.upload.HitUploadManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var lastPrepared: File? = null
    private var lastPerformedAt: Double = 0.0
    private var uploadRequestId: String? = null

    private val exercise: Exercise = Exercise.entries
        .firstOrNull { it.apiValue.equals(savedStateHandle.get<String>("exercise"), true) } ?: Exercise.Squat
    private val weight: Double = savedStateHandle.get<String>("weight")?.toDoubleOrNull() ?: 0.0

    val videoPath: String = savedStateHandle.get<String>("video").orEmpty()

    // Contexto: "global" | "g{id}" (grupo) | "e{id}" (evento) | "edit{hitId}@{performedAt}" (editar subido).
    private val uploadContext: String = savedStateHandle.get<String>("context") ?: "global"

    /** Modo edición de un HIT ya subido (long-press en el perfil). */
    val editMode: Boolean = uploadContext.startsWith("edit")
    private val remoteHitId: Int? =
        if (editMode) uploadContext.removePrefix("edit").substringBefore('@').toIntOrNull() else null
    /** Pin inicial (performedAt actual del HIT) para preposicionar el marcador al editar. */
    val initialPinSec: Double =
        if (editMode) uploadContext.substringAfter('@', "0").toDoubleOrNull() ?: 0.0 else 0.0

    private val ctxType: String = when {
        editMode -> "global"
        uploadContext == "global" -> "global"
        uploadContext.startsWith("g") -> "group"
        uploadContext.startsWith("e") -> "event"
        else -> "global"
    }
    private val ctxGroupId: Int? = if (ctxType == "group") uploadContext.drop(1).toIntOrNull() else null
    private val ctxEventId: Int? = if (ctxType == "event") uploadContext.drop(1).toIntOrNull() else null

    private var personalInfo: PersonalInfo? = null
    private var uploadJob: Job? = null

    private val _state = MutableStateFlow(EditVideoUiState())
    val state: StateFlow<EditVideoUiState> = _state.asStateFlow()

    init {
        getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch { userRepository.getPersonalInfo(uid).onSuccess { personalInfo = it } }
        }
        viewModelScope.launch {
            val frames = withContext(Dispatchers.IO) { generateFrames(File(videoPath), FRAME_COUNT) }
            _state.update { it.copy(frames = frames) }
        }
    }

    /**
     * startSec/endSec = recorte; pinSec = división Peso/Ejercicio (absoluta).
     * iOS #649: el vídeo se persiste como SavedHit ANTES de tocar la red y la subida corre en
     * HitUploadManager (foreground service + notificación): el usuario sale de la pantalla y
     * el progreso se ve en el banner del MainScreen / la notificación.
     */
    fun upload(startSec: Double, endSec: Double, pinSec: Double) {
        if (_state.value.isPreparing || uploadManager.state.value.inProgress) return
        val info = personalInfo
        val uid = getCurrentUser()?.uid
        if (info == null || uid == null) {
            _state.update { it.copy(error = context.getString(R.string.err_load_data)) }
            return
        }
        uploadJob = viewModelScope.launch {
            // Recorte/exportación primero (overlay "Preparando HIT…").
            _state.update { it.copy(isPreparing = true, error = null) }
            val unit = if (info.measurementSystem.equals("imperial", true)) "lbs" else "kg"
            val original = File(videoPath)
            // Recorta al rango y luego garantiza compatibilidad iOS (descarta AMR que el Transformer
            // puede transmuxar tal cual; en móvil real el audio ya es AAC y no se toca).
            val trimmed = trimVideo(original, (startSec * 1000).toLong(), (endSec * 1000).toLong())
            val toUpload = withContext(Dispatchers.IO) { ensureIosCompatible(trimmed) }
            lastPrepared = toUpload
            lastPerformedAt = (pinSec - startSec).coerceAtLeast(0.0)
            // Idempotencia: un UUID por intento lógico; los reintentos (desde HITS guardados)
            // reutilizan la clave y el backend no crea duplicados.
            val requestId = uploadRequestId ?: UUID.randomUUID().toString().also { uploadRequestId = it }
            // Persistencia durable ANTES de la red: si el proceso muere, el hit no se pierde.
            val hit = SavedHit(
                id = UUID.randomUUID().toString(),
                videoFileName = "hit_${System.currentTimeMillis()}.mp4",
                userId = uid,
                sport = exercise.sport.apiValue,
                exercise = exercise.apiValue,
                lift = weight,
                unit = unit,
                bodyWeightKg = info.bodyWeightKg,
                gender = info.gender,
                performedAt = lastPerformedAt,
                contextType = ctxType,
                groupId = ctxGroupId,
                eventId = ctxEventId,
                createdAt = System.currentTimeMillis() / 1000,
                clientRequestId = requestId,
                status = SavedHit.STATUS_UPLOADING,
            )
            withContext(Dispatchers.IO) { savedHitStore.save(hit, toUpload) }
            if (uploadManager.start(hit)) {
                // La subida sigue sola: salir a la pestaña Ranking (igual que iOS con la Live Activity).
                _state.update { it.copy(isPreparing = false, leftToUpload = true) }
            } else {
                withContext(Dispatchers.IO) { savedHitStore.setStatus(hit.id, SavedHit.STATUS_PENDING) }
                _state.update { it.copy(isPreparing = false, error = context.getString(R.string.upload_in_progress)) }
            }
        }
    }

    /**
     * Edita un HIT ya subido (1:1 con iOS): si no se recorta el vídeo, solo se envía el nuevo
     * performedAt; si se recorta, se exporta el vídeo recortado y se envía también.
     */
    fun editHit(startSec: Double, endSec: Double, pinSec: Double, durationSec: Double) {
        if (_state.value.isUploading) return
        val hitId = remoteHitId ?: run {
            _state.update { it.copy(error = context.getString(R.string.err_identify_hit)) }
            return
        }
        val performedAt = (pinSec - startSec).coerceAtLeast(0.0)
        val isTrimmed = startSec > 0.05 || endSec < durationSec - 0.05
        uploadJob = viewModelScope.launch {
            _state.update { it.copy(isPreparing = true, error = null) }
            val videoFile: File? = if (isTrimmed) {
                val trimmed = trimVideo(File(videoPath), (startSec * 1000).toLong(), (endSec * 1000).toLong())
                withContext(Dispatchers.IO) { ensureIosCompatible(trimmed) }
            } else {
                null
            }
            _state.update { it.copy(isPreparing = false, isUploading = true, progress = 0f) }
            var lastPct = -1
            editHitUseCase(
                hitId = hitId,
                performedAt = performedAt,
                videoFile = videoFile,
                onProgress = { p ->
                    val pct = (p * 100).toInt()
                    if (pct != lastPct) { lastPct = pct; _state.update { it.copy(progress = p) } }
                },
            ).onSuccess {
                // Editar un HIT afecta a ranking + perfil (y posibles grupos/eventos).
                refreshCoordinator.onHitUploaded()
                _state.update { it.copy(isUploading = false, progress = 1f, editSuccess = true) }
            }.onFailure { e ->
                _state.update { it.copy(isUploading = false, error = context.getString(R.string.err_edit_hit)) }
            }
        }
    }

    /**
     * Salir de la edición de un HIT nuevo guardándolo en "HITS guardados": recorta con el
     * recorte/pin actuales (igual que la subida) y lo deja listo para subir más tarde, en vez de
     * subirlo. No aplica al editar un HIT ya subido (ese ya existe en el servidor).
     */
    fun saveForLater(startSec: Double, endSec: Double, pinSec: Double) {
        if (editMode || _state.value.isUploading || _state.value.isPreparing) return
        viewModelScope.launch {
            _state.update { it.copy(isPreparing = true, error = null) }
            val original = File(videoPath)
            val trimmed = trimVideo(original, (startSec * 1000).toLong(), (endSec * 1000).toLong())
            val toSave = withContext(Dispatchers.IO) { ensureIosCompatible(trimmed) }
            lastPrepared = toSave
            lastPerformedAt = (pinSec - startSec).coerceAtLeast(0.0)
            saveLocally()
            _state.update { it.copy(isPreparing = false, savedLater = true) }
        }
    }

    /** Cancelar: guarda el HIT en local y muestra "HIT guardado" (igual que iOS). */
    fun cancelUpload() {
        uploadJob?.cancel()
        saveLocally()
        _state.update { it.copy(isUploading = false, isPreparing = false, progress = 0f, savedLater = true) }
    }

    /** Recarga lo afectado según el contexto del HIT subido. */
    private fun invalidateForContext() {
        when (ctxType) {
            "group" -> { ctxGroupId?.let { refreshCoordinator.invalidateGroup(it) }; refreshCoordinator.invalidateProfile() }
            "event" -> { ctxEventId?.let { refreshCoordinator.invalidateEvent(it) }; refreshCoordinator.invalidateProfile() }
            else -> refreshCoordinator.onHitUploaded()
        }
    }

    private fun saveLocally() {
        val info = personalInfo ?: return
        val uid = getCurrentUser()?.uid ?: return
        val file = lastPrepared ?: File(videoPath)
        val unit = if (info.measurementSystem.equals("imperial", true)) "lbs" else "kg"
        val hit = SavedHit(
            id = UUID.randomUUID().toString(),
            videoFileName = "hit_${System.currentTimeMillis()}.mp4",
            userId = uid,
            sport = exercise.sport.apiValue,
            exercise = exercise.apiValue,
            lift = weight,
            unit = unit,
            bodyWeightKg = info.bodyWeightKg,
            gender = info.gender,
            performedAt = lastPerformedAt,
            contextType = ctxType,
            groupId = ctxGroupId,
            eventId = ctxEventId,
            createdAt = System.currentTimeMillis() / 1000,
            clientRequestId = uploadRequestId,
        )
        runCatching { savedHitStore.save(hit, file) }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun clearDownloadSuccess() = _state.update { it.copy(downloadSuccess = false) }
    fun clearDownloadError() = _state.update { it.copy(downloadError = false) }

    /** Descarga a la galería el vídeo recortado [start, end] (icono descargar, igual que iOS). */
    fun download(startSec: Double, endSec: Double) {
        if (_state.value.isPreparing || _state.value.isUploading) return
        viewModelScope.launch {
            _state.update { it.copy(isPreparing = true) }
            val trimmed = trimVideo(File(videoPath), (startSec * 1000).toLong(), (endSec * 1000).toLong())
            val ok = withContext(Dispatchers.IO) { saveToGallery(trimmed) }
            _state.update { it.copy(isPreparing = false, downloadSuccess = ok, downloadError = !ok) }
        }
    }

    private fun saveToGallery(file: File): Boolean = runCatching {
        val name = "HitBosss_${System.currentTimeMillis()}.mp4"
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, name)
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/HitBosss")
                put(android.provider.MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            @Suppress("DEPRECATION")
            val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
            val dst = File(dir, name)
            file.copyTo(dst, overwrite = true)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(dst.absolutePath), arrayOf("video/mp4"), null)
            true
        }
    }.getOrDefault(false)

    /** Miniaturas equiespaciadas para el filmstrip (como loadVideoFrames de iOS). */
    private fun generateFrames(src: File, count: Int): List<Bitmap> {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(src.absolutePath)
            val durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs <= 0) return emptyList()
            (0 until count).mapNotNull { i ->
                val tUs = (durationMs * 1000L) * i / count
                runCatching {
                    r.getScaledFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, FRAME_W, FRAME_H)
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            runCatching { r.release() }
        }
    }

    /** Recorta [startMs, endMs] con Media3 Transformer. Devuelve el original si falla. */
    @OptIn(UnstableApi::class)
    private suspend fun trimVideo(src: File, startMs: Long, endMs: Long): File =
        suspendCancellableCoroutine { cont ->
            val out = File(src.parentFile, "trim_${src.nameWithoutExtension}.mp4")
            runCatching { out.delete() }
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(src))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build(),
                )
                .build()
            val edited = EditedMediaItem.Builder(mediaItem).build()
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(out)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        if (cont.isActive) cont.resume(src)
                    }
                })
                .build()
            runCatching { transformer.start(edited, out.absolutePath) }
                .onFailure { if (cont.isActive) cont.resume(src) }
            cont.invokeOnCancellation { runCatching { transformer.cancel() } }
        }

    /**
     * Garantiza un MP4 reproducible en iOS: deja la pista de vídeo y solo audio AAC (descarta AMR-NB del
     * emulador). Conserva la rotación. Solo se usa si el recorte falló.
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
                    mime.startsWith("audio/") -> hasIncompatible = true
                }
            }
            if (!hasIncompatible) return src

            val out = File(src.parentFile, "ios_${src.nameWithoutExtension}.mp4")
            muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val rotation = MediaMetadataRetriever().run {
                runCatching { setDataSource(src.absolutePath) }
                val rr = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                runCatching { release() }
                rr
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

    private companion object {
        const val FRAME_COUNT = 8
        const val FRAME_W = 120
        const val FRAME_H = 200
    }
}
