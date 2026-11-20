// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement
import kotlin.math.roundToInt

internal actual val playsVideosInline: Boolean = true

/**
 * YouTube's privacy-enhanced embed in an iframe that Compose lays over the gallery's preview. index.html's content
 * security policy allows frames only from www.youtube-nocookie.com. The sandbox lets the player run its scripts,
 * enter fullscreen, and open YouTube in a new tab, but not navigate the app's page. The page's referrer policy sends
 * YouTube only the app's origin, which its player requires to identify the embedding site.
 *
 * The browser draws the frame above the canvas, so Compose content drawn over it, such as a top bar the page
 * scrolls under, would be hidden. Compose cuts the frame off at its ancestors' clips, so the player sits in a
 * clipping window that starts below [coveredTop].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun InlineVideoPlayer(
    video: GameMedia.YouTubeVideo,
    title: String,
    coveredTop: Dp,
    modifier: Modifier,
) {
    val coveredTopPx = with(LocalDensity.current) { coveredTop.roundToPx() }
    // How far the bar covers the player, in pixels; read only in layout, so scrolling does not recompose.
    var coveredPx by remember { mutableIntStateOf(0) }
    // A new frame for each video, so a player never keeps another video's state.
    key(video.youTubeId) {
        WebElementView(
            factory = {
                (document.createElement("iframe") as HTMLIFrameElement).apply {
                    src = video.embedUrl
                    setAttribute("title", title)
                    setAttribute("allow", "autoplay; encrypted-media; picture-in-picture; fullscreen")
                    setAttribute("referrerpolicy", "strict-origin-when-cross-origin")
                    setAttribute(
                        "sandbox",
                        "allow-scripts allow-same-origin allow-presentation allow-popups " +
                            "allow-popups-to-escape-sandbox",
                    )
                    style.border = "0"
                    style.width = "100%"
                    style.height = "100%"
                }
            },
            modifier =
                modifier
                    .onGloballyPositioned { coordinates ->
                        val top = coordinates.positionInWindow().y.roundToInt()
                        coveredPx = (coveredTopPx - top).coerceIn(0, coordinates.size.height)
                    }.belowCoveredTop { coveredPx }
                    .fillMaxSize(),
            // Clearing the address stops playback at once, also while the element waits for removal.
            onRelease = { frame -> frame.src = "about:blank" },
        )
    }
}

/**
 * Keeps this element's size and place but clips it to the part below the top [coveredPx] pixels: a clipping window
 * that starts there, with the element placed back up inside it.
 */
private fun Modifier.belowCoveredTop(coveredPx: () -> Int): Modifier =
    layout { measurable, constraints ->
        val cut = coveredPx().coerceIn(0, constraints.maxHeight)
        val visibleHeight = constraints.maxHeight - cut
        val window = measurable.measure(constraints.copy(minHeight = visibleHeight, maxHeight = visibleHeight))
        layout(constraints.maxWidth, constraints.maxHeight) { window.place(0, cut) }
    }.clipToBounds()
        .layout { measurable, constraints ->
            // The window's height plus the part above it: the element's own full height.
            val fullHeight = constraints.maxHeight + coveredPx().coerceAtLeast(0)
            val element = measurable.measure(constraints.copy(minHeight = fullHeight, maxHeight = fullHeight))
            layout(constraints.maxWidth, constraints.maxHeight) { element.place(0, constraints.maxHeight - fullHeight) }
        }
