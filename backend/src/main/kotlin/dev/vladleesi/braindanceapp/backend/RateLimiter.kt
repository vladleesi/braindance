// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import redis.clients.jedis.RedisClient
import redis.clients.jedis.exceptions.JedisNoScriptException
import java.util.UUID

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

/**
 * One Redis key is shared by every replica, and the Lua script makes the sliding-window decision atomic. The script is
 * loaded once and then run by its SHA, so each call does not resend it.
 */
class RedisRateLimiter(
    private val redis: RedisClient,
    private val key: String = "braindance:igdb:rate",
) : RateLimiter {
    @Volatile private var scriptSha: String? = null

    override suspend fun takeSlot(): Boolean =
        withContext(Dispatchers.IO) {
            val reservation = UUID.randomUUID().toString()
            val result =
                try {
                    redis.evalsha(loadedScript(), 1, key, reservation)
                } catch (_: JedisNoScriptException) {
                    // Redis dropped its script cache, for example after a restart or SCRIPT FLUSH; load it again once.
                    scriptSha = null
                    redis.evalsha(loadedScript(), 1, key, reservation)
                }
            result == 1L
        }

    private fun loadedScript(): String = scriptSha ?: redis.scriptLoad(SCRIPT).also { scriptSha = it }

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
