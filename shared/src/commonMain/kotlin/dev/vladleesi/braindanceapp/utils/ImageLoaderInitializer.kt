// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import coil3.util.Logger
import okio.FileSystem

object ImageLoaderInitializer {
    @Composable
    fun initialize() {
        setSingletonImageLoaderFactory(::createImageLoader)
    }
}

// Cache configuration constants
private const val MEMORY_CACHE_PERCENTAGE = 0.3
private const val DISK_CACHE_MAX_SIZE_BYTES = 512L * 1024 * 1024

// Builds the ImageLoader with enabled caching policies
private fun createImageLoader(context: PlatformContext) =
    ImageLoader
        .Builder(context)
        .components { add(KtorNetworkFetcherFactory()) }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .memoryCache { configureMemoryCache(context) }
        .networkCachePolicy(CachePolicy.ENABLED)
        .configurePlatformCache()
        .crossfade(true)
        // Per-request logs are costly on every image load; keep only warnings and failures.
        .logger(DebugLogger(minLevel = Logger.Level.Warn))
        .build()

// Configures the memory cache with a specific percentage of available memory
private fun configureMemoryCache(context: PlatformContext) =
    MemoryCache
        .Builder()
        .maxSizePercent(context, MEMORY_CACHE_PERCENTAGE)
        .strongReferencesEnabled(true)
        .build()

private fun ImageLoader.Builder.configurePlatformCache(): ImageLoader.Builder {
    if (!isDiskCacheSupported) {
        return diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(null)
    }

    return diskCachePolicy(CachePolicy.ENABLED)
        .diskCache {
            DiskCache
                .Builder()
                .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                .maxSizeBytes(DISK_CACHE_MAX_SIZE_BYTES)
                .build()
        }
}

internal expect val isDiskCacheSupported: Boolean
