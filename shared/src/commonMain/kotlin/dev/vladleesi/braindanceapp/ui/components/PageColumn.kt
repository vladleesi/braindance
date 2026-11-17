// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.surface

/*
 * Every page lays its content out in one column, centered beside the navigation: its top bar, headings, and content
 * share the column's edges, so nothing moves sideways between screens of the same width. Pages of grids, card rows,
 * or a hero use the default Dimens.pageMaxWidth; pages that are one column of text or rows use
 * Dimens.readableTextWidth. Inside the column, text and placeholders keep their own readable widths, aligned to its
 * start.
 */

/**
 * The page's side margin. The desktop layout of wide Web windows has a wider one; phones and tablets keep the
 * standard margin at every size.
 */
@Composable
fun pageGutter(): Dp = if (desktopLayout() != null) Dimens.large else Dimens.medium

/** The width of the page column's content in [availableWidth]: all of it but the [gutter]s, up to [maxContentWidth]. */
fun pageContentWidth(
    availableWidth: Dp,
    gutter: Dp,
    maxContentWidth: Dp = Dimens.pageMaxWidth,
): Dp = (availableWidth - gutter * 2).coerceIn(0.dp, maxContentWidth)

/**
 * The space on each side of the page column's content in [availableWidth], gutter included, for lazy layouts that
 * span the whole width, so they also scroll from the margins, and inset their content by it.
 */
fun pageSidePadding(
    availableWidth: Dp,
    gutter: Dp,
    maxContentWidth: Dp = Dimens.pageMaxWidth,
): Dp = ((availableWidth - pageContentWidth(availableWidth, gutter, maxContentWidth)) / 2).coerceAtLeast(0.dp)

/**
 * Lays this element out as the page column: as wide as the space allows, up to [maxContentWidth] plus a [gutter] on
 * each side, and centered in it. It adds no padding, for elements that inset their own content by the gutter, such
 * as a carousel that scrolls through it; [pageColumn] adds it. Modifiers before this one, such as a background or a
 * scroll container, still span the whole width.
 */
fun Modifier.pageWidth(
    gutter: Dp,
    maxContentWidth: Dp = Dimens.pageMaxWidth,
): Modifier =
    fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxContentWidth + gutter * 2)
        .fillMaxWidth()

/**
 * Lays this element out in the page column, as [pageWidth] does, inset by the [gutter]. An element that limits its
 * own width after this modifier, such as a card with a readable maximum, keeps that width at the column's start; one
 * that should span the column whatever its content, such as an empty block, adds `fillMaxWidth()` after it.
 */
fun Modifier.pageColumn(
    gutter: Dp,
    maxContentWidth: Dp = Dimens.pageMaxWidth,
): Modifier =
    pageWidth(gutter, maxContentWidth)
        .padding(horizontal = gutter)
        .wrapContentWidth(Alignment.Start)

private val PreviewBlockHeight = 48.dp
private val PreviewWideWidth = 960.dp
private val PreviewReadingWidth = 400.dp

@Preview(widthDp = 1200)
@Composable
internal fun PageColumnPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(vertical = Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.medium),
        ) {
            // A wide page column, and a reading column, each with one block that keeps a readable width.
            Column(modifier = Modifier.pageColumn(Dimens.medium, maxContentWidth = PreviewWideWidth)) {
                SurfaceCard(modifier = Modifier.fillMaxWidth().height(PreviewBlockHeight)) {}
                Box(Modifier.height(Dimens.small))
                SurfaceCard(
                    modifier = Modifier.widthIn(max = PreviewReadingWidth).fillMaxWidth().height(PreviewBlockHeight),
                ) {}
            }
            Box(
                modifier =
                    Modifier
                        .pageColumn(Dimens.medium, maxContentWidth = PreviewReadingWidth)
                        .fillMaxWidth()
                        .height(PreviewBlockHeight)
                        .background(surface),
            )
        }
    }
}
