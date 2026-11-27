// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import ChevronLeft
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.navigation.externalLink
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_media_next
import dev.vladleesi.braindanceapp.resources.game_media_open_viewer
import dev.vladleesi.braindanceapp.resources.game_media_play
import dev.vladleesi.braindanceapp.resources.game_media_position
import dev.vladleesi.braindanceapp.resources.game_media_previous
import dev.vladleesi.braindanceapp.resources.game_media_screenshot
import dev.vladleesi.braindanceapp.resources.game_media_screenshot_of_game
import dev.vladleesi.braindanceapp.resources.game_media_title
import dev.vladleesi.braindanceapp.resources.game_media_unavailable
import dev.vladleesi.braindanceapp.resources.game_media_video
import dev.vladleesi.braindanceapp.resources.game_media_video_loading
import dev.vladleesi.braindanceapp.resources.game_media_video_of_game
import dev.vladleesi.braindanceapp.resources.game_media_video_unavailable
import dev.vladleesi.braindanceapp.resources.game_media_watch_on_youtube
import dev.vladleesi.braindanceapp.resources.game_media_youtube
import dev.vladleesi.braindanceapp.system.hasTouchScreen
import dev.vladleesi.braindanceapp.system.isExtraLargeWindow
import dev.vladleesi.braindanceapp.system.windowHeight
import dev.vladleesi.braindanceapp.ui.components.ListScrollbar
import dev.vladleesi.braindanceapp.ui.components.TonalButton
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.FocusVisibleIndication
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.black
import dev.vladleesi.braindanceapp.ui.style.icons.Expand
import dev.vladleesi.braindanceapp.ui.style.icons.OpenInNew
import dev.vladleesi.braindanceapp.ui.style.icons.Play
import dev.vladleesi.braindanceapp.ui.style.onMedia
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith70Alpha
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import dev.vladleesi.braindanceapp.utils.placeholder
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest
import dev.vladleesi.braindanceapp.utils.shimmerEffect
import dev.vladleesi.braindanceapp.utils.toContentDescription
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.jetbrains.compose.resources.stringResource

// The preview's frame. Screenshots of another shape are fitted into it, so selecting one never moves the layout.
private const val MEDIA_ASPECT_RATIO = 16f / 9f

// The preview takes at most this share of the window's height, so on a landscape phone the gallery still fits on
// screen, while on a laptop it fills the page's main column instead of leaving a gap beside the cards next to it.
private const val MAX_PREVIEW_HEIGHT_FRACTION = 0.75f
private const val UNSELECTED_THUMBNAIL_ALPHA = 0.6f
private const val NEXT_ICON_ROTATION_DEGREES = 180f
private const val THUMBNAIL_CONTENT_TYPE = "media_thumbnail"

// Wider than the game page's main column ever gets beside its rail and side column, so the preview fills that column.
// The window's height can still limit it first.
private val MaxPreviewWidth = 1440.dp

// In extra-large windows the thumbnails move to a rail beside the preview, as tall as it, instead of a strip under it:
// the rail fills width that would otherwise stay empty, and the gallery stays short enough to see whole. The gallery
// also needs this much width for it, wherever it is placed.
private val SideRailMinWidth = 840.dp
private val RailWidth = 208.dp
private val StripThumbnailWidth = 112.dp
private val PlayButtonSize = 64.dp
private val PlayIconSize = 32.dp
private val ThumbnailPlayBadgeSize = 24.dp
private val ThumbnailPlayIconSize = 12.dp
private val ArrowButtonSize = 40.dp
private val ArrowIconSize = 24.dp
private val SourceIconSize = 14.dp
private val SwipeThreshold = 48.dp

/**
 * The game's videos and screenshots: a large preview of the selected item over a strip of thumbnails, or, from
 * [SideRailMinWidth], beside a rail of them. Selecting a thumbnail changes only the preview. Screenshots show at once,
 * fitted into the 16:9 frame; a video shows YouTube's poster and loads nothing else until the user presses play,
 * then plays in the preview with YouTube's controls; videos play only there. A screenshot's full-screen button opens
 * [state]'s viewer, which shows the screenshots. If a video cannot play, [onWatchOnYouTube] offers it on YouTube
 * instead.
 *
 * Only the selected item's full-size image loads; thumbnails load as they scroll into view, at a small size. Pointer
 * devices also get previous and next buttons on the preview, touch screens swipe it, and arrow keys move through the
 * thumbnails. The gallery hides itself when it is empty. Each image shows a shimmering skeleton until it arrives, and
 * while [placeholder], another version's media is loading and the preview and thumbnails are skeletons. When the
 * viewer closes, keyboard focus returns to the full-screen button.
 *
 * Like a carousel, the gallery spans the page's width and insets its content by [horizontalPadding], usually the page
 * gutter: the heading and preview line up with the page column, and the thumbnail strip scrolls through the padding,
 * so thumbnails slide out at the screen's edge. [coveredTop] is the height of a bar at the window's top that the page
 * scrolls under; a playing video is cut off below it (see [YouTubePlayer]).
 *
 * The thumbnails sit in a strip under the preview, or, with [thumbnailRail], in a rail beside it. The rail needs the
 * width of Material's extra-large windows: in a narrower window it would leave the preview too small next to it.
 */
@Composable
fun MediaGallery(
    state: MediaGalleryState,
    gameName: String,
    onWatchOnYouTube: (GameMedia.YouTubeVideo) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    horizontalPadding: Dp = 0.dp,
    coveredTop: Dp = 0.dp,
    scrollState: ScrollState? = null,
    thumbnailRail: Boolean = isExtraLargeWindow(),
) {
    if (state.gallery.isEmpty) return
    val viewerButtonFocus = remember { FocusRequester() }
    ReturnFocusAfterViewer(state, viewerButtonFocus)
    // A window of unknown height, as in an IDE preview, does not limit the preview.
    val windowHeight = windowHeight()
    val maxPreviewWidth =
        if (windowHeight > 0.dp) {
            min(MaxPreviewWidth, windowHeight * MAX_PREVIEW_HEIGHT_FRACTION * MEDIA_ASPECT_RATIO)
        } else {
            MaxPreviewWidth
        }
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = (maxWidth - horizontalPadding * 2).coerceAtLeast(0.dp)
        val hasThumbnails = state.gallery.items.size > 1
        val sideRail = thumbnailRail && hasThumbnails && availableWidth >= SideRailMinWidth
        val previewWidth =
            if (sideRail) {
                min(availableWidth - RailWidth - Dimens.small, maxPreviewWidth)
            } else {
                min(availableWidth, maxPreviewWidth)
            }
        val previewHeight = previewWidth / MEDIA_ASPECT_RATIO
        val previewModifier =
            Modifier
                .size(width = previewWidth, height = previewHeight)
                .placeholder(visible = placeholder, shape = CornerShapes.small)
        // The heading, the preview, and a rail keep to the page column; only the strip reaches into the padding.
        val inset =
            Modifier
                .padding(horizontal = horizontalPadding)
                .width(if (sideRail) previewWidth + Dimens.small + RailWidth else previewWidth)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
            MediaHeader(state = state, modifier = inset, placeholder = placeholder)
            // One layout for both arrangements, so the preview, and a video playing in it, stay in place when the
            // thumbnails move between the rail and the strip, as when a phone turns.
            MediaBody(
                sideRail = sideRail,
                horizontalPadding = horizontalPadding,
                preview = {
                    MediaPreview(
                        state = state,
                        gameName = gameName,
                        width = previewWidth,
                        coveredTop = coveredTop,
                        viewerButtonFocus = viewerButtonFocus,
                        onWatchOnYouTube = onWatchOnYouTube,
                        modifier = previewModifier,
                        placeholder = placeholder,
                        scrollState = scrollState,
                    )
                },
                thumbnails = {
                    if (hasThumbnails) {
                        MediaThumbnails(
                            state = state,
                            vertical = sideRail,
                            placeholder = placeholder,
                            modifier =
                                if (sideRail) {
                                    Modifier.size(width = RailWidth, height = previewHeight)
                                } else {
                                    Modifier.fillMaxWidth()
                                },
                            contentPadding =
                                if (sideRail) PaddingValues() else PaddingValues(horizontal = horizontalPadding),
                        )
                    }
                },
            )
        }
    }
}

/**
 * Places the [preview] at the page column's start, inset by [horizontalPadding], and the [thumbnails] beside it in a
 * [sideRail] or under it across the whole width. Each keeps its own size.
 */
@Composable
private fun MediaBody(
    sideRail: Boolean,
    horizontalPadding: Dp,
    preview: @Composable () -> Unit,
    thumbnails: @Composable () -> Unit,
) {
    Layout(contents = listOf(preview, thumbnails), modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val padding = horizontalPadding.roundToPx()
        val gap = Dimens.small.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val previewPlaceable = measurables[0].firstOrNull()?.measure(loose)
        val thumbnailsPlaceable = measurables[1].firstOrNull()?.measure(loose)
        val previewWidth = previewPlaceable?.width ?: 0
        val previewHeight = previewPlaceable?.height ?: 0
        val thumbnailsHeight = thumbnailsPlaceable?.height ?: 0
        val height =
            when {
                thumbnailsPlaceable == null -> previewHeight
                sideRail -> maxOf(previewHeight, thumbnailsHeight)
                else -> previewHeight + gap + thumbnailsHeight
            }
        layout(constraints.maxWidth, height) {
            previewPlaceable?.placeRelative(padding, 0)
            if (sideRail) {
                thumbnailsPlaceable?.placeRelative(padding + previewWidth + gap, 0)
            } else {
                thumbnailsPlaceable?.placeRelative(0, previewHeight + gap)
            }
        }
    }
}

/**
 * Moves keyboard focus back to the full-screen button, through [viewerButtonFocus], when [state]'s viewer closes after
 * keyboard use. After touch, focus stays cleared: on Android, Back would otherwise first clear it and seem to do
 * nothing.
 */
@Composable
private fun ReturnFocusAfterViewer(
    state: MediaGalleryState,
    viewerButtonFocus: FocusRequester,
) {
    val inputModeManager = LocalInputModeManager.current
    LaunchedEffect(state, viewerButtonFocus, inputModeManager) {
        var wasOpen = state.isViewerOpen
        snapshotFlow { state.isViewerOpen }.collect { open ->
            val keyboard = inputModeManager.inputMode == InputMode.Keyboard
            if (wasOpen && !open && keyboard) viewerButtonFocus.requestFocusSafely()
            wasOpen = open
        }
    }
}

/** The section's heading and, in a gallery of several items, the selected item's place in it. */
@Composable
private fun MediaHeader(
    state: MediaGalleryState,
    modifier: Modifier,
    placeholder: Boolean,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.game_media_title),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        val count = state.gallery.items.size
        if (state.playbackStarting && !placeholder) {
            Text(
                text = stringResource(Res.string.game_media_video_loading),
                style = MaterialTheme.typography.caption,
                color = secondaryText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else if (count > 1) {
            Text(
                text = stringResource(Res.string.game_media_position, state.selectedIndex + 1, count),
                style = MaterialTheme.typography.caption,
                color = secondaryText,
                // The thumbnails already announce their position; this repeats it for sighted users only.
                modifier =
                    Modifier.clearAndSetSemantics {}.placeholder(
                        visible = placeholder,
                        shape = CornerShapes.small,
                    ),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun MediaPreview(
    state: MediaGalleryState,
    gameName: String,
    width: Dp,
    coveredTop: Dp,
    viewerButtonFocus: FocusRequester,
    onWatchOnYouTube: (GameMedia.YouTubeVideo) -> Unit,
    modifier: Modifier,
    placeholder: Boolean,
    scrollState: ScrollState?,
) {
    val item = state.selected ?: return
    val position = state.gallery.positionInKind(state.selectedIndex)
    val widthPx = with(LocalDensity.current) { width.roundToPx() }
    val swipeThresholdPx = with(LocalDensity.current) { SwipeThreshold.toPx() }
    val hasArrows = remember { !hasTouchScreen() } && state.gallery.items.size > 1
    val playing = state.isPlaying
    val playingVideo = (item as? GameMedia.YouTubeVideo)?.takeIf { playing && !state.playbackFailed && !placeholder }
    // The player is created on the first play and then stays for as long as the preview does, stopped or playing
    // another video: a platform view removed on its own, while this box stays, can crash Compose on Android.
    var playerCreated by remember { mutableStateOf(false) }
    if (playingVideo != null && !playerCreated) SideEffect { playerCreated = true }
    Box(
        modifier =
            modifier
                .clip(CornerShapes.small)
                .background(black)
                .then(if (placeholder) Modifier else Modifier.swipeToSelect(state, swipeThresholdPx)),
    ) {
        if (!placeholder) {
            // Each item gets its own image state, so a slow image never shows under the next selection.
            key(item.key) {
                when (item) {
                    is GameMedia.Screenshot ->
                        ScreenshotPreview(
                            url = item.previewUrl(widthPx),
                            thumbnailKey = thumbnailMemoryCacheKey(item),
                            description =
                                stringResource(
                                    Res.string.game_media_screenshot_of_game,
                                    gameName,
                                    position.number,
                                    position.count,
                                ),
                        )

                    is GameMedia.YouTubeVideo ->
                        if (playing && state.playbackFailed) {
                            VideoUnavailable(onWatchOnYouTube = { onWatchOnYouTube(item) })
                        } else {
                            // Stays under a playing video, so the frame is never empty while the player loads.
                            VideoPoster(
                                video = item,
                                widthPx = widthPx,
                                // YouTube's standard poster letterboxes the 16:9 frame; cropping removes the bars.
                                contentScale = ContentScale.Crop,
                                playDescription =
                                    stringResource(Res.string.game_media_play, position.number, position.count),
                                onPlay = state::play,
                            )
                        }
                }
            }
        }
        if (playerCreated || playingVideo != null) {
            YouTubePlayer(
                video = playingVideo,
                title =
                    stringResource(
                        Res.string.game_media_video_of_game,
                        gameName,
                        position.number,
                        position.count,
                    ),
                coveredTop = coveredTop,
                state = state,
                scrollState = scrollState,
            )
        }
        if (!placeholder) {
            PreviewControls(
                state = state,
                item = item,
                arrows = hasArrows && !playing,
                viewerButtonFocus = viewerButtonFocus,
            )
        }
    }
}

/**
 * The preview's own controls: previous and next for pointer devices when [arrows], hidden while a video plays because
 * the player has its own controls and these would sit under it, and a screenshot's full-screen button.
 */
@Composable
private fun BoxScope.PreviewControls(
    state: MediaGalleryState,
    item: GameMedia,
    arrows: Boolean,
    viewerButtonFocus: FocusRequester,
) {
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
    // A screenshot opens full screen with a button of its own, in the corner photo viewers use, so a tap or a
    // swipe on the image never opens it by accident. Videos play only here, so they have none.
    if (item is GameMedia.Screenshot) {
        MediaIconButton(
            icon = Expand,
            description = stringResource(Res.string.game_media_open_viewer),
            onClick = state::openViewer,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.small)
                    .focusRequester(viewerButtonFocus),
        )
    }
}

/**
 * The screenshot fitted into the frame and a short message if it cannot load. Until it arrives, the preview shows the
 * strip's thumbnail of it, or a shimmer when that is not loaded either.
 */
@Composable
private fun ScreenshotPreview(
    url: String,
    thumbnailKey: String,
    description: String,
) {
    var loaded by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = description.toContentDescription() },
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded && !failed) Box(modifier = Modifier.fillMaxSize().shimmerEffect())
        if (failed) {
            MediaMessage(text = stringResource(Res.string.game_media_unavailable))
        } else {
            AsyncImage(
                // Its own memory key: the preview decodes at another size than any thumbnail or backdrop.
                model =
                    rememberCachedImageRequest(
                        data = url,
                        memoryCacheKey = previewMemoryCacheKey(url),
                        placeholderMemoryCacheKey = thumbnailKey,
                        // The 16:9 thumbnail and a screenshot of another shape cannot crossfade without a jump.
                        crossfade = false,
                    ),
                // The box's description names the screenshot.
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { loaded = true },
                onError = { failed = true },
            )
        }
    }
}

/**
 * YouTube's poster for [video] with a play button, as one control, drawn with [contentScale]. A missing HD poster
 * falls back to the standard one; if neither loads, the play button still works.
 */
@Composable
internal fun VideoPoster(
    video: GameMedia.YouTubeVideo,
    widthPx: Int,
    contentScale: ContentScale,
    playDescription: String,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var posterUrl by remember(video, widthPx) { mutableStateOf(video.posterUrl(widthPx)) }
    var loaded by remember(posterUrl) { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.small, Dimens.micro)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onPlay,
                ).semantics { contentDescription = playDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) Box(modifier = Modifier.fillMaxSize().shimmerEffect())
        AsyncImage(
            model =
                rememberCachedImageRequest(
                    data = posterUrl,
                    memoryCacheKey = "gmv-$posterUrl",
                    placeholderMemoryCacheKey = thumbnailMemoryCacheKey(video),
                    // YouTube's standard poster is 4:3, its thumbnail 16:9: no crossfade, so it does not jump.
                    crossfade = false,
                ),
            // The control's description names the video; the poster adds nothing to it.
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { loaded = true },
            onError = {
                if (posterUrl != video.fallbackPosterUrl) posterUrl = video.fallbackPosterUrl else loaded = true
            },
        )
        PlayCircle(size = PlayButtonSize, iconSize = PlayIconSize)
        SourceLabel(
            text = stringResource(Res.string.game_media_youtube),
            modifier = Modifier.align(Alignment.BottomStart).padding(Dimens.compact),
        )
    }
}

/** In place of a video that cannot play here, such as one whose owner does not allow embedding: a link to YouTube. */
@Composable
internal fun VideoUnavailable(
    onWatchOnYouTube: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.medium),
        verticalArrangement = Arrangement.spacedBy(Dimens.compact, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MediaMessage(text = stringResource(Res.string.game_media_video_unavailable))
        TonalButton(onClick = onWatchOnYouTube, modifier = Modifier.externalLink(onWatchOnYouTube)) {
            Text(text = stringResource(Res.string.game_media_watch_on_youtube))
            Icon(
                imageVector = OpenInNew,
                contentDescription = null,
                modifier = Modifier.padding(start = Dimens.tiny).size(SourceIconSize),
            )
        }
    }
}

@Composable
internal fun MediaMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        color = secondaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(Dimens.medium),
    )
}

@Composable
internal fun PlayCircle(
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size).background(overlayBlackWith70Alpha, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = Play, contentDescription = null, tint = onMedia, modifier = Modifier.size(iconSize))
    }
}

/** Where the video comes from. */
@Composable
private fun SourceLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        color = onMedia,
        modifier =
            modifier
                .background(overlayBlackWith70Alpha, CornerShapes.pill)
                .padding(horizontal = Dimens.compact, vertical = Dimens.tiny),
    )
}

/**
 * A round icon button over media: previous and next, close, and full screen, with a 40 dp target. Disabled, it stays
 * in place, dimmed, so the controls around it never move; [mirrored] turns a left-pointing icon right.
 */
@Suppress("LongParameterList")
@Composable
internal fun MediaIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    mirrored: Boolean = false,
) {
    Box(
        modifier =
            modifier
                .size(ArrowButtonSize)
                .background(overlayBlackWith70Alpha, CircleShape)
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = null,
                    // Like the carousels' buttons, the focus highlight shows only for keyboard focus.
                    indication =
                        FocusVisibleIndication(ripple(bounded = false, radius = ArrowButtonSize / 2, color = onMedia)),
                    onClick = onClick,
                ).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onMedia.copy(alpha = if (enabled) 1f else ContentAlpha.disabled),
            modifier =
                Modifier
                    .size(ArrowIconSize)
                    .then(if (mirrored) Modifier.rotate(NEXT_ICON_ROTATION_DEGREES) else Modifier),
        )
    }
}

/**
 * Every item as a thumbnail, in a horizontal strip or a [vertical] rail. Only the thumbnails on screen compose and
 * load. The list follows the selection, so the selected thumbnail stays in view. Arrow keys along the list select
 * the neighboring item and move focus to it.
 */
@Composable
private fun MediaThumbnails(
    state: MediaGalleryState,
    vertical: Boolean,
    placeholder: Boolean,
    modifier: Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.selectedIndex)
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state, listState) {
        snapshotFlow { state.selectedIndex }.collect { index -> scrollUnlessInterrupted { listState.reveal(index) } }
    }
    val content: LazyListScope.() -> Unit = {
        itemsIndexed(
            items = state.gallery.items,
            key = { _, item -> item.key },
            contentType = { _, _ -> THUMBNAIL_CONTENT_TYPE },
        ) { index, item ->
            // Only the thumbnails whose selection changes recompose, not every thumbnail on screen.
            val selected by remember(state, index) { derivedStateOf { state.selectedIndex == index } }
            val position = state.gallery.positionInKind(index)
            val label =
                if (item is GameMedia.YouTubeVideo) Res.string.game_media_video else Res.string.game_media_screenshot
            MediaThumbnail(
                item = item,
                selected = selected,
                description =
                    stringResource(
                        label,
                        position.number,
                        position.count,
                    ),
                onSelect = { state.select(index) },
                onStep = { step ->
                    val target = index + step
                    if (target in state.gallery.items.indices) {
                        state.select(target)
                        focusManager.moveFocus(focusDirection(vertical, forward = step > 0))
                    }
                },
                vertical = vertical,
                placeholder = placeholder,
                modifier = if (vertical) Modifier.fillMaxWidth() else Modifier.width(StripThumbnailWidth),
            )
        }
    }
    if (vertical) {
        Row(modifier = modifier) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxHeight().selectableGroup(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(Dimens.small),
                content = content,
            )
            // Mouse users see how far the rail goes and can drag through it.
            ListScrollbar(state = listState)
        }
    } else {
        Column(modifier = modifier) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(Dimens.small),
                content = content,
            )
            // Mouse users see how far the strip goes and can drag through it; it lines up with the thumbnails.
            ListScrollbar(
                state = listState,
                orientation = Orientation.Horizontal,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun MediaThumbnail(
    item: GameMedia,
    selected: Boolean,
    description: String,
    onSelect: () -> Unit,
    onStep: (step: Int) -> Unit,
    vertical: Boolean,
    placeholder: Boolean,
    modifier: Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val url =
        when (item) {
            is GameMedia.YouTubeVideo -> item.thumbnailUrl
            is GameMedia.Screenshot -> item.thumbnailUrl
        }
    var loaded by remember(url) { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .aspectRatio(MEDIA_ASPECT_RATIO)
                // Outermost, so a version's skeleton also hides the selection border.
                .placeholder(visible = placeholder, shape = CornerShapes.small)
                .graphicsLayer { alpha = if (selected || hovered) 1f else UNSELECTED_THUMBNAIL_ALPHA }
                .clip(CornerShapes.small)
                .background(secondaryVariant)
                .then(if (selected) Modifier.border(Dimens.micro, accent, CornerShapes.small) else Modifier)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.small, Dimens.micro)
                .onPreviewKeyEvent { event ->
                    if (placeholder) return@onPreviewKeyEvent false
                    val step =
                        when {
                            event.type != KeyEventType.KeyDown -> 0
                            event.key == if (vertical) Key.DirectionUp else Key.DirectionLeft -> -1
                            event.key == if (vertical) Key.DirectionDown else Key.DirectionRight -> 1
                            else -> 0
                        }
                    if (step != 0) onStep(step)
                    step != 0
                }.pointerHoverIcon(if (placeholder) PointerIcon.Default else PointerIcon.Hand)
                .selectable(
                    selected = selected,
                    enabled = !placeholder,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Tab,
                    onClick = onSelect,
                ).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // A skeleton until the image arrives; a failed image leaves the plain tile, so it never looks still loading.
        if (!loaded) Box(modifier = Modifier.fillMaxSize().shimmerEffect())
        AsyncImage(
            // Its own memory key for the thumbnail's decoded size; the download is shared with the hero's backdrop.
            model = rememberCachedImageRequest(data = url, memoryCacheKey = thumbnailMemoryCacheKey(item)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { loaded = true },
            onError = { loaded = true },
        )
        if (item is GameMedia.YouTubeVideo) PlayCircle(size = ThumbnailPlayBadgeSize, iconSize = ThumbnailPlayIconSize)
    }
}

/**
 * The memory cache key of [item]'s thumbnail. The preview shows that decoded thumbnail, which the strip has usually
 * loaded already, while its own larger image loads, so a selection shows a picture at once and sharpens when ready.
 */
internal fun thumbnailMemoryCacheKey(item: GameMedia): String =
    when (item) {
        is GameMedia.YouTubeVideo -> "gmt-${item.thumbnailUrl}"
        is GameMedia.Screenshot -> "gmt-${item.thumbnailUrl}"
    }

/**
 * Runs [scroll], which another scroll, such as a gesture or keyboard focus bringing an item into view, may interrupt.
 * Compose interrupts a scroll by cancelling it; an interrupted scroll is simply over, so a caller that follows the
 * selection keeps following it. Only cancellation of the caller itself ends the caller.
 */
internal suspend fun scrollUnlessInterrupted(scroll: suspend () -> Unit) {
    try {
        scroll()
    } catch (interrupted: CancellationException) {
        currentCoroutineContext().ensureActive()
    }
}

/** The memory cache key of the preview's decoded image at [url]; the viewer shows it while its own image loads. */
internal fun previewMemoryCacheKey(url: String): String = "gmp-$url"

/** Requests focus, unless the target is not laid out yet; then focus stays where it is. */
internal fun FocusRequester.requestFocusSafely() {
    try {
        requestFocus()
    } catch (_: IllegalStateException) {
        // Not attached: nothing to focus.
    }
}

private fun focusDirection(
    vertical: Boolean,
    forward: Boolean,
): FocusDirection =
    when {
        vertical && forward -> FocusDirection.Down
        vertical -> FocusDirection.Up
        forward -> FocusDirection.Right
        else -> FocusDirection.Left
    }

/**
 * Scrolls the least distance that shows the item at [index] whole: a short glide to a neighbor, as arrow keys and the
 * preview's buttons select, and a jump without animation to a distant one.
 */
private suspend fun LazyListState.reveal(index: Int) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        scrollToItem(index)
        return
    }
    // Offsets start at the content's start; the content padding lies outside [0, contentEnd].
    val contentEnd = info.viewportEndOffset - info.afterContentPadding
    val end = item.offset + item.size
    val distance =
        when {
            item.offset < 0 -> item.offset
            end > contentEnd -> end - contentEnd
            else -> 0
        }
    if (distance != 0) animateScrollBy(distance.toFloat())
}

/** A horizontal swipe across the preview selects the next or previous item; vertical drags still scroll the page. */
private fun Modifier.swipeToSelect(
    state: MediaGalleryState,
    thresholdPx: Float,
): Modifier =
    pointerInput(state, thresholdPx) {
        var distance = 0f
        detectHorizontalDragGestures(
            onDragStart = { distance = 0f },
            onDragEnd = {
                when {
                    distance <= -thresholdPx -> state.selectNext()
                    distance >= thresholdPx -> state.selectPrevious()
                }
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                distance += dragAmount
            },
        )
    }

private const val PREVIEW_SCREENSHOT_COUNT = 5

private val PreviewGallery =
    MediaGallery(
        videos = listOf(GameMedia.YouTubeVideo(youTubeId = "aaaaaaaaaaa")),
        screenshots =
            List(PREVIEW_SCREENSHOT_COUNT) { index ->
                GameMedia.Screenshot(thumbUrl = "https://images.igdb.com/igdb/image/upload/t_thumb/sample$index.jpg")
            },
    )

@Preview(widthDp = 400)
@Composable
internal fun MediaGalleryPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(background).padding(vertical = Dimens.medium)) {
            MediaGallery(
                state = rememberMediaGalleryState(PreviewGallery),
                gameName = "Sample game",
                onWatchOnYouTube = {},
                horizontalPadding = Dimens.medium,
            )
        }
    }
}

// An extra-large window, where the thumbnails move into a rail beside the preview.
@Preview(widthDp = 1600)
@Composable
internal fun MediaGalleryRailPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(background).padding(vertical = Dimens.large)) {
            MediaGallery(
                state = rememberMediaGalleryState(PreviewGallery),
                gameName = "Sample game",
                onWatchOnYouTube = {},
                horizontalPadding = Dimens.large,
                thumbnailRail = true,
            )
        }
    }
}
