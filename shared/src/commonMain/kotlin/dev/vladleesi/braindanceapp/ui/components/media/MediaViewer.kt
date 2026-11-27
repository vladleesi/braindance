// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import ChevronLeft
import X
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.navigation.BrowserBackEntry
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_media_close
import dev.vladleesi.braindanceapp.resources.game_media_exit_window_fullscreen
import dev.vladleesi.braindanceapp.resources.game_media_fill
import dev.vladleesi.braindanceapp.resources.game_media_fill_description
import dev.vladleesi.braindanceapp.resources.game_media_fit
import dev.vladleesi.braindanceapp.resources.game_media_fit_description
import dev.vladleesi.braindanceapp.resources.game_media_next
import dev.vladleesi.braindanceapp.resources.game_media_position
import dev.vladleesi.braindanceapp.resources.game_media_previous
import dev.vladleesi.braindanceapp.resources.game_media_screenshot_of_game
import dev.vladleesi.braindanceapp.resources.game_media_unavailable
import dev.vladleesi.braindanceapp.resources.game_media_viewer
import dev.vladleesi.braindanceapp.resources.game_media_window_fullscreen
import dev.vladleesi.braindanceapp.system.hasTouchScreen
import dev.vladleesi.braindanceapp.ui.components.TonalButton
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.black
import dev.vladleesi.braindanceapp.ui.style.icons.Expand
import dev.vladleesi.braindanceapp.ui.style.icons.Shrink
import dev.vladleesi.braindanceapp.ui.style.onMedia
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest
import dev.vladleesi.braindanceapp.utils.shimmerEffect
import dev.vladleesi.braindanceapp.utils.toContentDescription
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

// Nearly black: the screenshot stands out, and a trace of the page shows that closing returns to it.
private const val BACKDROP_ALPHA = 0.94f

// Zoomed in further than this, a screenshot loads IGDB's original file for detail; until then the fitted size
// is enough.
private const val DETAIL_SCALE = 1.5f
private val TopBarHeight = 56.dp

// The side gutters that hold the previous and next buttons, so they never cover the screenshot.
private val ArrowGutter = 56.dp

/**
 * The full-screen viewer for [state]'s screenshots, starting at the selected one, drawn above the whole app (see
 * [dev.vladleesi.braindanceapp.ui.components.FullWindowOverlay]). Videos play only in the gallery's preview, so the
 * viewer has none. It shares [state] with the gallery, so closing it leaves the gallery on the screenshot viewed last.
 *
 * Screenshots fit the screen and can be zoomed and panned, filled to cover the screen, and zoomed back out to the
 * whole image. Touch screens swipe between them; pointer devices get previous and next buttons. Esc, the system Back,
 * or the browser's Back closes it; arrow keys, Home, and End move between screenshots, and +, −, and 0 zoom. On the
 * Web it can also enter the browser's fullscreen. Keyboard focus stays in the viewer while it is open.
 */
@Composable
fun MediaViewer(
    state: MediaGalleryState,
    gameName: String,
    modifier: Modifier = Modifier,
) {
    val screenshots = state.gallery.screenshots
    if (screenshots.isEmpty()) return
    val close = state::closeViewer
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = close,
    )
    BrowserBackEntry(onBack = close)
    val windowFullscreen = rememberWindowFullscreen()
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { focus.requestFocusSafely() }
    // One zoom per screenshot, which keeps the sizes its image and page reported; moving to another one fits the rest.
    val zoomStates = remember(state) { mutableMapOf<GameMedia.Screenshot, ZoomState>() }
    val zoomFor: (GameMedia.Screenshot) -> ZoomState = { screenshot -> zoomStates.getOrPut(screenshot) { ZoomState() } }
    val current = (state.selected as? GameMedia.Screenshot) ?: screenshots.first()
    val zoom = zoomFor(current)
    LaunchedEffect(current) {
        zoomStates.forEach { (screenshot, other) -> if (screenshot != current) other.reset() }
    }
    val pagerState = rememberPagerState(initialPage = state.selectedScreenshotIndex ?: 0) { screenshots.size }
    PagerFollowsSelection(state, pagerState)
    val paneTitle = stringResource(Res.string.game_media_viewer, gameName)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(black.copy(alpha = BACKDROP_ALPHA))
                // Takes every press, so nothing under the viewer reacts.
                .pointerInput(Unit) {}
                .onPreviewKeyEvent { event -> event.handleViewerKey(state, zoom) }
                .semantics {
                    this.paneTitle = paneTitle
                    isTraversalGroup = true
                }.focusProperties { onExit = { cancelFocusChange() } }
                .focusRequester(focus)
                .focusable(),
    ) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ViewerTopBar(state, zoom, gameName, windowFullscreen, onClose = close)
            ViewerContent(
                state = state,
                zoomFor = zoomFor,
                userScrollEnabled = !zoom.isZoomed,
                pagerState = pagerState,
                gameName = gameName,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

/** Keeps [pagerState] on [state]'s selected screenshot, and the selection on the page a swipe settles on. */
@Composable
private fun PagerFollowsSelection(
    state: MediaGalleryState,
    pagerState: PagerState,
) {
    LaunchedEffect(state, pagerState) {
        snapshotFlow { pagerState.settledPage }.collect(state::selectScreenshot)
    }
    LaunchedEffect(state, pagerState) {
        snapshotFlow { state.selectedScreenshotIndex }.collect { index ->
            if (index == null || index == pagerState.currentPage || pagerState.isScrollInProgress) return@collect
            // A neighbor slides in; a distant one, after Home or End, appears without sliding past the rest.
            scrollUnlessInterrupted {
                if (abs(index - pagerState.currentPage) == 1) {
                    pagerState.animateScrollToPage(index)
                } else {
                    pagerState.scrollToPage(index)
                }
            }
        }
    }
}

private fun KeyEvent.handleViewerKey(
    state: MediaGalleryState,
    zoom: ZoomState,
): Boolean {
    if (type != KeyEventType.KeyDown) return false
    when (key) {
        // Android's Back key reaches the focused viewer first, where Compose would otherwise spend it on clearing
        // focus, which the viewer keeps; the Back gesture still arrives through the back handler.
        Key.Escape, Key.Back -> state.closeViewer()
        Key.DirectionLeft -> state.selectPrevious()
        Key.DirectionRight -> state.selectNext()
        Key.MoveHome -> state.selectScreenshot(0)
        Key.MoveEnd -> state.select(state.gallery.items.lastIndex)
        Key.Plus, Key.Equals, Key.NumPadAdd -> zoom.zoomBy(KEY_ZOOM_STEP)
        Key.Minus, Key.NumPadSubtract -> zoom.zoomBy(1f / KEY_ZOOM_STEP)
        Key.Zero, Key.NumPad0 -> zoom.reset()
        else -> return false
    }
    return true
}

@Composable
private fun ViewerTopBar(
    state: MediaGalleryState,
    zoom: ZoomState,
    gameName: String,
    windowFullscreen: WindowFullscreen?,
    onClose: () -> Unit,
) {
    val index = state.selectedScreenshotIndex ?: return
    Row(
        modifier = Modifier.fillMaxWidth().height(TopBarHeight).padding(horizontal = Dimens.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        MediaIconButton(icon = X, description = stringResource(Res.string.game_media_close), onClick = onClose)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.game_media_position, index + 1, state.gallery.screenshots.size),
                style = MaterialTheme.typography.subtitle2,
                color = onMedia,
                maxLines = 1,
            )
            Text(
                text = gameName,
                style = MaterialTheme.typography.caption,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Fill matters only when the screenshot's shape differs visibly from the screen's.
        if (zoom.canFill) FillToggle(zoom)
        windowFullscreen?.let { fullscreen ->
            MediaIconButton(
                icon = if (fullscreen.isActive) Shrink else Expand,
                description =
                    stringResource(
                        if (fullscreen.isActive) {
                            Res.string.game_media_exit_window_fullscreen
                        } else {
                            Res.string.game_media_window_fullscreen
                        },
                    ),
                onClick = { if (fullscreen.isActive) fullscreen.exit() else fullscreen.enter() },
            )
        }
    }
}

/** Switches between the whole screenshot and one filling the screen; filled, it can still be panned. */
@Composable
private fun FillToggle(zoom: ZoomState) {
    val filled = zoom.isZoomed
    val description =
        stringResource(if (filled) Res.string.game_media_fit_description else Res.string.game_media_fill_description)
    TonalButton(onClick = zoom::toggleFill, modifier = Modifier.semantics { contentDescription = description }) {
        Text(
            text = stringResource(if (filled) Res.string.game_media_fit else Res.string.game_media_fill),
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ViewerContent(
    state: MediaGalleryState,
    zoomFor: (GameMedia.Screenshot) -> ZoomState,
    userScrollEnabled: Boolean,
    pagerState: PagerState,
    gameName: String,
    modifier: Modifier,
) {
    val screenshots = state.gallery.screenshots
    val arrows = remember { !hasTouchScreen() } && screenshots.size > 1
    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(horizontal = if (arrows) ArrowGutter else 0.dp),
            key = { page -> screenshots[page].key },
            // Only the screenshot on screen loads; a neighbor composes only while a swipe shows it.
            beyondViewportPageCount = 0,
            // A zoomed screenshot pans instead.
            userScrollEnabled = userScrollEnabled,
        ) { page ->
            val screenshot = screenshots[page]
            ZoomableScreenshot(
                screenshot = screenshot,
                zoom = zoomFor(screenshot),
                description =
                    stringResource(Res.string.game_media_screenshot_of_game, gameName, page + 1, screenshots.size),
            )
        }
        if (arrows) {
            MediaIconButton(
                icon = ChevronLeft,
                description = stringResource(Res.string.game_media_previous),
                enabled = state.canSelectPrevious,
                onClick = state::selectPrevious,
                modifier = Modifier.align(Alignment.CenterStart).padding(Dimens.small),
            )
            MediaIconButton(
                icon = ChevronLeft,
                description = stringResource(Res.string.game_media_next),
                enabled = state.canSelectNext,
                onClick = state::selectNext,
                mirrored = true,
                modifier = Modifier.align(Alignment.CenterEnd).padding(Dimens.small),
            )
        }
    }
}

/**
 * A screenshot fitted to the screen, zoomable and pannable through [zoom]. It loads IGDB's size for the screen's
 * width, showing the strip's thumbnail of it until then, and IGDB's original file only once zoomed in past
 * [DETAIL_SCALE], decoded at its full size for the detail; the fitted image stays under it until it arrives.
 */
@Composable
private fun ZoomableScreenshot(
    screenshot: GameMedia.Screenshot,
    zoom: ZoomState,
    description: String,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { size -> zoom.containerSize = size.toSize() }
                .zoomGestures(zoom)
                .semantics { contentDescription = description.toContentDescription() },
        contentAlignment = Alignment.Center,
    ) {
        val url = screenshot.previewUrl(constraints.maxWidth)
        var loaded by remember(url) { mutableStateOf(false) }
        var failed by remember(url) { mutableStateOf(false) }
        var detailRequested by remember(screenshot) { mutableStateOf(false) }
        if (zoom.scale > DETAIL_SCALE) detailRequested = true
        if (!loaded && !failed) Box(modifier = Modifier.fillMaxSize().shimmerEffect())
        if (failed) {
            MediaMessage(text = stringResource(Res.string.game_media_unavailable))
            return@BoxWithConstraints
        }
        AsyncImage(
            model =
                rememberCachedImageRequest(
                    data = url,
                    memoryCacheKey = "gmf-$url",
                    placeholderMemoryCacheKey = thumbnailMemoryCacheKey(screenshot),
                    crossfade = false,
                ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().zoomed(zoom),
            onSuccess = { success ->
                loaded = true
                zoom.imageSize = success.painter.intrinsicSize
            },
            onError = { failed = true },
        )
        if (detailRequested) {
            // Transparent until it loads, so the fitted image shows until then; a failure leaves it.
            AsyncImage(
                model =
                    rememberCachedImageRequest(
                        data = screenshot.originalUrl,
                        memoryCacheKey = "gmo-${screenshot.originalUrl}",
                        crossfade = false,
                        keepOriginalSize = true,
                    ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().zoomed(zoom),
            )
        }
    }
}

private const val PREVIEW_SCREENSHOT_COUNT = 3

private val PreviewGallery =
    MediaGallery(
        videos = listOf(GameMedia.YouTubeVideo(youTubeId = "aaaaaaaaaaa")),
        screenshots =
            List(PREVIEW_SCREENSHOT_COUNT) { index ->
                GameMedia.Screenshot(thumbUrl = "https://images.igdb.com/igdb/image/upload/t_thumb/sample$index.jpg")
            },
    )

@Preview(widthDp = 400, heightDp = 800)
@Composable
internal fun MediaViewerPreview() {
    BraindanceTheme {
        MediaViewer(
            state = remember { MediaGalleryState(PreviewGallery, initialIndex = 1, initialViewerOpen = true) },
            gameName = "Sample game",
        )
    }
}
