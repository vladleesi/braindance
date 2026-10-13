// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import redis.clients.jedis.JedisPooled

fun interface RateLimiter {
    suspend fun takeSlot(): Boolean
}

/** Process-local only: overlapping instances do not share reservations. */
class InMemoryRateLimiter(
    private val nanoTime: () -> Long = System::nanoTime,
) : RateLimiter {
    private val mutex = Mutex()
    private val reservations = ArrayDeque<Long>()

    override suspend fun takeSlot(): Boolean =
        mutex.withLock {
            val now = nanoTime()
            while (reservations.isNotEmpty() && now - reservations.first() >= WINDOW_NANOS) {
                reservations.removeFirst()
            }
            if (reservations.size >= MAX_REQUESTS) {
                false
            } else {
                reservations.addLast(now)
                true
            }
        }

    private companion object {
        const val MAX_REQUESTS = 4
        const val WINDOW_NANOS = 1_000_000_000L
    }
}

/** One Redis key is shared by every replica. Redis EVAL makes the sliding-window decision atomic. */
class RedisRateLimiter(
    private val redis: JedisPooled,
    private val key: String = "braindance:igdb:rate",
) : RateLimiter {
    override suspend fun takeSlot(): Boolean =
        withContext(Dispatchers.IO) {
            val result =
                redis.eval(
                    SCRIPT,
                    1,
                    key,
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                )
            result == 1L
        }

    companion object {
        private val SCRIPT =
            """
            local key = KEYS[1]
            local now = redis.call('TIME')
            local ms = now[1] * 1000 + math.floor(now[2] / 1000)
            redis.call('ZREMRANGEBYSCORE', key, '-inf', ms - 1000)
            if redis.call('ZCARD', key) >= 4 then return 0 end
            redis.call('ZADD', key, ms, ARGV[1])
            redis.call('PEXPIRE', key, 1000)
            return 1
            """.trimIndent()
    }
}
