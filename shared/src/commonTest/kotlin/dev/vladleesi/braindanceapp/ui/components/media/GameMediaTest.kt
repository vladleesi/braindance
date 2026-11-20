// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import dev.vladleesi.braindanceapp.data.models.games.Screenshot
import dev.vladleesi.braindanceapp.data.models.games.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameMediaTest {
    private val thumbs = "//images.igdb.com/igdb/image/upload/t_thumb"

    private fun video(id: String?) = Video(id = null, videoId = id)

    private fun screenshot(url: String?) = Screenshot(animated = null, id = null, url = url)

    @Test
    fun videosComeFirstAndEachKindKeepsTheApiOrder() {
        val gallery =
            mediaGallery(
                videos = listOf(video("bbbbbbbbbbb"), video("aaaaaaaaaaa")),
                screenshots = listOf(screenshot("$thumbs/sc2.jpg"), screenshot("$thumbs/sc1.jpg")),
            )

        assertEquals(
            listOf(
                "video:bbbbbbbbbbb",
                "video:aaaaaaaaaaa",
                "screenshot:https:$thumbs/sc2.jpg",
                "screenshot:https:$thumbs/sc1.jpg",
            ),
            gallery.items.map { it.key },
        )
    }

    @Test
    fun duplicatesAndInvalidItemsAreLeftOutSoKeysAreUnique() {
        val gallery =
            mediaGallery(
                videos =
                    listOf(
                        video("yowv6_rspoM"),
                        video("yowv6_rspoM"),
                        video(null),
                        video(""),
                        // Too short, too long, and characters outside YouTube's alphabet.
                        video("short"),
                        video("yowv6_rspoMx"),
                        video("abc\"><scrip"),
                        video("abc/def?ghi"),
                    ),
                screenshots =
                    listOf(
                        screenshot("$thumbs/sc1.jpg"),
                        screenshot("https:$thumbs/sc1.jpg"),
                        screenshot(null),
                        screenshot(" "),
                        screenshot("http://images.igdb.com/igdb/image/upload/t_thumb/sc2.jpg"),
                        screenshot("javascript:alert(1)//t_thumb/"),
                        screenshot("https://user:secret@images.igdb.com/igdb/image/upload/t_thumb/sc3.jpg"),
                        // Another size than t_thumb cannot be resized, so the gallery would load the original.
                        screenshot("//images.igdb.com/igdb/image/upload/t_original/sc4.jpg"),
                    ),
            )

        assertEquals(listOf(GameMedia.YouTubeVideo("yowv6_rspoM")), gallery.videos)
        assertEquals(listOf(GameMedia.Screenshot("https:$thumbs/sc1.jpg")), gallery.screenshots)
        assertEquals(
            gallery.items.size,
            gallery.items
                .map { it.key }
                .toSet()
                .size,
        )
    }

    @Test
    fun missingOrEmptyListsGiveAnEmptyGallery() {
        assertTrue(mediaGallery(videos = null, screenshots = null).isEmpty)
        assertTrue(mediaGallery(videos = emptyList(), screenshots = listOf(screenshot(null))).isEmpty)
        assertTrue(MediaGallery().isEmpty)
    }

    @Test
    fun youTubeAddressesAreBuiltFromTheValidatedId() {
        val video = GameMedia.YouTubeVideo("yowv6_rspoM")

        assertEquals("https://i.ytimg.com/vi/yowv6_rspoM/mqdefault.jpg", video.thumbnailUrl)
        assertEquals("https://i.ytimg.com/vi/yowv6_rspoM/hqdefault.jpg", video.posterUrl(widthPx = 480))
        assertEquals("https://i.ytimg.com/vi/yowv6_rspoM/maxresdefault.jpg", video.posterUrl(widthPx = 481))
        assertEquals("https://i.ytimg.com/vi/yowv6_rspoM/hqdefault.jpg", video.fallbackPosterUrl)
        assertEquals(
            "https://www.youtube-nocookie.com/embed/yowv6_rspoM?autoplay=1&rel=0&playsinline=1",
            video.embedUrl,
        )
        assertEquals("https://www.youtube.com/watch?v=yowv6_rspoM", video.watchUrl)
        assertEquals("yowv6_rspoM", " yowv6_rspoM ".toYouTubeIdOrNull())
        assertNull("yowv6 rspoM".toYouTubeIdOrNull())
    }

    @Test
    fun screenshotsLoadSmallThumbnailsAndAFittedPreviewForTheDisplayWidth() {
        val screenshot = GameMedia.Screenshot("https:$thumbs/sc1.jpg")

        assertEquals("https://images.igdb.com/igdb/image/upload/t_screenshot_med/sc1.webp", screenshot.thumbnailUrl)
        assertEquals("https://images.igdb.com/igdb/image/upload/t_720p/sc1.webp", screenshot.previewUrl(widthPx = 400))
        assertEquals("https://images.igdb.com/igdb/image/upload/t_720p/sc1.webp", screenshot.previewUrl(widthPx = 1280))
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_1080p/sc1.webp",
            screenshot.previewUrl(widthPx = 1281),
        )
    }

    @Test
    fun positionsCountWithinEachKind() {
        val gallery =
            mediaGallery(
                videos = listOf(video("aaaaaaaaaaa"), video("bbbbbbbbbbb")),
                screenshots = List(3) { index -> screenshot("$thumbs/sc$index.jpg") },
            )

        assertEquals(MediaPosition(number = 1, count = 2), gallery.positionInKind(0))
        assertEquals(MediaPosition(number = 2, count = 2), gallery.positionInKind(1))
        assertEquals(MediaPosition(number = 1, count = 3), gallery.positionInKind(2))
        assertEquals(MediaPosition(number = 3, count = 3), gallery.positionInKind(4))
    }
}
