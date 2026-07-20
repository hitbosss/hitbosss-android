package com.hitbosss.data.local

import kotlinx.serialization.encodeToString

import android.content.Context
import com.hitbosss.domain.model.SavedHit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistencia local de HITs no subidos (equivale a LocalHitsManager de iOS): metadata en un JSON y
 * el vídeo copiado a filesDir/saved_hits/. Sirve para reintentar la subida desde "HITS guardados".
 */
@Singleton
class SavedHitStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir = File(context.filesDir, "saved_hits").apply { mkdirs() }
    private val index = File(dir, "index.json")
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun getAll(): List<SavedHit> = runCatching {
        if (!index.exists()) emptyList() else json.decodeFromString<List<SavedHit>>(index.readText())
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(hit: SavedHit, sourceVideo: File) {
        runCatching { sourceVideo.copyTo(File(dir, hit.videoFileName), overwrite = true) }
        val all = getAll() + hit
        index.writeText(json.encodeToString(all))
    }

    @Synchronized
    fun delete(id: String) {
        val all = getAll()
        all.firstOrNull { it.id == id }?.let { runCatching { File(dir, it.videoFileName).delete() } }
        index.writeText(json.encodeToString(all.filterNot { it.id == id }))
    }

    @Synchronized
    fun setStatus(id: String, status: String) {
        val all = getAll().map { if (it.id == id) it.copy(status = status) else it }
        index.writeText(json.encodeToString(all))
    }

    /** Al arrancar: si el proceso murió a mitad de subida, el hit vuelve a "pendiente" (iOS #649). */
    @Synchronized
    fun reconcile() {
        val all = getAll()
        if (all.none { it.status == SavedHit.STATUS_UPLOADING }) return
        index.writeText(json.encodeToString(all.map {
            if (it.status == SavedHit.STATUS_UPLOADING) it.copy(status = SavedHit.STATUS_PENDING) else it
        }))
    }

    fun videoFile(hit: SavedHit): File = File(dir, hit.videoFileName)
}
