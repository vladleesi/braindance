// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import dev.vladleesi.braindanceapp.data.models.games.Screenshot
import dev.vladleesi.braindanceapp.data.models.games.Video
import dev.vladleesi.braindanceapp.utils.CoverSize
import dev.vladleesi.braindanceapp.utils.toCoverUrl
import io.ktor.http.URLProtocol
import io.ktor.http.parseUrl

/** One item of a game's media gallery: a YouTube video or a screenshot. */
sealed interface GameMedia {
    /** Unique within its gallery, so lazy lists and a saved selection can use it. */
    val key: String

    /**
     * A YouTube video. [youTubeId] is validated (see [toYouTubeIdOrNull]); every address is built from it here, so
     * no address from the API reaches the player or an external app.
     */
    data class YouTubeVideo(
        val youTubeId: String,
    ) : GameMedia {
        override val key: String get() = "video:$youTubeId"

        /** 320x180, 16:9 without letterboxing: the strip's thumbnail. */
        val thumbnailUrl: String get() = "$YOUTUBE_IMAGE_BASE/$youTubeId/mqdefault.jpg"

        /**
         * The poster for a preview [widthPx] pixels wide. YouTube has a 1280x720 poster only for videos uploaded in
         * HD, so a wide preview falls back to [fallbackPosterUrl] when it is missing.
         */
        fun posterUrl(widthPx: Int): String = if (widthPx > FALLBACK_POSTER_WIDTH_PX) hdPosterUrl else fallbackPosterUrl

        private val hdPosterUrl: String get() = "$YOUTUBE_IMAGE_BASE/$youTubeId/maxresdefault.jpg"

        /** 480x360 with the 16:9 frame letterboxed: every video has it; the preview crops the bars. */
        val fallbackPosterUrl: String get() = "$YOUTUBE_IMAGE_BASE/$youTubeId/hqdefault.jpg"

        /**
         * The privacy-enhanced embed: YouTube sets no cookies before playback. It starts playing when loaded, which
         * happens only after the user presses play, and suggests only videos from the same channel at the end.
         */
        val embedUrl: String get() = "https://www.youtube-nocookie.com/embed/$youTubeId?autoplay=1&rel=0&playsinline=1"

        /** The video's page, which the YouTube app opens where it is installed. */
        val watchUrl: String get() = "https://www.youtube.com/watch?v=$youTubeId"
    }

    /** An IGDB screenshot. [thumbUrl] is its HTTPS address at IGDB's `t_thumb` size, from which the sizes derive. */
    data class Screenshot(
        val thumbUrl: String,
    ) : GameMedia {
        override val key: String get() = "screenshot:$thumbUrl"

        /** 569x320, cropped to 16:9: the strip's thumbnail, the same file as the hero's compact backdrop. */
        val thumbnailUrl: String get() = thumbUrl.toCoverUrl(CoverSize.SCREENSHOT_MED)

        /**
         * The image for a preview [widthPx] pixels wide, at its own aspect ratio: IGDB fits its 720p and 1080p sizes
         * into the frame, while its smaller screenshot sizes are cropped to 16:9.
         */
        fun previewUrl(widthPx: Int): String =
            thumbUrl.toCoverUrl(if (widthPx > P_720_WIDTH_PX) CoverSize.P_1080 else CoverSize.P_720)
    }
}

/**
 * A game's videos and then its screenshots, each in the API's order, without duplicates or items that cannot be
 * shown. It holds no selection, so a future fullscreen viewer can show the same gallery.
 */
data class MediaGallery(
    val videos: List<GameMedia.YouTubeVideo> = emptyList(),
    val screenshots: List<GameMedia.Screenshot> = emptyList(),
) {
    /** Every item in the order the gallery shows them: videos first. */
    val items: List<GameMedia> = videos + screenshots

    val isEmpty: Boolean get() = items.isEmpty()

    /** The item at [index] counted within its kind, 1-based, with that kind's count, for labels such as "2 of 5". */
    fun positionInKind(index: Int): MediaPosition =
        if (index < videos.size) {
            MediaPosition(number = index + 1, count = videos.size)
        } else {
            MediaPosition(number = index - videos.size + 1, count = screenshots.size)
        }
}

data class MediaPosition(
    val number: Int,
    val count: Int,
)

/** The gallery for these API lists; items without a valid video ID or screenshot address are left out. */
internal fun mediaGallery(
    videos: List<Video>?,
    screenshots: List<Screenshot>?,
): MediaGallery =
    MediaGallery(
        videos =
            videos
                .orEmpty()
                .mapNotNull { video -> video.videoId.toYouTubeIdOrNull()?.let(GameMedia::YouTubeVideo) }
                .distinctBy { it.youTubeId },
        screenshots =
            screenshots
                .orEmpty()
                .mapNotNull { screenshot -> screenshot.url.toScreenshotUrlOrNull()?.let(GameMedia::Screenshot) }
                .distinctBy { it.thumbUrl },
    )

/** This value when it is a YouTube video ID: 11 characters of letters, digits, `-`, and `_`. */
internal fun String?.toYouTubeIdOrNull(): String? = this?.trim()?.takeIf(youTubeIdPattern::matches)

/**
 * This screenshot address over HTTPS, when it is an absolute or protocol-relative `https` address with a host, no
 * credentials, and IGDB's `t_thumb` size, from which the gallery derives its sizes. Anything else is left out.
 */
internal fun String?.toScreenshotUrlOrNull(): String? {
    val candidate = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val absolute = if (candidate.startsWith("//")) "https:$candidate" else candidate
    val url = parseUrl(absolute) ?: return null
    val valid =
        absolute.startsWith("https://", ignoreCase = true) &&
            url.protocol == URLProtocol.HTTPS &&
            url.host.isNotEmpty() &&
            url.user == null &&
            THUMB_SEGMENT in url.encodedPath
    return absolute.takeIf { valid }
}

private val youTubeIdPattern = Regex("[A-Za-z0-9_-]{11}")

private const val YOUTUBE_IMAGE_BASE = "https://i.ytimg.com/vi"
private const val THUMB_SEGMENT = "/t_thumb/"

// hqdefault's width. A wider preview asks for the 1280x720 poster.
private const val FALLBACK_POSTER_WIDTH_PX = 480

// IGDB's 720p width. A wider preview asks for 1080p.
private const val P_720_WIDTH_PX = 1280
