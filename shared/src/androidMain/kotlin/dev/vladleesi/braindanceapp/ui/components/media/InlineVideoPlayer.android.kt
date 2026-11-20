// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

// Videos open in the YouTube app or the browser; see playsVideosInline in commonMain.
internal actual val playsVideosInline: Boolean = false

@Composable
internal actual fun InlineVideoPlayer(
    video: GameMedia.YouTubeVideo,
    title: String,
    coveredTop: Dp,
    modifier: Modifier,
) {
    // Never called: the gallery opens videos outside the app here.
}
