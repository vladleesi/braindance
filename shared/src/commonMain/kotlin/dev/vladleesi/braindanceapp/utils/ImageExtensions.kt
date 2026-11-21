// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size

fun String.toContentDescription(): String = "Image: $this"

/**
 * Builds an image request whose decoded bitmap is memory-cached under [memoryCacheKey]. Pass it to `AsyncImage` with
 * the same `contentScale` the image is drawn with: `AsyncImage` sizes the request from its layout constraints, so the
 * bitmap matches the displayed pixel size, never smaller and never larger than the download. A painter from
 * `rememberAsyncImagePainter` has no size and would keep the downloaded file's full pixel size.
 *
 * The downloaded file is disk-cached under its URL, which already names the image and its size. So every place that
 * shows the same file, such as a card and the details screen it opens, shares one download, while each keeps its own
 * memory key for its own decoded size.
 *
 * [placeholderMemoryCacheKey] names another image already in the memory cache, such as a smaller copy of the same
 * picture, to show while this one loads; nothing is shown when it is not cached. Turn off [crossfade] for an image of
 * another shape than its placeholder: while fading, Coil sizes both to the larger width and the larger height of the
 * two, a shape neither has, so the picture would jump when the image arrives and again when the fade ends. Set
 * [keepOriginalSize] for an image that is zoomed in, which needs the file's every pixel.
 */
@Composable
fun rememberCachedImageRequest(
    data: Any?,
    memoryCacheKey: String?,
    placeholderMemoryCacheKey: String? = null,
    crossfade: Boolean = true,
    keepOriginalSize: Boolean = !scalesDecodedImagesSmoothly,
): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(context, data, memoryCacheKey, placeholderMemoryCacheKey, crossfade, keepOriginalSize) {
        cachedImageRequest(
            context = context,
            data = data,
            memoryCacheKey = memoryCacheKey,
            keepOriginalSize = keepOriginalSize,
            placeholderMemoryCacheKey = placeholderMemoryCacheKey,
            crossfade = crossfade,
        )
    }
}

/**
 * [keepOriginalSize] fixes the request at the downloaded file's pixel size, so a platform whose downscaling is not
 * filtered (see [scalesDecodedImagesSmoothly]) cannot lower image quality. Otherwise the request has no size, and
 * `AsyncImage` sizes it from its layout constraints.
 */
internal fun cachedImageRequest(
    context: PlatformContext,
    data: Any?,
    memoryCacheKey: String?,
    keepOriginalSize: Boolean = !scalesDecodedImagesSmoothly,
    placeholderMemoryCacheKey: String? = null,
    crossfade: Boolean = true,
): ImageRequest {
    val builder =
        ImageRequest
            .Builder(context)
            .data(data)
            .memoryCacheKey(memoryCacheKey)
            .placeholderMemoryCacheKey(placeholderMemoryCacheKey)
            .crossfade(crossfade)
    if (keepOriginalSize) {
        builder.size(Size.ORIGINAL)
    }
    return builder.build()
}

/** Whether Coil filters an image it scales down to the requested size, so the smaller bitmap keeps its quality. */
internal expect val scalesDecodedImagesSmoothly: Boolean
