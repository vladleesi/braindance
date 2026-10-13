// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.runtime.Composable
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

fun String.toContentDescription(): String = "Image: $this"

@Composable
fun rememberCachedImagePainter(
    data: Any?,
    imageCacheKey: String?,
) = rememberAsyncImagePainter(
    model =
        ImageRequest
            .Builder(LocalPlatformContext.current)
            .data(data)
            .memoryCacheKey(imageCacheKey)
            .diskCacheKey(imageCacheKey)
            .crossfade(true)
            .build(),
)
