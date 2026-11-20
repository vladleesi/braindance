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
 * Which item of [gallery] is selected, and whether the user has started the selected video. It does not depend on
 * any layout, so the inline gallery and a future fullscreen viewer can share one instance and stay on the same item.
 *
 * Selection stops at the first and last items rather than wrapping around. Selecting another item stops playback, so
 * at most one player exists and it never outlives its video.
 */
@Stable
class MediaGalleryState(
    val gallery: MediaGallery,
    initialIndex: Int = 0,
) {
    private val lastIndex = gallery.items.lastIndex

    /** The selected item's index, or 0 for an empty gallery. */
    var selectedIndex by mutableIntStateOf(initialIndex.coerceIn(0, lastIndex.coerceAtLeast(0)))
        private set

    /** The selected item, or null for an empty gallery. */
    val selected: GameMedia? get() = gallery.items.getOrNull(selectedIndex)

    /** Whether the user started the selected video; false for a screenshot. */
    var isPlaying by mutableStateOf(false)
        private set

    val canSelectPrevious: Boolean get() = selectedIndex > 0
    val canSelectNext: Boolean get() = selectedIndex < lastIndex

    /** Selects the item at [index], clamped to the gallery. Selecting another item stops playback. */
    fun select(index: Int) {
        if (lastIndex < 0) return
        val target = index.coerceIn(0, lastIndex)
        if (target == selectedIndex) return
        selectedIndex = target
        isPlaying = false
    }

    fun selectPrevious() = select(selectedIndex - 1)

    fun selectNext() = select(selectedIndex + 1)

    /** Starts the selected video. It does nothing for a screenshot. */
    fun play() {
        if (selected is GameMedia.YouTubeVideo) isPlaying = true
    }

    fun stop() {
        isPlaying = false
    }
}

/**
 * The selection for [gallery]. The selected index survives recreation and returning to the screen; playback does
 * not, so a video never starts again by itself. Another gallery, such as another version's, starts at its first item.
 */
@Composable
fun rememberMediaGalleryState(gallery: MediaGallery): MediaGalleryState =
    rememberSaveable(
        gallery,
        saver =
            Saver(
                save = { state -> state.selectedIndex },
                restore = { index -> MediaGalleryState(gallery, initialIndex = index) },
            ),
    ) {
        MediaGalleryState(gallery)
    }
