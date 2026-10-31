// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.vladleesi.braindanceapp.system.toPx
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.white

@Composable
fun LazyListState.calculateScrollTopBarColors(): Triple<Color, Color, Color> {
    val targetColor = navBar
    val maxScrollOffsetPx = Dimens.topBarHeightWithInsets.toPx()

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
