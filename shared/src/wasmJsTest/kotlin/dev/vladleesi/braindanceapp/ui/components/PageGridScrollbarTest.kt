// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PageGridScrollbarTest {
    @Test
    fun measuredRowAccountsForTwoFullWidthHeaderRows() {
        assertEquals(
            6,
            estimatedGridRows(columns = 5, lastVisibleRow = 3, firstFreeColumn = 4, remainingItems = 9),
        )
    }

    @Test
    fun partiallyFilledLastRowCountsOnce() {
        assertEquals(
            6,
            estimatedGridRows(columns = 5, lastVisibleRow = 5, firstFreeColumn = 3, remainingItems = 0),
        )
    }

    @Test
    fun remainingCardsFillTheVisibleRowBeforeAddingAnotherRow() {
        assertEquals(
            2,
            estimatedGridRows(columns = 5, lastVisibleRow = 1, firstFreeColumn = 3, remainingItems = 2),
        )
    }
}
