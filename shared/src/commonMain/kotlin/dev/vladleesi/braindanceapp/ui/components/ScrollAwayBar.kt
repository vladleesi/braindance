// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.surface
import kotlinx.coroutines.launch

/*
 * In the desktop layout the header stays on screen, so a screen's own bar (its title or back button, and its actions)
 * scrolls away with the content instead of staying too: reading then takes the whole height under the header, which
 * matters most on laptops. Phones and tablets keep the bar on screen, as Material's top app bar does.
 */

/**
 * How far scrollable content has moved from its top, in pixels, for a bar that scrolls away with it, and how to go
 * back there. Read [scrolledPx] only while laying out or drawing, so scrolling does not recompose.
 */
@Stable
class ContentScrollPosition(
    val scrolledPx: () -> Int,
    val scrollToTop: suspend () -> Unit,
) {
    companion object {
        /** Content that does not scroll, such as a message. */
        val Unscrolled = ContentScrollPosition(scrolledPx = { 0 }, scrollToTop = {})
    }
}

@Composable
fun rememberContentScrollPosition(state: ScrollState): ContentScrollPosition =
    remember(state) { ContentScrollPosition(scrolledPx = { state.value }, scrollToTop = { state.animateScrollTo(0) }) }

/** Past the first item, the bar is out of view, whatever the item's size. */
@Composable
fun rememberContentScrollPosition(state: LazyGridState): ContentScrollPosition =
    remember(state) {
        ContentScrollPosition(
            scrolledPx = {
                if (state.firstVisibleItemIndex ==
                    0
                ) {
                    state.firstVisibleItemScrollOffset
                } else {
                    Int.MAX_VALUE
                }
            },
            scrollToTop = { state.animateScrollToItem(0) },
        )
    }

/** Past the first item, the bar is out of view, whatever the item's size. */
@Composable
fun rememberContentScrollPosition(state: LazyListState): ContentScrollPosition =
    remember(state) {
        ContentScrollPosition(
            scrolledPx = {
                if (state.firstVisibleItemIndex ==
                    0
                ) {
                    state.firstVisibleItemScrollOffset
                } else {
                    Int.MAX_VALUE
                }
            },
            scrollToTop = { state.animateScrollToItem(0) },
        )
    }

/**
 * A [bar] drawn over the top of its content that moves up with the content at [position], as if it were part of it,
 * until it is out of view. The content leaves the bar's height free at its top. A bar that takes keyboard focus while
 * out of view scrolls the content back to the top, so the focus is never hidden. The parent clips it, so it never
 * covers the header above.
 */
@Composable
fun ScrollAwayBar(
    position: ContentScrollPosition,
    modifier: Modifier = Modifier,
    bar: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier =
            modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, -position.scrolledPx().coerceIn(0, placeable.height))
                    }
                }.onFocusChanged { state ->
                    if (state.hasFocus && position.scrolledPx() > 0) {
                        coroutineScope.launch { position.scrollToTop() }
                    }
                },
    ) {
        bar()
    }
}

/**
 * A screen's own [bar] above [content] that scrolls. Phones and tablets keep the bar on screen above the content. In
 * the desktop layout the bar is a [ScrollAwayBar] at the content's [position], and [content] starts its scrolling
 * content `topInset` lower, the bar's [barHeight] and the status bar, so the two move as one. A bar with a back button
 * is [Dimens.backTopBarHeight] tall.
 */
@Composable
fun ScrollAwayBarLayout(
    position: ContentScrollPosition,
    bar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = Dimens.topBarHeight,
    content: @Composable (topInset: Dp) -> Unit,
) {
    if (desktopLayout() != null) {
        Box(modifier = modifier.fillMaxSize().clipToBounds()) {
            content(Dimens.statusBarInsets + barHeight)
            ScrollAwayBar(position = position, bar = bar)
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            bar()
            Box(modifier = Modifier.weight(1f)) { content(0.dp) }
        }
    }
}

private val PreviewBlockHeight = 120.dp
private const val PREVIEW_BLOCK_COUNT = 8

@Preview(widthDp = 400, heightDp = 600)
@Composable
internal fun ScrollAwayBarLayoutPreview() {
    BraindanceTheme {
        val scrollState = rememberScrollState()
        ScrollAwayBarLayout(
            position = rememberContentScrollPosition(scrollState),
            bar = { TopAppBar(title = "Title", showBackButton = true) },
            modifier = Modifier.background(background),
        ) { topInset ->
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(top = topInset)) {
                repeat(PREVIEW_BLOCK_COUNT) {
                    Box(
                        modifier =
                            Modifier
                                .padding(Dimens.medium)
                                .fillMaxWidth()
                                .height(PreviewBlockHeight)
                                .background(surface),
                    )
                }
            }
        }
    }
}
