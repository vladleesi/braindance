// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The gallery's selection, its one playing video, and its full-screen screenshot viewer.
 *
 * Videos play only in the gallery's preview, so there is at most one player, and selecting another item stops it.
 * The viewer shows the screenshots alone, opens on the selected screenshot, and moves the same selection, so closing
 * it leaves the gallery on the screenshot viewed last. Selection stops at the first and last items rather than
 * wrapping around.
 */
@Stable
class MediaGalleryState(
    val gallery: MediaGallery,
    initialIndex: Int = 0,
    initialViewerOpen: Boolean = false,
) {
    private val lastIndex = gallery.items.lastIndex

    /** The selected item's index, or 0 for an empty gallery. */
    var selectedIndex by mutableIntStateOf(initialIndex.coerceIn(0, lastIndex.coerceAtLeast(0)))
        private set

    /** The selected item, or null for an empty gallery. */
    val selected: GameMedia? get() = gallery.items.getOrNull(selectedIndex)

    /** Whether the full-screen screenshot viewer is open; it opens only on a selected screenshot. */
    var isViewerOpen by mutableStateOf(initialViewerOpen && selected is GameMedia.Screenshot)
        private set

    /** Whether the user started the selected video in the preview. */
    var isPlaying by mutableStateOf(false)
        private set

    /** Whether the embedded player is starting the selected video. */
    internal var playbackStarting by mutableStateOf(false)
        private set

    /** Whether the selected video failed to play, so the player gives way to a link to YouTube. */
    var playbackFailed by mutableStateOf(false)
        private set

    /** The selected screenshot's index among the screenshots, or null while a video is selected. */
    val selectedScreenshotIndex: Int?
        get() = (selectedIndex - gallery.videos.size).takeIf { selected is GameMedia.Screenshot }

    val canSelectPrevious: Boolean get() = selectedIndex > firstSelectableIndex
    val canSelectNext: Boolean get() = selectedIndex < lastIndex

    // The viewer moves through screenshots only.
    private val firstSelectableIndex: Int get() = if (isViewerOpen) gallery.videos.size else 0

    /** Selects the item at [index], clamped to what can be selected. Selecting another item stops playback. */
    fun select(index: Int) {
        if (lastIndex < 0) return
        val target = index.coerceIn(firstSelectableIndex, lastIndex)
        if (target == selectedIndex) return
        selectedIndex = target
        stop()
    }

    fun selectPrevious() = select(selectedIndex - 1)

    fun selectNext() = select(selectedIndex + 1)

    /** Selects the screenshot at [index] among the screenshots. */
    fun selectScreenshot(index: Int) = select(gallery.videos.size + index)

    /** Starts the selected video in the preview. It does nothing for a screenshot or while the viewer is open. */
    fun play() {
        if (selected !is GameMedia.YouTubeVideo || isViewerOpen) return
        playbackFailed = false
        isPlaying = true
    }

    fun stop() {
        isPlaying = false
        playbackStarting = false
        playbackFailed = false
    }

    internal fun onPlaybackStartingChanged(
        video: GameMedia.YouTubeVideo,
        starting: Boolean,
    ) {
        if (isPlaying && selected == video && !playbackFailed) playbackStarting = starting
    }

    /** The player of [video] could not play it. Reports from a video no longer playing are ignored. */
    fun onPlaybackFailed(video: GameMedia.YouTubeVideo) {
        if (isPlaying && selected == video) {
            playbackStarting = false
            playbackFailed = true
        }
    }

    /** Opens the full-screen viewer on the selected screenshot. It does nothing while a video is selected. */
    fun openViewer() {
        if (selected is GameMedia.Screenshot) isViewerOpen = true
    }

    /** Closes the viewer; the gallery keeps the screenshot viewed last. */
    fun closeViewer() {
        isViewerOpen = false
    }

    internal companion object {
        /**
         * Saves the selection and whether the viewer is open, so a rotation that recreates the Android activity keeps
         * them. Playback is not saved: a video never starts again by itself.
         */
        fun saver(gallery: MediaGallery): Saver<MediaGalleryState, List<Any>> =
            Saver(
                save = { state -> listOf(state.selectedIndex, state.isViewerOpen) },
                restore = { saved ->
                    MediaGalleryState(
                        gallery = gallery,
                        initialIndex = saved[0] as Int,
                        initialViewerOpen = saved[1] as Boolean,
                    )
                },
            )
    }
}

/**
 * The selection for [gallery]. Selection and the open viewer survive recreation and returning to the screen; another
 * gallery, such as another version's, starts at its first item.
 */
@Composable
fun rememberMediaGalleryState(gallery: MediaGallery): MediaGalleryState =
    rememberSaveable(gallery, saver = MediaGalleryState.saver(gallery)) {
        MediaGalleryState(gallery)
    }
