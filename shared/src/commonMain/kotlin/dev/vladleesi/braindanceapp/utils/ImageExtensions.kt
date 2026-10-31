// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

fun String.toContentDescription(): String = "Image: $this"

@Composable
fun rememberCachedImagePainter(
    data: Any?,
    imageCacheKey: String?,
): AsyncImagePainter {
    val context = LocalPlatformContext.current
    val request =
        remember(context, data, imageCacheKey) {
            ImageRequest
                .Builder(context)
                .data(data)
                .memoryCacheKey(imageCacheKey)
                .diskCacheKey(imageCacheKey)
                .crossfade(true)
                .build()
        }
    return rememberAsyncImagePainter(model = request)
}
