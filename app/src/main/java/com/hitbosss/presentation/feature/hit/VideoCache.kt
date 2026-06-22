package com.hitbosss.presentation.feature.hit

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

/**
 * Caché de vídeo en disco compartida por todos los reproductores de hits (Media3) + prefetch.
 * Los bytes descargados se reutilizan entre aperturas, deslizamientos y el preload, de modo que
 * reabrir o pasar a otro hit es instantáneo.
 *
 * SimpleCache exige una única instancia por directorio en todo el proceso → singleton lazy.
 */
@UnstableApi
object VideoCache {
    private const val MAX_BYTES = 256L * 1024 * 1024  // 256 MB LRU total
    // Tope por clip al prefetch: los hits son cortos, así se cachea el clip ENTERO (incluido el
    // átomo 'moov' esté donde esté) sin descargar de más en el raro caso de un vídeo muy largo.
    private const val PREFETCH_BYTES = 40L * 1024 * 1024

    @Volatile private var cache: SimpleCache? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefetched = Collections.synchronizedSet(mutableSetOf<String>())

    private fun cache(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "media"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext),
        ).also { cache = it }
    }

    private fun cacheDataSourceFactory(context: Context): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    /** Factoría de MediaSource con caché para construir los ExoPlayer. */
    fun mediaSourceFactory(context: Context): DefaultMediaSourceFactory =
        DefaultMediaSourceFactory(cacheDataSourceFactory(context))

    /**
     * Prefetch del clip completo en la caché compartida (en segundo plano), para que al abrirlo o
     * deslizar a él arranque sin descarga en frío. Idempotente y tolerante a fallos.
     */
    fun prefetch(context: Context, url: String?) {
        if (url.isNullOrBlank() || !prefetched.add(url)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                val spec = DataSpec.Builder().setUri(url).setLength(PREFETCH_BYTES).build()
                CacheWriter(cacheDataSourceFactory(appContext).createDataSource(), spec, null, null).cache()
            } catch (_: Exception) {
                prefetched.remove(url) // reintentar la próxima vez si falló
            }
        }
    }
}
