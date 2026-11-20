// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Whether the gallery plays videos in place. Only the Web does, with YouTube's privacy-enhanced embed. On Android and
 * iOS, play opens the video in the YouTube app or the browser: an embedded player there would need a web view with
 * its own lifecycle, and YouTube refuses to play in web views that do not identify the app embedding them.
 */
internal expect val playsVideosInline: Boolean

/**
 * The player for [video], started by the user, filling [modifier]'s bounds; [title] names it for assistive
 * technology. It is created only when composed, so no player loads before the user presses play, and it is removed
 * when it leaves the composition. [coveredTop] is the height of the window's top that a bar covers, such as a
 * screen's top bar that the page scrolls under: the player is cut off below it. Only called where
 * [playsVideosInline].
 */
@Composable
internal expect fun InlineVideoPlayer(
    video: GameMedia.YouTubeVideo,
    title: String,
    coveredTop: Dp,
    modifier: Modifier,
)
