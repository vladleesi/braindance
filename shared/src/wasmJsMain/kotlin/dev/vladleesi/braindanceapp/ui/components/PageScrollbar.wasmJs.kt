// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.ui.unit.LayoutDirection
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.overlayWhiteWith45Alpha
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

private val PageScrollbarStyle =
    defaultScrollbarStyle().copy(
        minimalHeight = Dimens.large,
        thickness = Dimens.small + Dimens.micro,
        shape = PageScrollbarThumbShape,
        unhoverColor = overlayWhiteWith45Alpha,
        hoverColor = secondaryText,
    )

@Composable
internal actual fun BoxScope.PageScrollbar(state: ScrollableState) {
    if (desktopLayout() == null) return

    val canScroll by remember(state) {
        derivedStateOf(structuralEqualityPolicy()) { state.canScrollForward || state.canScrollBackward }
    }
    // Touch-only windows can reach the desktop breakpoint when rotated or on a tablet.
    if (!canScroll || !rememberMediaQueryMatches("(any-pointer: fine)")) return

    key(state) {
        AnimatedPageScrollbar(state, scrolling = state.isScrollInProgress)
    }
}

@Composable
private fun BoxScope.AnimatedPageScrollbar(
    state: ScrollableState,
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
                is LazyGridState -> rememberScrollbarAdapter(state)
                else -> return@AnimatedVisibility
            }
        VerticalScrollbar(
            adapter = adapter,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(top = Dimens.tiny, bottom = Dimens.tiny, end = Dimens.tiny)
                    .scrollable(state, Orientation.Vertical, reverseDirection = true),
            style = PageScrollbarStyle,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun rememberMediaQueryMatches(query: String): Boolean =
    produceState(initialValue = false, key1 = query) {
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
            AnimatedPageScrollbar(state, scrolling = true)
        }
    }
}
