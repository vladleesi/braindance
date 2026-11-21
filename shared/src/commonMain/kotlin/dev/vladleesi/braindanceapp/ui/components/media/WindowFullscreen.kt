// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.runtime.Composable

/** Fullscreen for the whole app window, where the platform offers it on request. */
internal interface WindowFullscreen {
    /** Whether the window is fullscreen, by [enter] or otherwise. */
    val isActive: Boolean

    fun enter()

    fun exit()
}

/**
 * The window's fullscreen mode while this is in the composition, or null where an app cannot request it: on Android
 * and iOS the viewer already fills the screen. Fullscreen the viewer entered ends when it leaves the composition.
 */
@Composable
internal expect fun rememberWindowFullscreen(): WindowFullscreen?
