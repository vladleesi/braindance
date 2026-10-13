// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryRateLimiterTest {
    @Test
    fun concurrentReservationsShareOneWindow(): Unit =
        runBlocking {
            val limiter = InMemoryRateLimiter { 0L }
            val results = (1..20).map { async(Dispatchers.Default) { limiter.takeSlot() } }.awaitAll()
            assertEquals(4, results.count { it })
        }

    @Test
    fun reservationsExpireIndividually(): Unit =
        runBlocking {
            var now = 0L
            val limiter = InMemoryRateLimiter { now }
            assertTrue(limiter.takeSlot())
            now = 500_000_000L
            repeat(3) { assertTrue(limiter.takeSlot()) }
            now = 999_999_999L
            assertFalse(limiter.takeSlot())
            now = 1_000_000_000L
            assertTrue(limiter.takeSlot())
            assertFalse(limiter.takeSlot())
            now = 1_500_000_000L
            repeat(3) { assertTrue(limiter.takeSlot()) }
            assertFalse(limiter.takeSlot())
        }
}
