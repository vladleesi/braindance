// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaGalleryStateTest {
    private val video = GameMedia.YouTubeVideo("aaaaaaaaaaa")
    private val secondVideo = GameMedia.YouTubeVideo("bbbbbbbbbbb")
    private val screenshots =
        List(3) { index -> GameMedia.Screenshot("https://images.igdb.com/igdb/image/upload/t_thumb/sc$index.jpg") }
    private val gallery = MediaGallery(videos = listOf(video, secondVideo), screenshots = screenshots)

    @Test
    fun theFirstItemIsSelectedAtFirst() {
        val state = MediaGalleryState(gallery)

        assertEquals(0, state.selectedIndex)
        assertEquals(video, state.selected)
        assertNull(state.selectedScreenshotIndex)
        assertFalse(state.isPlaying)
        assertFalse(state.isViewerOpen)
        assertFalse(state.canSelectPrevious)
        assertTrue(state.canSelectNext)
    }

    @Test
    fun selectionStopsAtBothEnds() {
        val state = MediaGalleryState(gallery)

        state.selectPrevious()
        assertEquals(0, state.selectedIndex)

        repeat(10) { state.selectNext() }
        assertEquals(gallery.items.lastIndex, state.selectedIndex)
        assertFalse(state.canSelectNext)

        state.select(-5)
        assertEquals(0, state.selectedIndex)
        state.select(99)
        assertEquals(gallery.items.lastIndex, state.selectedIndex)
    }

    @Test
    fun rapidSelectionEndsOnTheLastSelectedItem() {
        val state = MediaGalleryState(gallery)

        listOf(3, 1, 4, 2).forEach(state::select)

        assertEquals(2, state.selectedIndex)
        assertEquals(screenshots[0], state.selected)
        assertEquals(0, state.selectedScreenshotIndex)
    }

    @Test
    fun selectingAnotherItemStopsPlaybackAndScreenshotsNeverPlay() {
        val state = MediaGalleryState(gallery)

        state.play()
        assertTrue(state.isPlaying)
        state.select(0)
        assertTrue(state.isPlaying, "selecting the playing video again keeps it playing")

        state.selectNext()
        assertFalse(state.isPlaying)

        state.select(2)
        state.play()
        assertFalse(state.isPlaying)
    }

    @Test
    fun aFailedVideoCanBeRetriedAndFailuresOfAnotherVideoAreIgnored() {
        val state = MediaGalleryState(gallery)
        state.play()

        state.onPlaybackFailed(secondVideo)
        assertFalse(state.playbackFailed)

        state.onPlaybackFailed(video)
        assertTrue(state.playbackFailed)

        state.play()
        assertFalse(state.playbackFailed)

        state.onPlaybackFailed(video)
        state.selectNext()
        assertFalse(state.playbackFailed)
    }

    @Test
    fun theViewerOpensOnlyOnAScreenshotAndMovesThroughScreenshotsOnly() {
        val state = MediaGalleryState(gallery)

        state.openViewer()
        assertFalse(state.isViewerOpen, "videos play only in the preview")

        state.select(3)
        state.openViewer()
        assertTrue(state.isViewerOpen)
        assertEquals(1, state.selectedScreenshotIndex)

        state.selectPrevious()
        state.selectPrevious()
        state.select(0)
        assertEquals(2, state.selectedIndex, "the viewer never selects a video")
        assertFalse(state.canSelectPrevious)

        state.selectScreenshot(2)
        state.closeViewer()
        assertFalse(state.isViewerOpen)
        assertEquals(4, state.selectedIndex, "the gallery shows the screenshot viewed last")
        assertTrue(state.canSelectPrevious)
    }

    @Test
    fun noVideoPlaysWhileTheViewerIsOpen() {
        val state = MediaGalleryState(gallery, initialIndex = 2)
        state.openViewer()

        state.select(0)
        state.play()

        assertFalse(state.isPlaying)
    }

    @Test
    fun recreationKeepsTheSelectionAndTheViewerButNeverPlayback() {
        val saver = MediaGalleryState.saver(gallery)

        fun recreate(state: MediaGalleryState): MediaGalleryState {
            val saved = with(saver) { SaverScope { true }.save(state) }
            return saver.restore(checkNotNull(saved)) ?: error("not restored")
        }

        val inViewer = MediaGalleryState(gallery, initialIndex = 3).apply { openViewer() }
        val restored = recreate(inViewer)
        assertEquals(3, restored.selectedIndex)
        assertTrue(restored.isViewerOpen)

        val playing = MediaGalleryState(gallery, initialIndex = 1).apply { play() }
        val returned = recreate(playing)
        assertEquals(1, returned.selectedIndex)
        assertFalse(returned.isPlaying, "a video never starts again by itself")
    }

    @Test
    fun aRestoredIndexIsClampedAndAViewerOnAVideoStaysClosed() {
        assertEquals(gallery.items.lastIndex, MediaGalleryState(gallery, initialIndex = 42).selectedIndex)
        assertEquals(0, MediaGalleryState(gallery, initialIndex = -1).selectedIndex)
        assertFalse(MediaGalleryState(gallery, initialIndex = 0, initialViewerOpen = true).isViewerOpen)
    }

    @Test
    fun anEmptyGalleryHasNoSelection() {
        val state = MediaGalleryState(MediaGallery(), initialIndex = 2, initialViewerOpen = true)

        state.selectNext()
        state.select(1)
        state.play()
        state.openViewer()

        assertEquals(0, state.selectedIndex)
        assertNull(state.selected)
        assertFalse(state.isPlaying)
        assertFalse(state.isViewerOpen)
        assertFalse(state.canSelectPrevious)
        assertFalse(state.canSelectNext)
    }
}
