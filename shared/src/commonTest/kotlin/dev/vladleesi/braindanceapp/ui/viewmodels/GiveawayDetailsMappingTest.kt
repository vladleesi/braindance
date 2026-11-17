// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.formatShortDate
import dev.vladleesi.braindanceapp.utils.withThousandsSeparators
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GiveawayDetailsMappingTest {
    private val today = LocalDate(2026, 11, 17)

    @Test
    fun aFutureEndDateCountsTheDaysLeft() {
        assertEquals(
            GiveawayEnding.EndsOn(date = LocalDate(2026, 11, 20), daysLeft = 3),
            giveaway(endDate = "2026-11-20 23:59:00").toGiveawayDetails(today).ending,
        )
        assertEquals(
            GiveawayEnding.EndsOn(date = today, daysLeft = 0),
            giveaway(endDate = "2026-11-17 23:59:00").toGiveawayDetails(today).ending,
        )
    }

    @Test
    fun aPassedEndDateOrAnExpiredStatusEndsTheGiveaway() {
        assertEquals(
            GiveawayEnding.Ended(date = LocalDate(2026, 11, 16)),
            giveaway(endDate = "2026-11-16 23:59:00").toGiveawayDetails(today).ending,
        )
        assertEquals(
            GiveawayEnding.Ended(date = LocalDate(2026, 12, 1)),
            giveaway(endDate = "2026-12-01 00:00:00", status = "Expired").toGiveawayDetails(today).ending,
        )
        assertEquals(
            GiveawayEnding.Ended(date = null),
            giveaway(endDate = "N/A", status = "expired").toGiveawayDetails(today).ending,
        )
    }

    @Test
    fun anUnknownEndDateIsOpenEnded() {
        listOf("N/A", "", "soon", null).forEach { endDate ->
            assertEquals(GiveawayEnding.OpenEnded, giveaway(endDate = endDate).toGiveawayDetails(today).ending)
        }
    }

    @Test
    fun numberedInstructionsBecomeStepsWithoutTheirNumbers() {
        assertEquals(
            listOf("Click the button.", "Log in to your account.", "Add it to your library."),
            "1. Click the button.\r\n2) Log in to your account.\n\n3.Add it to your library.".claimSteps(),
        )
        assertEquals(listOf("Open the page.", "Claim it."), "1. Open the page.<br>2. Claim it.<BR />".claimSteps())
    }

    @Test
    fun unnumberedOrMissingInstructionsKeepTheirText() {
        assertEquals(listOf("Just claim it on the store page."), "Just claim it on the store page.".claimSteps())
        assertEquals(emptyList(), " \r\n ".claimSteps())
    }

    @Test
    fun platformsKeepEveryStoreNameOnceAndTheirLogos() {
        val details = giveaway(platforms = "PC, Epic Games Store, , PC").toGiveawayDetails(today)
        assertEquals(listOf("PC", "Epic Games Store"), details.platformNames)
        assertEquals(listOf(ParentPlatformType.PC, ParentPlatformType.Other), details.platforms)
    }

    @Test
    fun linksAreKeptOnlyWhenSafeAndUnknownValuesAreEmpty() {
        val details =
            giveaway(
                worth = "N/A",
                openUrl = "https://www.gamerpower.com/open/sample",
                sourceUrl = "http://www.gamerpower.com/sample",
            ).toGiveawayDetails(today)
        assertEquals("https://www.gamerpower.com/open/sample", details.openUrl)
        assertNull(details.sourceUrl)
        assertEquals("", details.worth)
        assertEquals(LocalDate(2026, 11, 1), details.addedOn)
    }

    @Test
    fun countsAndDatesReadTheWayTheAppWritesThem() {
        assertEquals("0", 0.withThousandsSeparators())
        assertEquals("999", 999.withThousandsSeparators())
        assertEquals("12,500", 12_500.withThousandsSeparators())
        assertEquals("1,234,567", 1_234_567.withThousandsSeparators())
        assertEquals("-1,500", (-1_500).withThousandsSeparators())
        assertEquals("Dec 3, 2026", LocalDate(2026, 12, 3).formatShortDate())
    }

    private fun giveaway(
        endDate: String? = "N/A",
        status: String? = "Active",
        platforms: String? = "PC",
        worth: String? = "$4.99",
        openUrl: String? = null,
        sourceUrl: String? = null,
    ) = GiveawayResponse(
        id = 1,
        title = "Sample",
        worth = worth,
        thumbnail = null,
        image = null,
        description = null,
        instructions = null,
        openGiveawayUrl = openUrl,
        publishedDate = "2026-11-01 10:00:00",
        type = "Game",
        platforms = platforms,
        endDate = endDate,
        users = 0,
        status = status,
        gamerpowerUrl = sourceUrl,
        openGiveaway = null,
    )
}
