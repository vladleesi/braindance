// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var firstOffset by remember { mutableStateOf(0f) }

    val combinedOffset =
        remember(
            firstVisibleItemIndex,
            firstVisibleItemScrollOffset,
        ) {
            val scrollOffset = firstVisibleItemScrollOffset.toFloat()
            when (firstVisibleItemIndex) {
                0 -> scrollOffset.also { firstOffset = scrollOffset }
                1 -> scrollOffset + firstOffset
                else -> maxScrollOffsetPx
            }
        }

    val fraction = (combinedOffset / maxScrollOffsetPx).coerceIn(0f, 1f)

    return Triple(
        lerp(Color.Transparent, targetColor, fraction),
        lerp(targetColor, Color.Transparent, fraction),
        lerp(Color.Transparent, white, fraction),
    )
}
