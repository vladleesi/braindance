// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.Dimens
import kotlin.test.Test
import kotlin.test.assertEquals

class PageColumnTest {
    @Test
    fun aPhoneKeepsTheWholeWidthButTheGutters() {
        assertEquals(328.dp, pageContentWidth(availableWidth = 360.dp, gutter = Dimens.medium))
        assertEquals(Dimens.medium, pageSidePadding(availableWidth = 360.dp, gutter = Dimens.medium))
    }

    @Test
    fun aDesktopWindowFillsTheSpaceBesideTheSidebar() {
        // The content area beside the desktop sidebar in a 1920 dp window: no spare width, only the gutters.
        assertEquals(1624.dp, pageContentWidth(availableWidth = 1688.dp, gutter = Dimens.large))
        assertEquals(Dimens.large, pageSidePadding(availableWidth = 1688.dp, gutter = Dimens.large))
    }

    @Test
    fun aWideWindowCentersTheColumnAtItsMaximumWidth() {
        // The content area beside the desktop sidebar in a 2560 dp window.
        assertEquals(Dimens.pageMaxWidth, pageContentWidth(availableWidth = 2328.dp, gutter = Dimens.large))
        assertEquals(204.dp, pageSidePadding(availableWidth = 2328.dp, gutter = Dimens.large))
    }

    @Test
    fun aReadingColumnCentersAtTheReadableWidth() {
        val readable = Dimens.readableTextWidth
        assertEquals(readable, pageContentWidth(availableWidth = 1688.dp, gutter = Dimens.large, readable))
        assertEquals(484.dp, pageSidePadding(availableWidth = 1688.dp, gutter = Dimens.large, readable))
    }

    @Test
    fun spaceNarrowerThanTheGuttersLeavesNoContentWidthAndNoNegativePadding() {
        assertEquals(0.dp, pageContentWidth(availableWidth = 20.dp, gutter = Dimens.medium))
        assertEquals(10.dp, pageSidePadding(availableWidth = 20.dp, gutter = Dimens.medium))
        assertEquals(0.dp, pageSidePadding(availableWidth = 0.dp, gutter = Dimens.medium))
    }
}
