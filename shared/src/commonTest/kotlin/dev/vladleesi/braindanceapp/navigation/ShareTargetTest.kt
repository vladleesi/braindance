// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.navigation.routes.CompilationRoute
import dev.vladleesi.braindanceapp.navigation.routes.EventsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShareTargetTest {
    @Test
    fun sharedContentLinksToItsCanonicalWebAppAddress() {
        assertEquals("https://app.braindancegg.com/games/42", ShareTarget.Game(42, "Game").shareUrl())
        assertEquals("https://app.braindancegg.com/giveaway/7", ShareTarget.Giveaway(7, "Giveaway").shareUrl())
        assertEquals("https://app.braindancegg.com/events/19", ShareTarget.Event(19, "Showcase").shareUrl())
        assertEquals(
            listOf(
                "https://app.braindancegg.com/popular",
                "https://app.braindancegg.com/anticipated",
                "https://app.braindancegg.com/giveaways",
            ),
            Compilation.entries.map { ShareTarget.CompilationList(it, "List").shareUrl() },
        )
    }

    @Test
    fun everySharedLinkOpensTheSharedScreenInTheApps() {
        val targets =
            listOf(
                ShareTarget.Game(42, "Game"),
                ShareTarget.Giveaway(7, "Giveaway"),
                ShareTarget.Event(19, "Showcase"),
            ) +
                Compilation.entries.map { ShareTarget.CompilationList(it, "List") }
        val expected =
            listOf(GameDetailsRoute(42), GiveawayDetailsRoute(7), EventsRoute(19)) +
                Compilation.entries.map(::CompilationRoute)
        assertEquals(expected, targets.map { appLinkRoute(it.shareUrl()) })
    }

    @Test
    fun contentWithoutAValidAddressHasNoLink() {
        // The apps reject these IDs, so the link would open nothing; no share button is offered for them.
        assertNull(ShareTarget.Game(0, "Game").shareUrl())
        assertNull(ShareTarget.Game(-1, "Game").shareUrl())
        assertNull(ShareTarget.Giveaway(0, "Giveaway").shareUrl())
        assertNull(ShareTarget.Event(-1, "Showcase").shareUrl())
    }

    @Test
    fun theNameNeverChangesTheLink() {
        // Names come from the API and go only into the message text, never into the address.
        val name = "Name/../../evil?x=1#y"
        assertEquals("https://app.braindancegg.com/games/42", ShareTarget.Game(42, name).shareUrl())
    }
}
