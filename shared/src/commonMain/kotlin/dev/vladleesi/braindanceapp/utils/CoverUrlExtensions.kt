// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath

private const val JPEG_EXTENSION = ".jpg"
private const val WEBP_EXTENSION = ".webp"

/** A cover size below [CoverSize.P_720] with the pixels IGDB serves it at. IGDB stores covers at 3:4. */
private class SmallerCover(
    val size: CoverSize,
    val widthPx: Int,
    val heightPx: Int,
)

// Smallest first. About 13 KB and 21 KB as WebP, against about 60 KB for the 540x720 cover at CoverSize.P_720.
private val smallerCovers =
    listOf(
        SmallerCover(CoverSize.COVER_SMALL_2X, widthPx = 180, heightPx = 240),
        SmallerCover(CoverSize.COVER_BIG, widthPx = 264, heightPx = 352),
    )

/**
 * This IGDB image's address at [size], over HTTPS and as WebP. IGDB serves every image in both formats under the same
 * name; the WebP has the same pixel size and is about a quarter smaller than the JPEG.
 */
fun String.toCoverUrl(size: CoverSize): String {
    val coverUrlBuilder =
        URLBuilder(this).apply {
            protocol = URLProtocol.HTTPS
            val sizedPath = encodedPath.replace(CoverSize.THUMB.size, size.size)
            encodedPath =
                if (sizedPath.endsWith(JPEG_EXTENSION)) {
                    sizedPath.removeSuffix(JPEG_EXTENSION) + WEBP_EXTENSION
                } else {
                    sizedPath
                }
        }
    return coverUrlBuilder.buildString()
}

/**
 * The smallest cover size whose pixels fill a display of this size, or null when only [CoverSize.P_720] does.
 */
internal fun smallerCoverSizeOrNull(
    displayWidthPx: Int,
    displayHeightPx: Int,
): CoverSize? =
    smallerCovers
        .firstOrNull { cover -> displayWidthPx <= cover.widthPx && displayHeightPx <= cover.heightPx }
        ?.size

/**
 * This cover's address at [size] when it is requested at [CoverSize.P_720], the size cards ask for; otherwise null,
 * as for another host's image. Only covers are requested at [CoverSize.P_720].
 */
internal fun String.largeCoverUrlAtOrNull(size: CoverSize): String? {
    val largeSizeSegment = "/${CoverSize.P_720.size}/"
    return if (contains(largeSizeSegment)) replace(largeSizeSegment, "/${size.size}/") else null
}

enum class CoverSize(
    val size: String,
) {
    THUMB("t_thumb"),
    COVER_SMALL("t_cover_small"),
    COVER_SMALL_2X("t_cover_small_2x"),
    COVER_BIG("t_cover_big"),
    LOGO_MED("t_logo_med"),
    P_720("t_720p"),
    P_1080("t_1080p"),
    ORIGINAL("t_original"),
}
