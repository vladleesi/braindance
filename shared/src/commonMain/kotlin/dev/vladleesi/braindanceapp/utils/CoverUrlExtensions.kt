// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath

fun String.toCoverUrl(size: CoverSize): String {
    val coverUrlBuilder =
        URLBuilder(this).apply {
            protocol = URLProtocol.HTTPS
            encodedPath = encodedPath.replace(CoverSize.THUMB.size, size.size)
        }
    return coverUrlBuilder.buildString()
}

enum class CoverSize(
    val size: String,
) {
    THUMB("t_thumb"),
    COVER_SMALL("t_cover_small"),
    COVER_BIG("t_cover_big"),
    LOGO_MED("t_logo_med"),
    P_720("t_720p"),
    P_1080("t_1080p"),
    ORIGINAL("t_original"),
}
