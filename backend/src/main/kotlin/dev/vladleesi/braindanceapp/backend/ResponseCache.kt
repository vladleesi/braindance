// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

/**
 * A successful upstream response body and the content type to serve it with. Not a data class: generated equality
 * would compare the byte array by reference, and the cache only needs identity.
 */
internal class CachedResponse(
    val body: ByteArray,
    val contentType: String? = null,
)

/**
 * Process-local cache for successful public upstream responses. Entries expire after [ttlMillis]; the least recently
 * used entries are evicted to stay within [maxBytes], and bodies larger than [maxEntryBytes] are never stored.
 * Concurrent misses for one key share a single load, so a burst or a cache-bypassing client cannot multiply upstream
 * requests for the same data. Callers must build keys only from validated, normalized request data.
 */
internal class ResponseCache(
    private val ttlMillis: Long,
    private val maxBytes: Long,
    private val maxEntryBytes: Int,
    private val clock: Clock = Clock.systemUTC(),
) {
    private class Entry(
        val response: CachedResponse,
        val expiresAt: Long,
    )

    private val entries = LinkedHashMap<String, Entry>(INITIAL_CAPACITY, LOAD_FACTOR, true)
    private var storedBytes = 0L
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<LoadResult>>()

    /** Either a cacheable response or an uncached outcome, such as an upstream error status. */
    sealed interface LoadResult {
        data class Hit(
            val response: CachedResponse,
        ) : LoadResult

        data class Uncached(
            val status: Int,
        ) : LoadResult
    }

    suspend fun getOrLoad(
        key: String,
        load: suspend () -> LoadResult,
    ): LoadResult {
        cached(key)?.let { return LoadResult.Hit(it) }
        val loading = CompletableDeferred<LoadResult>()
        val leader = inFlight.putIfAbsent(key, loading)
        if (leader != null) return awaitLeader(key, leader, load)
        try {
            val result = load()
            if (result is LoadResult.Hit) store(key, result.response)
            loading.complete(result)
            return result
        } catch (e: Throwable) {
            // Waiters observe the same failure; cancellation of this caller is handled by each waiter below.
            loading.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key, loading)
        }
    }

    private suspend fun awaitLeader(
        key: String,
        leader: CompletableDeferred<LoadResult>,
        load: suspend () -> LoadResult,
    ): LoadResult =
        try {
            leader.await()
        } catch (e: CancellationException) {
            // The leading request was cancelled, for example by a client disconnect; an active waiter loads itself.
            currentCoroutineContext().ensureActive()
            getOrLoad(key, load)
        }

    private fun cached(key: String): CachedResponse? =
        synchronized(entries) {
            val entry = entries[key] ?: return null
            if (clock.millis() < entry.expiresAt) return entry.response
            entries.remove(key)
            storedBytes -= entry.response.body.size
            null
        }

    private fun store(
        key: String,
        response: CachedResponse,
    ) {
        val size = response.body.size
        if (size > maxEntryBytes) return
        synchronized(entries) {
            entries.remove(key)?.let { storedBytes -= it.response.body.size }
            entries[key] = Entry(response, clock.millis() + ttlMillis)
            storedBytes += size
            val iterator = entries.values.iterator()
            while (storedBytes > maxBytes && iterator.hasNext()) {
                storedBytes -=
                    iterator
                        .next()
                        .response.body.size
                iterator.remove()
            }
        }
    }

    private companion object {
        const val INITIAL_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
    }
}
