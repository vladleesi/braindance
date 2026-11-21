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
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import kotlin.math.roundToInt

/**
 * YouTube's privacy-enhanced embed in an iframe that Compose lays over the canvas. index.html's content security
 * policy allows frames only from www.youtube-nocookie.com. The sandbox lets the player run its scripts, enter
 * fullscreen, and open YouTube in a new tab, but not navigate the app's page. The page's referrer policy sends YouTube
 * only the app's origin, which its player requires to identify the embedding site. A video that cannot play shows
 * YouTube's own message with a link to YouTube, so [onFailed] is not needed here.
 *
 * Unlike Android and iOS, the Web gets a new iframe for each video and none while stopped: changing an iframe's address
 * would add an entry to the browser's history, so Back would step through videos instead of pages. Removing a
 * browser element on its own is safe here.
 *
 * The browser draws the frame above the canvas, so Compose content drawn over it, such as a top bar the page scrolls
 * under, would be hidden. Compose cuts the frame off at its ancestors' clips, so the player sits in a clipping window
 * that starts below [coveredTop]. The frame is another site's page, so the mouse wheel over it never reaches the app;
 * see [EmbeddedPlayer] for how the page still scrolls there.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun YouTubePlayer(
    video: GameMedia.YouTubeVideo?,
    title: String,
    coveredTop: Dp,
    onFailed: (GameMedia.YouTubeVideo) -> Unit,
    modifier: Modifier,
) {
    video ?: return
    val coveredTopPx = with(LocalDensity.current) { coveredTop.roundToPx() }
    // How far the bar covers the player, in pixels; read only in layout, so scrolling does not recompose.
    var coveredPx by remember { mutableIntStateOf(0) }
    // A new frame for each video, so a player never keeps another video's state.
    key(video.youTubeId) {
        val player = remember { EmbeddedPlayer(video, title) }
        HtmlElementView(
            factory = { player.element },
            modifier =
                modifier
                    .onGloballyPositioned { coordinates ->
                        val top = coordinates.positionInWindow().y.roundToInt()
                        coveredPx = (coveredTopPx - top).coerceIn(0, coordinates.size.height)
                    }.belowCoveredTop { coveredPx }
                    .fillMaxSize(),
            onRelease = { player.release() },
        )
    }
}

// The bottom of the player that its controls take: the progress bar and the buttons under it.
private const val CONTROLS_HEIGHT_PX = 60
private const val LISTENING_INTERVAL_MILLIS = 250
private const val LISTENING_ATTEMPTS = 40

// How long the shield waits for the player to answer before stepping aside, so a player whose messages never arrive
// still takes clicks.
private const val LOADING_TIMEOUT_MILLIS = 10_000

// How long a ready player may take to start before the page takes it as blocked from playing by itself, as a
// browser's autoplay policy can, and leaves YouTube's own play button to the user.
private const val START_TIMEOUT_MILLIS = 3_000
private const val BLANK_PAGE = "about:blank"
private const val DISPLAY_SHOWN = "block"
private const val DISPLAY_HIDDEN = "none"

/**
 * One video's embedded player: the frame, and a transparent shield over its picture while the video loads and plays,
 * which leaves YouTube's controls at the bottom uncovered.
 *
 * Like a page that embeds a video, the app should scroll when the wheel turns over a playing video, but the frame
 * keeps the wheel to itself. The shield takes it instead and passes it on to the app's canvas, and a click on it pauses
 * the video, as a click on the picture does in YouTube's player; once paused, the shield is gone, so YouTube's own
 * buttons and links over the picture work. While the video loads, which the first time can take seconds, a click on
 * it does nothing, so a second click does not stop the video the user just started. When the player is ready it is
 * asked to play, as on Android and iOS, in case it did not start by itself.
 *
 * The page learns whether the video plays through YouTube's message channel, accepting messages only from this frame
 * and YouTube's origin, and reading nothing but the player's state. If the player never answers, the shield steps
 * aside after [LOADING_TIMEOUT_MILLIS], leaving YouTube's player as it would be without the shield.
 */
private class EmbeddedPlayer(
    video: GameMedia.YouTubeVideo,
    title: String,
) {
    private val frame =
        (document.createElement("iframe") as HTMLIFrameElement).apply {
            src = video.webEmbedUrl(window.location.origin)
            setAttribute("title", title)
            setAttribute("allow", "autoplay; encrypted-media; picture-in-picture; fullscreen")
            setAttribute("referrerpolicy", "strict-origin-when-cross-origin")
            setAttribute(
                "sandbox",
                "allow-scripts allow-same-origin allow-presentation allow-popups allow-popups-to-escape-sandbox",
            )
            style.border = "0"
            style.width = "100%"
            style.height = "100%"
            style.display = DISPLAY_SHOWN
        }

    private val shield =
        (document.createElement("div") as HTMLDivElement).apply {
            setAttribute("aria-hidden", "true")
            style.position = "absolute"
            style.setProperty("inset", "0 0 ${CONTROLS_HEIGHT_PX}px 0")
        }

    val element: HTMLDivElement =
        (document.createElement("div") as HTMLDivElement).apply {
            style.position = "relative"
            style.width = "100%"
            style.height = "100%"
            appendChild(frame)
            appendChild(shield)
        }

    private var listeningTimer: Int? = null
    private var listeningAttempts = 0

    // Whether the video is still starting: it has not played yet, and the player has not been given up on.
    private var loading = true

    // Whether the player has begun loading the video, which shows that it is starting.
    private var buffered = false
    private var loadingTimer: Int? = window.setTimeout({ giveUpStarting() }, LOADING_TIMEOUT_MILLIS)

    private val onMessage: (Event) -> Unit = { event ->
        val message = event as MessageEvent
        val text = stringData(message)
        if (message.origin == YOUTUBE_EMBED_ORIGIN && isFromFrame(message, frame) && text != null) {
            if (listeningTimer != null) {
                stopListening()
                // The player reports state changes only to a page that asks for them.
                send(embedCommand("addEventListener", "onStateChange"))
            }
            when (embedEventOf(text)) {
                EmbedEvent.READY -> {
                    send(embedCommand("playVideo"))
                    if (loading && !buffered) restartLoadingTimer(START_TIMEOUT_MILLIS)
                }
                EmbedEvent.PLAYING -> stopLoading(playing = true)
                // Loading the video counts as starting it, however long it takes, up to the time limit; once it has
                // played, waiting for data counts as playing.
                EmbedEvent.BUFFERING -> {
                    buffered = true
                    if (loading) restartLoadingTimer(LOADING_TIMEOUT_MILLIS) else showShield()
                }
                // A starting player reports that it has not started yet; only a pause or an end stops it.
                EmbedEvent.STOPPED -> if (!loading) hideShield()
                null -> Unit
            }
        }
    }

    // The player subscribes the page once it has loaded; until it answers, the page asks again.
    private val onLoad: (Event) -> Unit = {
        stopListening()
        listeningAttempts = 0
        send(EMBED_LISTENING_MESSAGE)
        listeningTimer =
            window.setInterval(
                {
                    listeningAttempts++
                    if (listeningAttempts >= LISTENING_ATTEMPTS) stopListening() else send(EMBED_LISTENING_MESSAGE)
                    null
                },
                LISTENING_INTERVAL_MILLIS,
            )
    }

    // Compose's canvas, found on the first wheel turn; the app keeps one canvas for its whole life.
    private var canvas: EventTarget? = null

    private val onWheel: (Event) -> Unit = { event ->
        val target = canvas ?: composeCanvasNear(element)?.also { canvas = it }
        if (target != null) forwardWheel(event, target)
    }

    // A click on a starting video does nothing, so it cannot stop the video the user just started.
    private val onClick: (Event) -> Unit = {
        if (!loading) {
            hideShield()
            send(embedCommand("pauseVideo"))
        }
    }

    init {
        window.addEventListener("message", onMessage)
        frame.addEventListener("load", onLoad)
        shield.addEventListener("wheel", onWheel)
        shield.addEventListener("click", onClick)
    }

    /** Stops playback at once, also while the element waits for removal, and stops listening to the player. */
    fun release() {
        stopListening()
        loadingTimer?.let(window::clearTimeout)
        window.removeEventListener("message", onMessage)
        frame.removeEventListener("load", onLoad)
        shield.removeEventListener("wheel", onWheel)
        shield.removeEventListener("click", onClick)
        frame.src = BLANK_PAGE
    }

    private fun send(message: String) {
        frame.contentWindow?.postMessage(message.toJsString(), YOUTUBE_EMBED_ORIGIN)
    }

    /** Ends the start: the shield stays while the video [playing], and steps aside otherwise. */
    private fun stopLoading(playing: Boolean) {
        loadingTimer?.let(window::clearTimeout)
        loadingTimer = null
        loading = false
        if (playing) showShield() else hideShield()
    }

    private fun showShield() {
        shield.style.display = DISPLAY_SHOWN
    }

    private fun hideShield() {
        shield.style.display = DISPLAY_HIDDEN
    }

    private fun giveUpStarting(): JsAny? {
        stopLoading(playing = false)
        return null
    }

    private fun restartLoadingTimer(millis: Int) {
        loadingTimer?.let(window::clearTimeout)
        loadingTimer = window.setTimeout({ giveUpStarting() }, millis)
    }

    private fun stopListening() {
        listeningTimer?.let(window::clearInterval)
        listeningTimer = null
    }
}

/** [event]'s data when it is text, as YouTube's player posts it, or null. */
private fun stringData(event: MessageEvent): String? = js("typeof event.data === 'string' ? event.data : null")

/** Whether [event] was posted by the page in [frame]. */
private fun isFromFrame(
    event: MessageEvent,
    frame: HTMLIFrameElement,
): Boolean = js("event.source === frame.contentWindow")

/**
 * Compose's canvas for an element in its interop layer: the canvas is in an open shadow root whose host shares an
 * ancestor with the layer, so it is found by looking through the children of [from]'s ancestors.
 */
private fun composeCanvasNear(from: Element): EventTarget? =
    js(
        """(() => {
        for (let node = from.parentElement; node; node = node.parentElement) {
            for (const child of node.children) {
                const canvas = child.shadowRoot ? child.shadowRoot.querySelector('canvas') : null;
                if (canvas) return canvas;
            }
        }
        return null;
    })()""",
    )

/**
 * Passes a wheel [event] on to Compose's [canvas], which takes it as the wheel turning at the same point, and keeps
 * the browser from handling it too.
 */
private fun forwardWheel(
    event: Event,
    canvas: EventTarget,
): Unit =
    js(
        """{
        event.preventDefault();
        canvas.dispatchEvent(new WheelEvent('wheel', {
            deltaX: event.deltaX, deltaY: event.deltaY, deltaZ: event.deltaZ, deltaMode: event.deltaMode,
            clientX: event.clientX, clientY: event.clientY, screenX: event.screenX, screenY: event.screenY,
            ctrlKey: event.ctrlKey, shiftKey: event.shiftKey, altKey: event.altKey, metaKey: event.metaKey,
            buttons: event.buttons, bubbles: true, cancelable: true, composed: true
        }));
    }""",
    )

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
