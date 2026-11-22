// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import dev.vladleesi.braindanceapp.ui.style.Dimens
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.math.roundToInt

/**
 * YouTube's privacy-enhanced embed in an iframe that Compose lays over the canvas. index.html's content security
 * policy allows frames only from www.youtube-nocookie.com. The sandbox lets the player run its scripts, enter
 * fullscreen, and open YouTube in a new tab, but not navigate the app's page. The page's referrer policy sends YouTube
 * only the app's origin, which its player requires to identify the embedding site. A video that cannot play shows
 * YouTube's own message with a link to YouTube.
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
    state: MediaGalleryState,
    scrollState: ScrollState?,
) {
    video ?: return
    val density = LocalDensity.current
    val coveredTopPx = with(density) { coveredTop.roundToPx() }
    val lineHeightPx = with(density) { Dimens.medium.toPx().toDouble() }
    val onWheel by rememberUpdatedState<(Event) -> Unit>({ event ->
        scrollState?.let { scroll ->
            val delta = wheelDelta(event, density.density.toDouble(), lineHeightPx, scroll.viewportSize).toFloat()
            if (delta != 0f && scroll.dispatchRawDelta(delta) != 0f) event.preventDefault()
        }
    })
    val onControlScroll by rememberUpdatedState<(Double) -> Unit>({ delta ->
        scrollState?.dispatchRawDelta((delta * density.density).toFloat())
    })
    // How far the bar covers the player, in pixels; read only in layout, so scrolling does not recompose.
    var coveredPx by remember { mutableIntStateOf(0) }
    // A new frame for each video, so a player never keeps another video's state.
    key(video.youTubeId) {
        val player =
            remember(state) {
                EmbeddedPlayer(
                    video = video,
                    title = title,
                    onWheel = { event -> onWheel(event) },
                    onControlScroll = { delta -> onControlScroll(delta) },
                    onStartSettled = { state.onPlaybackStartingChanged(video, starting = false) },
                )
            }
        DisposableEffect(player) {
            state.onPlaybackStartingChanged(video, starting = true)
            onDispose { state.onPlaybackStartingChanged(video, starting = false) }
        }
        HtmlElementView(
            factory = { player.element },
            modifier =
                Modifier
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
private const val PAUSED_HEADER_HEIGHT_PX = 48
private const val CONTROL_SCROLL_CENTER_PX = 16_384
private const val CONTROL_SCROLL_RANGE_PX = CONTROL_SCROLL_CENTER_PX * 2
private const val CONTROL_SCROLL_EDGE_PX = 1_024
private const val LISTENING_INTERVAL_MILLIS = 250
private const val LISTENING_ATTEMPTS = 40

// How long the shield waits for the player to answer before stepping aside, so a player whose messages never arrive
// still takes clicks.
private const val LOADING_TIMEOUT_MILLIS = 10_000

// Give a ready player time to start, including browsers that block autoplay without sending the event.
private const val START_TIMEOUT_MILLIS = 1_500
private const val BLANK_PAGE = "about:blank"
private const val DISPLAY_SHOWN = "block"
private const val DISPLAY_HIDDEN = "none"
private val PlayCommand = embedCommand("playVideo")

/**
 * The frame keeps wheel events within its own page. A transparent shield sends wheel deltas over the picture to the
 * details page while the video starts, plays, or is paused. Clicks over the picture pause or resume started videos.
 * YouTube's bottom controls and paused header stay available, and the shield steps aside for the end screen or errors.
 * The browser chains scrolls from the frame to the wrapper. The picture stays fixed within the wrapper while those
 * deltas move the details page, so YouTube's own controls keep receiving pointer input.
 *
 * The page learns whether the video plays through YouTube's message channel, accepting messages only from this frame
 * and YouTube's origin, and reading nothing but the player's state. If the player never answers, the shield steps
 * aside after [LOADING_TIMEOUT_MILLIS], leaving YouTube's player as it would be without the shield.
 */
private class EmbeddedPlayer(
    video: GameMedia.YouTubeVideo,
    title: String,
    private val onWheel: (Event) -> Unit,
    private val onControlScroll: (Double) -> Unit,
    private val onStartSettled: () -> Unit,
) {
    private val frame =
        (document.createElement("iframe") as HTMLIFrameElement).apply {
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
            src = video.webEmbedUrl(window.location.origin)
        }

    private val shield =
        (document.createElement("div") as HTMLDivElement).apply {
            setAttribute("aria-hidden", "true")
            style.position = "absolute"
            style.setProperty("inset", "0 0 ${CONTROLS_HEIGHT_PX}px 0")
        }

    private val picture =
        (document.createElement("div") as HTMLDivElement).apply {
            style.position = "sticky"
            style.top = "0"
            style.height = "calc(100% - ${CONTROL_SCROLL_RANGE_PX}px)"
            appendChild(frame)
            appendChild(shield)
        }

    val element: HTMLDivElement =
        (document.createElement("div") as HTMLDivElement).apply {
            style.width = "100%"
            style.height = "100%"
            // Clip the frame and its wheel shield to the same corners as the Compose media preview.
            style.borderRadius = "${Dimens.small.value}px"
            style.overflowX = "hidden"
            style.overflowY = "auto"
            style.setProperty("scrollbar-width", "none")
            style.setProperty("overscroll-behavior", "contain")
            appendChild(
                (document.createElement("div") as HTMLDivElement).apply {
                    style.height = "calc(100% + ${CONTROL_SCROLL_RANGE_PX}px)"
                    appendChild(picture)
                },
            )
        }

    private var controlScrollTop = 0.0

    private fun centerControlScroll() {
        element.scrollTop = CONTROL_SCROLL_CENTER_PX.toDouble()
        controlScrollTop = element.scrollTop
    }

    private val onScroll: (Event) -> Unit = {
        val top = element.scrollTop
        val delta = top - controlScrollTop
        if (delta != 0.0) {
            controlScrollTop = top
            onControlScroll(delta)
            // Keep headroom in both directions without resetting the native scroller on each event.
            if (top < CONTROL_SCROLL_EDGE_PX || top > CONTROL_SCROLL_RANGE_PX - CONTROL_SCROLL_EDGE_PX) {
                centerControlScroll()
            }
        }
    }

    private var listeningTimer: Int? = null
    private var listeningAttempts = 0

    // Whether the video is still starting: it has not played yet, and the player has not been given up on.
    private var loading = true
    private var picturePlayback: EmbedEvent? = null

    // Whether the player has begun loading the video, which shows that it is starting.
    private var buffered = false
    private var requestedPlay = false
    private var retriedMuted = false
    private var loadingTimer: Int? = window.setTimeout({ giveUpStarting() }, LOADING_TIMEOUT_MILLIS)

    private val onMessage: (Event) -> Unit = { event ->
        val message = event as MessageEvent
        if (message.origin == YOUTUBE_EMBED_ORIGIN && isFromFrame(message, frame)) {
            stringData(message)?.let(::embedEventOf)?.let(::onPlayerEvent)
        }
    }

    private fun onPlayerEvent(event: EmbedEvent) {
        if (listeningTimer != null) {
            stopListening()
            for (name in listOf("onStateChange", "onAutoplayBlocked", "onError")) {
                send(embedCommand("addEventListener", name))
            }
        }
        when (event) {
            EmbedEvent.READY -> requestPlay()
            EmbedEvent.AUTOPLAY_BLOCKED -> if (loading) retryMuted() else updatePicturePlayback(null)
            EmbedEvent.FAILED -> stopLoading()
            EmbedEvent.PLAYING -> stopLoading(EmbedEvent.PLAYING)
            EmbedEvent.PAUSED -> if (picturePlayback != null) updatePicturePlayback(event) else requestPlay()
            EmbedEvent.BUFFERING -> onBuffering()
            EmbedEvent.STOPPED -> if (loading) requestPlay() else updatePicturePlayback(null)
        }
    }

    private fun requestPlay() {
        // An already-ready iframe can first report an unstarted state, without replaying onReady.
        if (!loading || requestedPlay || buffered) return
        requestedPlay = true
        send(PlayCommand)
        restartLoadingTimer(START_TIMEOUT_MILLIS, ::retryMuted)
    }

    private fun retryMuted() {
        if (!loading) return
        if (retriedMuted) {
            stopLoading()
            return
        }
        retriedMuted = true
        requestedPlay = true
        buffered = false
        send(embedCommand("mute"))
        send(PlayCommand)
        restartLoadingTimer(START_TIMEOUT_MILLIS)
    }

    private fun onBuffering() {
        if (picturePlayback != null) {
            updatePicturePlayback(EmbedEvent.BUFFERING)
        } else if (loading && !buffered) {
            buffered = true
            restartLoadingTimer(LOADING_TIMEOUT_MILLIS)
        }
    }

    // The player subscribes the page once it has loaded; until it answers, the page asks again.
    private val onLoad: (Event) -> Unit = {
        centerControlScroll()
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

    private val onClick: (Event) -> Unit = {
        when (picturePlayback) {
            EmbedEvent.PAUSED -> send(PlayCommand)
            EmbedEvent.PLAYING, EmbedEvent.BUFFERING -> send(embedCommand("pauseVideo"))
            else -> Unit
        }
    }

    init {
        window.addEventListener("message", onMessage)
        frame.addEventListener("load", onLoad)
        element.addEventListener("scroll", onScroll)
        shield.addEventListener("wheel", onWheel)
        shield.addEventListener("click", onClick)
    }

    /** Stops playback at once, also while the element waits for removal, and stops listening to the player. */
    fun release() {
        stopListening()
        loadingTimer?.let(window::clearTimeout)
        window.removeEventListener("message", onMessage)
        frame.removeEventListener("load", onLoad)
        element.removeEventListener("scroll", onScroll)
        shield.removeEventListener("wheel", onWheel)
        shield.removeEventListener("click", onClick)
        frame.src = BLANK_PAGE
    }

    private fun send(message: String) {
        frame.contentWindow?.postMessage(message.toJsString(), YOUTUBE_EMBED_ORIGIN)
    }

    private fun stopLoading(playback: EmbedEvent? = null) {
        loadingTimer?.let(window::clearTimeout)
        loadingTimer = null
        if (loading) {
            loading = false
            onStartSettled()
        }
        updatePicturePlayback(playback)
    }

    private fun updatePicturePlayback(playback: EmbedEvent?) {
        if (picturePlayback != playback || shield.style.display.isEmpty()) {
            picturePlayback = playback
            shield.style.display = if (playback == null) DISPLAY_HIDDEN else DISPLAY_SHOWN
            shield.style.top = if (playback == EmbedEvent.PAUSED) "${PAUSED_HEADER_HEIGHT_PX}px" else "0"
        }
    }

    private fun giveUpStarting(): JsAny? {
        stopLoading()
        return null
    }

    private fun restartLoadingTimer(
        millis: Int,
        onTimeout: () -> Unit = { stopLoading() },
    ) {
        loadingTimer?.let(window::clearTimeout)
        loadingTimer =
            window.setTimeout(
                {
                    onTimeout()
                    null
                },
                millis,
            )
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

/** Converts a wheel event's pixel, line or page delta into Compose pixels. Ctrl-wheel stays with browser zoom. */
private fun wheelDelta(
    event: Event,
    pixelScale: Double,
    lineHeight: Double,
    pageHeight: Int,
): Double =
    js(
        """event.ctrlKey ? 0 :
        event.deltaY * (event.deltaMode === 1 ? lineHeight : event.deltaMode === 2 ? pageHeight : pixelScale)""",
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
