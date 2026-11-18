// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkPressTest {
    private fun press(
        primary: Boolean = false,
        middle: Boolean = false,
        ctrl: Boolean = false,
        meta: Boolean = false,
        shift: Boolean = false,
        apple: Boolean,
    ) = linkPress(primary, middle, ctrl, meta, shift, isApplePlatform = apple)

    @Test
    fun aPlainClickKeepsTheElementsOwnAction() {
        assertEquals(LinkPress.Activate, press(primary = true, apple = false))
        assertEquals(LinkPress.Activate, press(primary = true, apple = true))
    }

    @Test
    fun aMiddleClickOpensANewTabEverywhere() {
        assertEquals(LinkPress.OpenInNewTab, press(middle = true, apple = false))
        assertEquals(LinkPress.OpenInNewTab, press(middle = true, apple = true))
    }

    @Test
    fun theNewTabModifierFollowsTheOperatingSystem() {
        assertEquals(LinkPress.OpenInNewTab, press(primary = true, ctrl = true, apple = false))
        assertEquals(LinkPress.OpenInNewTab, press(primary = true, meta = true, apple = true))
        // ⌘ is not a link modifier on Windows and Linux, where it is the Windows or Super key.
        assertEquals(LinkPress.Activate, press(primary = true, meta = true, apple = false))
        assertEquals(LinkPress.OpenInNewTab, press(primary = true, shift = true, apple = false))
        assertEquals(LinkPress.OpenInNewTab, press(primary = true, shift = true, apple = true))
    }

    @Test
    fun aControlClickOnApplePlatformsIsASecondaryClickAndOpensNothing() {
        assertEquals(LinkPress.Ignore, press(primary = true, ctrl = true, apple = true))
    }

    @Test
    fun otherButtonsKeepTheElementsOwnHandling() {
        assertEquals(LinkPress.Activate, press(apple = false))
        assertEquals(LinkPress.Activate, press(ctrl = true, apple = true))
    }
}
