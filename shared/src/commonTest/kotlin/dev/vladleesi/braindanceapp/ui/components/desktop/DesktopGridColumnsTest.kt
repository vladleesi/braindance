// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopGridColumnsTest {
    @Test
    fun wideLayoutShowsEachSectionAtItsMaximum() {
        // The main column beside the sidebar and the context column in a 1440 dp window.
        assertEquals(DesktopGridColumns(games = 5, anticipated = 6, giveaways = 5), DesktopGridColumns.forWidth(832.dp))
    }

    @Test
    fun widerColumnsNeverExceedTheMaximums() {
        assertEquals(
            DesktopGridColumns(games = 5, anticipated = 6, giveaways = 5),
            DesktopGridColumns.forWidth(1120.dp),
        )
    }

    @Test
    fun narrowerColumnsShowFewerCardsThatStillFitTheirMinimumWidth() {
        // The main column beside the navigation rail in a 768 dp window.
        assertEquals(DesktopGridColumns(games = 3, anticipated = 4, giveaways = 3), DesktopGridColumns.forWidth(608.dp))
    }

    @Test
    fun everySectionKeepsAtLeastTwoCards() {
        assertEquals(DesktopGridColumns(games = 2, anticipated = 2, giveaways = 2), DesktopGridColumns.forWidth(100.dp))
    }
}
