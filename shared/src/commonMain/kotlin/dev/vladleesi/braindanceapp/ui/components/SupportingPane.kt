// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.foreground

/** The width of a supporting pane: cards of facts and links, like Home's context column. */
val SupportingPaneWidth = 320.dp

/** The narrowest the main content may get beside a supporting pane before the pane moves below it. */
val SupportingPaneMinMainWidth = 360.dp

/** Whether a [paneWidth] supporting pane fits beside main content at least [minMainWidth] wide in [width]. */
fun supportingPaneFits(
    width: Dp,
    paneWidth: Dp = SupportingPaneWidth,
    minMainWidth: Dp = SupportingPaneMinMainWidth,
): Boolean = width >= minMainWidth + Dimens.large + paneWidth

/**
 * Material's supporting pane layout in the page column: [main] takes all the width the [pane] leaves, and the pane,
 * a [paneWidth] column of related details, keeps the column's end, as Home's context column does. When the main
 * content would get narrower than [minMainWidth], the pane follows it at the readable width instead. A [pane] that
 * emits nothing leaves the whole width to [main]. Both are top-aligned, and the main content keeps its text to the
 * readable width itself.
 */
@Composable
fun SupportingPaneLayout(
    main: @Composable () -> Unit,
    pane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    paneWidth: Dp = SupportingPaneWidth,
    minMainWidth: Dp = SupportingPaneMinMainWidth,
) {
    Layout(
        contents = listOf(main, pane),
        modifier = modifier,
    ) { (mainMeasurables, paneMeasurables), constraints ->
        val width = constraints.maxWidth
        val gap = Dimens.large.roundToPx()
        val paneWidthPx = paneWidth.roundToPx()
        val beside = paneMeasurables.isNotEmpty() && supportingPaneFits(width.toDp(), paneWidth, minMainWidth)
        val mainWidth = if (beside) width - gap - paneWidthPx else width
        val mainPlaceable = mainMeasurables.firstOrNull()?.measure(Constraints(maxWidth = mainWidth))
        val paneMax = if (beside) paneWidthPx else minOf(Dimens.readableTextWidth.roundToPx(), width)
        val panePlaceable = paneMeasurables.firstOrNull()?.measure(Constraints(maxWidth = paneMax))
        val mainHeight = mainPlaceable?.height ?: 0
        val paneHeight = panePlaceable?.height ?: 0
        val height =
            when {
                panePlaceable == null -> mainHeight
                beside -> maxOf(mainHeight, paneHeight)
                else -> mainHeight + gap + paneHeight
            }
        layout(width, height) {
            mainPlaceable?.placeRelative(0, 0)
            if (beside) {
                // At the column's end, however narrow the main content is.
                panePlaceable?.placeRelative(width - panePlaceable.width, 0)
            } else {
                panePlaceable?.placeRelative(0, mainHeight + gap)
            }
        }
    }
}

private val PreviewBlockHeight = 180.dp

@Preview(widthDp = 1100)
@Composable
internal fun SupportingPaneLayoutPreview() {
    BraindanceTheme {
        SupportingPaneLayout(
            main = { Box(modifier = Modifier.fillMaxWidth().height(PreviewBlockHeight).background(foreground)) },
            pane = { Box(modifier = Modifier.fillMaxWidth().height(PreviewBlockHeight / 2).background(accent)) },
            modifier = Modifier.background(background).fillMaxWidth(),
        )
    }
}
