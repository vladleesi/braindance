// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.w3c.dom.events.Event

private const val SCROLLBAR_HIDE_DELAY_MILLIS = 500L
private const val SCROLLBAR_FADE_OUT_MILLIS = 200

// Inset the painted thumb while keeping its full layout available for mouse input.
private val PageScrollbarThumbShape =
    object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline =
            with(density) {
                val insetX = minOf(Dimens.micro.toPx(), size.width / 2)
                val insetY = minOf(Dimens.tiny.toPx(), size.height / 2)
                val thumb = Rect(insetX, insetY, size.width - insetX, size.height - insetY)
                val radius = CornerRadius(minOf(thumb.width, thumb.height) / 2)
                Outline.Rounded(RoundRect(thumb, radius))
            }
    }

/** The width of every scrollbar in the app, which a list's scrollbar gutter also reserves. */
internal val PageScrollbarThickness = Dimens.small + Dimens.micro

// The thumb at rest: the theme's foreground, faint.
private const val THUMB_RESTING_ALPHA = 0.45f

/** The look of every scrollbar in the app, the page's and those of lists inside it, in the theme's colors. */
@Composable
internal fun rememberPageScrollbarStyle(): ScrollbarStyle {
    val resting = foreground.copy(alpha = THUMB_RESTING_ALPHA)
    val hovered = secondaryText
    return remember(resting, hovered) {
        defaultScrollbarStyle().copy(
            minimalHeight = Dimens.large,
            thickness = PageScrollbarThickness,
            shape = PageScrollbarThumbShape,
            unhoverColor = resting,
            hoverColor = hovered,
        )
    }
}

@Composable
internal actual fun BoxScope.PageScrollbar(
    state: ScrollableState,
    topInset: Dp,
) {
    // Middle-click autoscrolling works in every layout, wherever a mouse is used.
    PageAutoscrollTarget(state)
    if (desktopLayout() == null) return

    val canScroll by remember(state) {
        derivedStateOf(structuralEqualityPolicy()) { state.canScrollForward || state.canScrollBackward }
    }
    // Touch-only windows can reach the desktop breakpoint when rotated or on a tablet.
    if (!canScroll || !rememberMediaQueryMatches(FINE_POINTER_QUERY)) return

    key(state) {
        AnimatedPageScrollbar(state, topInset, scrolling = state.isScrollInProgress)
    }
}

@Composable
private fun BoxScope.AnimatedPageScrollbar(
    state: ScrollableState,
    topInset: Dp,
    scrolling: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val dragging by interactionSource.collectIsDraggedAsState()
    val reduceMotion = rememberMediaQueryMatches("(prefers-reduced-motion: reduce)")
    val active = scrolling || dragging
    val keepVisible by produceState(initialValue = active, key1 = active) {
        if (!active && value) delay(SCROLLBAR_HIDE_DELAY_MILLIS)
        value = active
    }
    AnimatedVisibility(
        visible = active || keepVisible,
        modifier = Modifier.align(Alignment.CenterEnd),
        enter = EnterTransition.None,
        exit =
            if (reduceMotion) {
                ExitTransition.None
            } else {
                fadeOut(tween(SCROLLBAR_FADE_OUT_MILLIS, easing = LinearEasing))
            },
    ) {
        val adapter =
            when (state) {
                is ScrollState -> rememberScrollbarAdapter(state)
                is LazyListState -> rememberScrollbarAdapter(state)
                is LazyGridState -> {
                    val gridAdapter = rememberScrollbarAdapter(state)
                    remember(state, gridAdapter) { PageGridScrollbarAdapter(state, gridAdapter) }
                }
                else -> return@AnimatedVisibility
            }
        VerticalScrollbar(
            adapter = adapter,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(top = topInset + Dimens.tiny, bottom = Dimens.tiny, end = Dimens.tiny)
                    .scrollable(state, Orientation.Vertical, reverseDirection = true),
            style = rememberPageScrollbarStyle(),
            interactionSource = interactionSource,
        )
    }
}

private class PageGridScrollbarAdapter(
    private val state: LazyGridState,
    private val adapter: ScrollbarAdapter,
) : ScrollbarAdapter by adapter {
    override val contentSize: Double
        get() {
            val layout = state.layoutInfo
            val estimatedSize = adapter.contentSize
            val last =
                layout.visibleItemsInfo.lastOrNull { it.row != LazyGridItemInfo.UnknownRow }
                    ?: return estimatedSize
            val columns = layout.maxSpan
            val estimatedRows = (layout.totalItemsCount + columns - 1) / columns
            // Compose's estimate counts every item as one cell. Use the measured row to account for spans.
            val measuredRows =
                estimatedGridRows(
                    columns = columns,
                    lastVisibleRow = last.row,
                    firstFreeColumn = last.column + last.span,
                    remainingItems = layout.totalItemsCount - last.index - 1,
                )
            val extraRows = (measuredRows - estimatedRows).coerceAtLeast(0)
            val padding = layout.beforeContentPadding + layout.afterContentPadding
            val averageRowWithSpacing = (estimatedSize - padding + layout.mainAxisItemSpacing) / estimatedRows
            return estimatedSize + extraRows * averageRowWithSpacing
        }

    private val maxOffset: Double
        get() = (contentSize - viewportSize).coerceAtLeast(0.0)

    override val scrollOffset: Double
        get() =
            when {
                !state.canScrollBackward -> 0.0
                !state.canScrollForward -> maxOffset
                else -> adapter.scrollOffset.coerceIn(0.0, maxOffset)
            }

    override suspend fun scrollTo(scrollOffset: Double) {
        val itemCount = state.layoutInfo.totalItemsCount
        if (itemCount == 0) return
        when {
            scrollOffset <= 0.0 -> state.scrollToItem(0)
            scrollOffset >= maxOffset -> state.scrollToItem(itemCount - 1)
            else -> adapter.scrollTo(adapter.scrollOffset + scrollOffset - this.scrollOffset)
        }
    }
}

internal fun estimatedGridRows(
    columns: Int,
    lastVisibleRow: Int,
    firstFreeColumn: Int,
    remainingItems: Int,
): Int = lastVisibleRow + (firstFreeColumn + remainingItems + columns - 1) / columns

/** Whether the browser has a mouse or another precise pointer, for controls that only a pointer uses. */
internal const val FINE_POINTER_QUERY = "(any-pointer: fine)"

/**
 * Whether the browser matches the CSS media [query], updated when that changes. It starts from the current answer, so
 * the first frame already has the right layout.
 */
@Composable
internal fun rememberMediaQueryMatches(query: String): Boolean =
    produceState(initialValue = remember(query) { window.matchMedia(query).matches }, key1 = query) {
        val media = window.matchMedia(query)
        val listener: (Event) -> Unit = { value = media.matches }
        media.addEventListener("change", listener)
        value = media.matches
        awaitDispose { media.removeEventListener("change", listener) }
    }.value

@Preview
@Composable
internal fun PageScrollbarPreview() {
    BraindanceTheme {
        val state = rememberScrollState()
        Box(Modifier.fillMaxWidth().height(Dimens.extraLarge)) {
            Column(Modifier.verticalScroll(state)) {
                Text("First row", modifier = Modifier.padding(Dimens.medium))
                Text("Second row", modifier = Modifier.padding(Dimens.medium))
            }
            AnimatedPageScrollbar(state, topInset = Dimens.medium, scrolling = true)
        }
    }
}
