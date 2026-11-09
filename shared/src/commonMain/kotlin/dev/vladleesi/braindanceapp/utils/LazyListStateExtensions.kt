// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.white

/** The default cache window of [rememberCachedLazyListState]: one viewport ahead and one behind. */
const val LAZY_CACHE_WINDOW_VIEWPORT_FRACTION = 1f

/**
 * Creates a [LazyListState] whose list keeps items composed within a window around the viewport, measured in
 * viewport heights (or widths for a horizontal list).
 *
 * Items within [aheadFraction] in the scroll direction are composed during idle frame time, before they become
 * visible. Items within [behindFraction] on the other side stay composed after they scroll out, so scrolling back does
 * not compose them again. Use it where each item is expensive to compose, such as a section with a carousel or
 * images. Plain rows of text compose quickly, so the default prefetch is enough for them. A larger window holds more
 * items in memory.
 *
 * Like [rememberLazyListState], the state survives recreation. Changing either fraction creates a new state, which
 * resets the scroll position.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun rememberCachedLazyListState(
    aheadFraction: Float = LAZY_CACHE_WINDOW_VIEWPORT_FRACTION,
    behindFraction: Float = LAZY_CACHE_WINDOW_VIEWPORT_FRACTION,
): LazyListState {
    val cacheWindow =
        remember(aheadFraction, behindFraction) {
            LazyLayoutCacheWindow(aheadFraction = aheadFraction, behindFraction = behindFraction)
        }
    return rememberLazyListState(cacheWindow = cacheWindow)
}

@Composable
fun LazyListState.calculateScrollTopBarColors(): Triple<Color, Color, Color> {
    val targetColor = navBar
    val maxScrollOffsetPx = with(LocalDensity.current) { Dimens.topBarHeightWithInsets.toPx() }

    // Derived so callers recompose only while the fraction changes, not on every scroll frame.
    val fraction by remember(this, maxScrollOffsetPx) {
        var firstItemOffset = 0f
        derivedStateOf {
            val scrollOffset = firstVisibleItemScrollOffset.toFloat()
            val combinedOffset =
                when (firstVisibleItemIndex) {
                    0 -> scrollOffset.also { firstItemOffset = it }
                    1 -> scrollOffset + firstItemOffset
                    else -> maxScrollOffsetPx
                }
            (combinedOffset / maxScrollOffsetPx).coerceIn(0f, 1f)
        }
    }

    return Triple(
        lerp(Color.Transparent, targetColor, fraction),
        lerp(targetColor, Color.Transparent, fraction),
        lerp(Color.Transparent, white, fraction),
    )
}
