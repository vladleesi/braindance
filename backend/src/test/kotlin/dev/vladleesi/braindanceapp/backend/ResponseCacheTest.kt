// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ResponseCacheTest {
    private class MutableClock(
        var nowMillis: Long = 0,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?) = this

        override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    }

    private fun hit(size: Int): ResponseCache.LoadResult = ResponseCache.LoadResult.Hit(CachedResponse(ByteArray(size)))

    @Test
    fun servesFreshEntriesAndReloadsAfterExpiry(): Unit =
        runBlocking {
            val clock = MutableClock()
            val cache = ResponseCache(ttlMillis = 1000, maxBytes = 100, maxEntryBytes = 100, clock = clock)
            val loads = AtomicInteger()
            repeat(3) { cache.getOrLoad("key") { hit(10).also { loads.incrementAndGet() } } }
            assertEquals(1, loads.get())
            clock.nowMillis = 1000
            cache.getOrLoad("key") { hit(10).also { loads.incrementAndGet() } }
            assertEquals(2, loads.get())
        }

    @Test
    fun keepsWithinByteBudgetAndSkipsOversizedEntries(): Unit =
        runBlocking {
            val cache = ResponseCache(ttlMillis = 60_000, maxBytes = 100, maxEntryBytes = 60)
            val loads = mutableMapOf<String, Int>()

            suspend fun load(
                key: String,
                size: Int,
            ) = cache.getOrLoad(key) { hit(size).also { loads[key] = (loads[key] ?: 0) + 1 } }

            load("a", 40)
            load("b", 40)
            load("a", 40) // Marks "a" as recently used.
            load("c", 40) // Exceeds 100 bytes and evicts the least recently used entry, "b".
            load("a", 40)
            load("b", 40)
            assertEquals(mapOf("a" to 1, "b" to 2, "c" to 1), loads)

            load("large", 61)
            load("large", 61)
            assertEquals(2, loads["large"])
        }

    @Test
    fun failuresAreSharedButNeverStored(): Unit =
        runBlocking {
            val cache = ResponseCache(ttlMillis = 60_000, maxBytes = 100, maxEntryBytes = 100)
            val loads = AtomicInteger()
            repeat(2) {
                val result =
                    cache.getOrLoad(
                        "key",
                    ) { ResponseCache.LoadResult.Uncached(502).also { loads.incrementAndGet() } }
                assertEquals(ResponseCache.LoadResult.Uncached(502), result)
            }
            assertEquals(2, loads.get())
            assertFailsWith<IllegalStateException> { cache.getOrLoad("key") { error("upstream failed") } }
            assertIs<ResponseCache.LoadResult.Hit>(cache.getOrLoad("key") { hit(1) })
        }

    @Test
    fun concurrentMissesShareOneLoad(): Unit =
        runBlocking {
            val cache = ResponseCache(ttlMillis = 60_000, maxBytes = 100, maxEntryBytes = 100)
            val release = CompletableDeferred<Unit>()
            val loads = AtomicInteger()
            val callers =
                List(20) {
                    async(start = CoroutineStart.UNDISPATCHED) {
                        cache.getOrLoad("key") {
                            loads.incrementAndGet()
                            release.await()
                            hit(10)
                        }
                    }
                }
            release.complete(Unit)
            assertEquals(1, callers.awaitAll().distinct().size)
            assertEquals(1, loads.get())
        }

    @Test
    fun waiterLoadsItselfWhenLeaderIsCancelled(): Unit =
        runBlocking {
            val cache = ResponseCache(ttlMillis = 60_000, maxBytes = 100, maxEntryBytes = 100)
            val leaderStarted = CompletableDeferred<Unit>()
            val leader =
                async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrLoad("key") {
                        leaderStarted.complete(Unit)
                        CompletableDeferred<Unit>().await()
                        hit(1)
                    }
                }
            leaderStarted.await()
            val waiter = async(start = CoroutineStart.UNDISPATCHED) { cache.getOrLoad("key") { hit(2) } }
            leader.cancelAndJoin()
            val result = assertIs<ResponseCache.LoadResult.Hit>(waiter.await())
            assertEquals(2, result.response.body.size)
        }
}
