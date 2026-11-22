// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.models.games.GamingEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class EventsViewModelTest {
    @Test
    fun missingEndUsesThreeHoursFromStart() {
        val start = 10_000L
        val end = start + 3.hours.inWholeSeconds
        assertEquals(EventStatus.UPCOMING, eventStatus(start, null, start - 1))
        assertEquals(EventStatus.LIVE, eventStatus(start, null, start))
        assertEquals(EventStatus.LIVE, eventStatus(start, null, end - 1))
        assertEquals(EventStatus.ENDED, eventStatus(start, null, end))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aVisibleFeedUpdatesStatusWithoutReloading() =
        runTest {
            val start = 1.hours.inWholeSeconds
            val end = start + 1.minutes.inWholeSeconds
            val clock =
                object : Clock {
                    override fun now(): Instant =
                        Instant.fromEpochSeconds(start - 1.minutes.inWholeSeconds) +
                            testScheduler.currentTime.milliseconds
                }
            val feed = MutableStateFlow(SectionState.Success(listOf(GamingEvent(1, "Show", start, end))))
            val statuses =
                eventCards(feed, clock)
                    .map { (it as SectionState.Success).items.single().status }
                    .distinctUntilChanged()
                    .take(3)
                    .toList()
            assertEquals(listOf(EventStatus.UPCOMING, EventStatus.LIVE, EventStatus.ENDED), statuses)
        }
}
