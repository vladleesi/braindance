// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/**
 * Per-client token buckets for routes whose cache misses cost IGDB calls. Each client may send [burst] requests at once
 * and [perMinute] on average. State is process-local and bounded to [maxClients]; the least recently seen clients are
 * forgotten first, which only ever lets a request through.
 */
internal class ClientRateLimiter(
    private val burst: Int = DEFAULT_BURST,
    private val perMinute: Int = DEFAULT_PER_MINUTE,
    private val maxClients: Int = DEFAULT_MAX_CLIENTS,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private class Bucket(
        var tokens: Double,
        var updatedAt: Long,
    )

    private val buckets =
        object : LinkedHashMap<String, Bucket>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bucket>) = size > maxClients
        }

    private val tokensPerNano = perMinute / TimeUnit.MINUTES.toNanos(1).toDouble()

    /** Takes one request for [client]: null when it is allowed, otherwise whole seconds until it would be. */
    fun take(client: String): Long? =
        synchronized(buckets) {
            val now = nanoTime()
            val bucket = buckets.getOrPut(client) { Bucket(burst.toDouble(), now) }
            bucket.tokens = minOf(burst.toDouble(), bucket.tokens + (now - bucket.updatedAt) * tokensPerNano)
            bucket.updatedAt = now
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1
                null
            } else {
                val waitNanos = (1 - bucket.tokens) / tokensPerNano
                ceil(waitNanos / TimeUnit.SECONDS.toNanos(1)).toLong().coerceAtLeast(1)
            }
        }

    private companion object {
        // A person browsing games opens far fewer than one per second; a script looping over IDs does not.
        const val DEFAULT_BURST = 20
        const val DEFAULT_PER_MINUTE = 60
        const val DEFAULT_MAX_CLIENTS = 10_000
        const val INITIAL_CAPACITY = 256
        const val LOAD_FACTOR = 0.75f
    }
}
