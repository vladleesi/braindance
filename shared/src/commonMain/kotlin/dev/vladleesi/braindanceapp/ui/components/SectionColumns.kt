// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.surface

/** The narrowest a section column gets: a list of rows or a card still reads comfortably at this width. */
val SectionColumnMinWidth = 360.dp

/** How many sections of at least [minColumnWidth] fit side by side in [width], from one up to [maxColumns]. */
fun sectionColumnsFor(
    width: Dp,
    maxColumns: Int,
    minColumnWidth: Dp = SectionColumnMinWidth,
): Int = ((width + Dimens.large) / (minColumnWidth + Dimens.large)).toInt().coerceIn(1, maxOf(1, maxColumns))

/**
 * [items] split into at most [columns] runs in reading order, as evenly as possible, the first runs taking one more
 * when they do not divide evenly.
 */
internal fun <T> splitIntoColumns(
    items: List<T>,
    columns: Int,
): List<List<T>> {
    val count = columns.coerceIn(1, maxOf(1, items.size))
    val base = items.size / count
    val extra = items.size % count
    var start = 0
    return List(count) { index ->
        val size = base + if (index < extra) 1 else 0
        items.subList(start, start + size).also { start += size }
    }
}

/**
 * Independent sections, such as lists of settings or cards, side by side in [columns] equal columns that fill the
 * page column, top-aligned and in reading order, as [splitIntoColumns] divides them. One column stacks them at the
 * readable width, as phones and tablets show them.
 */
@Composable
fun SectionColumns(
    columns: Int,
    sections: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    val spacing = Arrangement.spacedBy(Dimens.large)
    val runs = splitIntoColumns(sections, columns)
    if (runs.size <= 1) {
        Column(modifier = modifier.widthIn(max = Dimens.readableTextWidth), verticalArrangement = spacing) {
            sections.forEach { section -> section() }
        }
    } else {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = spacing) {
            runs.forEach { run ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = spacing) {
                    run.forEach { section -> section() }
                }
            }
        }
    }
}

private val PreviewSectionHeights = listOf(200.dp, 280.dp, 160.dp)

@Preview(widthDp = 1200)
@Composable
internal fun SectionColumnsPreview() {
    BraindanceTheme {
        SectionColumns(
            columns = PreviewSectionHeights.size,
            sections =
                PreviewSectionHeights.map { height ->
                    { Box(modifier = Modifier.fillMaxWidth().height(height).background(surface)) }
                },
            modifier = Modifier.background(background).padding(Dimens.large),
        )
    }
}
