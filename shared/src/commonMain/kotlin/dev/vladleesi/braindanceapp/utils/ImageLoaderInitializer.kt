// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import coil3.util.Logger
import dev.vladleesi.braindanceapp.data.api.retryConfig
import dev.vladleesi.braindanceapp.data.api.timeoutConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import okio.FileSystem

object ImageLoaderInitializer {
    @Composable
    fun initialize() {
        setSingletonImageLoaderFactory(::createImageLoader)
    }
}

/**
 * Image downloads use the same per-attempt timeout and bounded retries as backend requests, so a stalled image cannot
 * load indefinitely. [engine] is null in the app, which selects the platform engine.
 */
internal fun imageHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        retryConfig()
        timeoutConfig()
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}

// Coil's defaults size the memory cache for the device and the disk cache at 2% of free space, capped at 250 MB.
private fun createImageLoader(context: PlatformContext) =
    ImageLoader
        .Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient = { imageHttpClient() })) }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .configurePlatformCache()
        .crossfade(true)
        // Per-request logs are costly on every image load; keep only warnings and failures.
        .logger(DebugLogger(minLevel = Logger.Level.Warn))
        .build()

private fun ImageLoader.Builder.configurePlatformCache(): ImageLoader.Builder {
    if (!isDiskCacheSupported) {
        return diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(null)
    }

    // The directory name is kept so images cached by earlier versions are reused instead of orphaned.
    return diskCachePolicy(CachePolicy.ENABLED)
        .diskCache {
            DiskCache
                .Builder()
                .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                .build()
        }
}

internal expect val isDiskCacheSupported: Boolean
