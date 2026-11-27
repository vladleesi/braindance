// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.Dimens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopLayoutRulesTest {
    @Test
    fun sectionsSplitInReadingOrderWithTheFirstColumnsTakingTheRemainder() {
        assertEquals(listOf(listOf("A", "B"), listOf("C")), splitIntoColumns(listOf("A", "B", "C"), columns = 2))
        assertEquals(
            listOf(listOf("A", "B"), listOf("C"), listOf("D")),
            splitIntoColumns(listOf("A", "B", "C", "D"), columns = 3),
        )
        assertEquals(listOf(listOf("A"), listOf("B"), listOf("C")), splitIntoColumns(listOf("A", "B", "C"), 3))
    }

    @Test
    fun moreColumnsThanSectionsNeverLeavesAnEmptyColumn() {
        assertEquals(listOf(listOf("A"), listOf("B")), splitIntoColumns(listOf("A", "B"), columns = 5))
    }

    @Test
    fun zeroOrNegativeColumnsStackEverythingInOne() {
        assertEquals(listOf(listOf("A", "B")), splitIntoColumns(listOf("A", "B"), columns = 0))
        assertEquals(listOf(listOf("A", "B")), splitIntoColumns(listOf("A", "B"), columns = -1))
    }

    @Test
    fun noSectionsGiveOneEmptyColumn() {
        assertEquals(listOf(emptyList()), splitIntoColumns(emptyList<String>(), columns = 3))
    }

    @Test
    fun sectionColumnsFollowTheWidthUpToTheirMaximum() {
        // The page column beside the desktop sidebar in 1100, 1440, and 1920 dp windows.
        assertEquals(2, sectionColumnsFor(804.dp, maxColumns = 3))
        assertEquals(3, sectionColumnsFor(1144.dp, maxColumns = 3))
        assertEquals(3, sectionColumnsFor(1624.dp, maxColumns = 3))
        assertEquals(2, sectionColumnsFor(1624.dp, maxColumns = 2))
    }

    @Test
    fun aNarrowColumnOrAZeroMaximumKeepsOneColumn() {
        assertEquals(1, sectionColumnsFor(500.dp, maxColumns = 3))
        assertEquals(1, sectionColumnsFor(0.dp, maxColumns = 3))
        assertEquals(1, sectionColumnsFor(1624.dp, maxColumns = 0))
    }

    @Test
    fun theSupportingPaneFitsOnlyBesideMainContentOfTheMinimumWidth() {
        val exact = SupportingPaneMinMainWidth + Dimens.large + SupportingPaneWidth
        assertTrue(supportingPaneFits(exact))
        assertFalse(supportingPaneFits(exact - 1.dp))
        assertTrue(supportingPaneFits(1624.dp))
        assertFalse(supportingPaneFits(904.dp, minMainWidth = 600.dp))
    }
}
