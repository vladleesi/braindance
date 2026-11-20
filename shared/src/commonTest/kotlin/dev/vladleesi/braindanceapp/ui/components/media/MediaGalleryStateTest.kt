// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

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
        assertFalse(state.isPlaying)
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
        assertEquals(screenshots.last(), state.selected)
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
    }

    @Test
    fun selectingAnotherItemStopsPlaybackAndScreenshotsNeverPlay() {
        val state = MediaGalleryState(gallery)

        state.play()
        assertTrue(state.isPlaying)

        // Selecting the item already selected keeps it playing.
        state.select(0)
        assertTrue(state.isPlaying)

        state.selectNext()
        assertEquals(secondVideo, state.selected)
        assertFalse(state.isPlaying)

        state.play()
        state.stop()
        assertFalse(state.isPlaying)

        state.select(2)
        state.play()
        assertFalse(state.isPlaying)
    }

    @Test
    fun aRestoredIndexIsClampedToTheGallery() {
        assertEquals(3, MediaGalleryState(gallery, initialIndex = 3).selectedIndex)
        assertEquals(gallery.items.lastIndex, MediaGalleryState(gallery, initialIndex = 42).selectedIndex)
        assertEquals(0, MediaGalleryState(gallery, initialIndex = -1).selectedIndex)
    }

    @Test
    fun anEmptyGalleryHasNoSelection() {
        val state = MediaGalleryState(MediaGallery(), initialIndex = 2)

        state.selectNext()
        state.select(1)
        state.play()

        assertEquals(0, state.selectedIndex)
        assertNull(state.selected)
        assertFalse(state.isPlaying)
        assertFalse(state.canSelectPrevious)
        assertFalse(state.canSelectNext)
    }
}
