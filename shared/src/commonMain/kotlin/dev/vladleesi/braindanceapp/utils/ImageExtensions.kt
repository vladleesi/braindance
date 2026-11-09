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
 * Builds an image request cached under [imageCacheKey]. Pass it to `AsyncImage` with the same `contentScale` the
 * image is drawn with: `AsyncImage` sizes the request from its layout constraints, so the bitmap matches the displayed
 * pixel size, never smaller and never larger than the download. A painter from `rememberAsyncImagePainter` has no size
 * and would keep the downloaded file's full pixel size.
 */
@Composable
fun rememberCachedImageRequest(
    data: Any?,
    imageCacheKey: String?,
): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(context, data, imageCacheKey) { cachedImageRequest(context, data, imageCacheKey) }
}

/**
 * [keepOriginalSize] fixes the request at the downloaded file's pixel size, so a platform whose downscaling is not
 * filtered (see [scalesDecodedImagesSmoothly]) cannot lower image quality. Otherwise the request has no size, and
 * `AsyncImage` sizes it from its layout constraints.
 */
internal fun cachedImageRequest(
    context: PlatformContext,
    data: Any?,
    imageCacheKey: String?,
    keepOriginalSize: Boolean = !scalesDecodedImagesSmoothly,
): ImageRequest {
    val builder =
        ImageRequest
            .Builder(context)
            .data(data)
            .memoryCacheKey(imageCacheKey)
            .diskCacheKey(imageCacheKey)
            .crossfade(true)
    if (keepOriginalSize) {
        builder.size(Size.ORIGINAL)
    }
    return builder.build()
}

/** Whether Coil filters an image it scales down to the requested size, so the smaller bitmap keeps its quality. */
internal expect val scalesDecodedImagesSmoothly: Boolean
