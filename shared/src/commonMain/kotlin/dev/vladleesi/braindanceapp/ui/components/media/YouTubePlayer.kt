// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * YouTube's privacy-enhanced player, with YouTube's own controls, in one web view that lives as long as this stays in
 * the composition: an iframe on the Web, a WebView on Android, and a WKWebView on iOS.
 *
 * It plays [video] at once, so set it only after the user pressed play. Another video loads into the same web view.
 * With [video] null, the player is stopped: its page is unloaded, which ends playback and frees the video's memory,
 * and it takes no space and no input, so whatever is under it can be used. [title] names it for assistive
 * technology. Playback changes are reported to [state].
 *
 * Keep it in place while the user moves between videos and stops them, and let it leave the composition only with its
 * parent. A platform view removed on its own, while the layout around it stays, can still be placed by that layout
 * in the same frame, which crashes Compose on Android; reusing the view also saves creating one per video. Links out
 * of the player, such as YouTube's logo, open in the YouTube app or the browser. [coveredTop] is the height of a bar
 * at the window's top that the page scrolls under: a web view drawn above the app's content is cut off below it.
 * On the Web, wheel input over the playing picture moves the surrounding page through [scrollState].
 */
@Composable
internal expect fun YouTubePlayer(
    video: GameMedia.YouTubeVideo?,
    title: String,
    coveredTop: Dp,
    state: MediaGalleryState,
    scrollState: ScrollState?,
)

/**
 * No size at all while the player is stopped ([video] is null), so it covers nothing and takes no input. Put it before
 * the modifiers that size the player: its zero size then holds whatever size they ask for.
 */
internal fun Modifier.collapsedWhenStopped(video: GameMedia.YouTubeVideo?): Modifier =
    if (video != null) this else size(0.dp)
