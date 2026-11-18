// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import kotlin.test.Test
import kotlin.test.assertEquals

class CollapsedChoicesTest {
    private val versions = listOf("Base game", "GOTY", "Complete", "Next-Gen")

    @Test
    fun aPhoneShowsOneOrTwoChoicesAsTheyAre() {
        assertEquals(listOf("Base game"), collapsedChoices(listOf("Base game"), maxShown = 1, selectedIndex = 0))
        assertEquals(versions.take(2), collapsedChoices(versions.take(2), maxShown = 1, selectedIndex = 1))
    }

    @Test
    fun moreThanTwoCollapseToTheSelectedOne() {
        assertEquals(listOf("Base game"), collapsedChoices(versions, maxShown = 1, selectedIndex = 0))
        assertEquals(listOf("Complete"), collapsedChoices(versions, maxShown = 1, selectedIndex = 2))
    }

    @Test
    fun aWideRowKeepsItsOrderAndAlwaysShowsTheSelection() {
        val many = (1..10).map { "Edition $it" }
        assertEquals(
            many.take(7),
            collapsedChoices(many.take(7), maxShown = 6, selectedIndex = 6),
            "one hidden saves nothing",
        )
        assertEquals(many.take(6), collapsedChoices(many, maxShown = 6, selectedIndex = 2))
        assertEquals(many.take(5) + "Edition 9", collapsedChoices(many, maxShown = 6, selectedIndex = 8))
    }
}
