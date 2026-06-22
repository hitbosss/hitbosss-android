package com.hitbosss

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HitbosssApp : Application(), ImageLoaderFactory {

    /**
     * ImageLoader global con decoder de frames de vídeo y caché en disco propia, para que las
     * miniaturas de los hits (perfil/ranking/comunidad) se decodifiquen una vez y persistan →
     * preview instantánea al reabrir.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            // Ignora los cache-headers del servidor: avatares/portadas se sirven de la caché en disco
            // sin re-pedirlos (mismo URL = misma imagen). Ahorra requests; solo se piden los nuevos.
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
}
