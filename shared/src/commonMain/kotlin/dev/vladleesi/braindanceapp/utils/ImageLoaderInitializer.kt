// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.size.Dimension
import coil3.size.Scale
import coil3.util.DebugLogger
import coil3.util.Logger
import dev.vladleesi.braindanceapp.data.api.apiFixtureEngine
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
        .components {
            add(SmallerCoverInterceptor())
            add(KtorNetworkFetcherFactory(httpClient = { imageHttpClient(engine = apiFixtureEngine()) }))
        }.memoryCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .configurePlatformCache()
        .crossfade(true)
        // Per-request logs are costly on every image load; keep only warnings and failures.
        .logger(DebugLogger(minLevel = Logger.Level.Warn))
        .build()

/**
 * Downloads a cropped cover at the smallest IGDB size that fills the pixels it is displayed at, such as a desktop card
 * on a standard-density screen, instead of the large file every card asks for; see [smallerCoverSizeOrNull].
 *
 * Each size is cached under its own keys, so a larger display of the same cover never gets a smaller file from a
 * cache. If the smaller file does not fill the display after all, which only a cover that is not 3:4 can cause, the
 * large one is loaded as before. A request with no pixel size, such as one that keeps the original size, and an image
 * that is fitted rather than cropped are left as they are.
 */
internal class SmallerCoverInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val displayWidth = chain.size.width as? Dimension.Pixels
        val displayHeight = chain.size.height as? Dimension.Pixels
        if (displayWidth == null || displayHeight == null || request.scale != Scale.FILL) return chain.proceed()
        val smallerSize = smallerCoverSizeOrNull(displayWidth.px, displayHeight.px)
        val smallerUrl = smallerSize?.let { size -> (request.data as? String)?.largeCoverUrlAtOrNull(size) }
        if (smallerSize == null || smallerUrl == null) return chain.proceed()
        val keySuffix = "-${smallerSize.size}"
        val smallerRequest =
            request
                .newBuilder()
                .data(smallerUrl)
                .memoryCacheKey(request.memoryCacheKey?.let { key -> key + keySuffix })
                .diskCacheKey(request.diskCacheKey?.let { key -> key + keySuffix })
                .build()
        val smallerResult = chain.withRequest(smallerRequest).proceed()
        val isTooSmall =
            smallerResult is SuccessResult &&
                (
                    smallerResult.image.width < displayWidth.px - SIZE_TOLERANCE_PX ||
                        smallerResult.image.height < displayHeight.px - SIZE_TOLERANCE_PX
                )
        return if (isTooSmall) chain.proceed() else smallerResult
    }

    private companion object {
        // Decoders round the scaled size, and a few covers are one pixel short of 3:4.
        const val SIZE_TOLERANCE_PX = 1
    }
}

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
