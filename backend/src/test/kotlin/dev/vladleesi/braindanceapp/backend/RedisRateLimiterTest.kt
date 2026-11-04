// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import redis.clients.jedis.RedisClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedisRateLimiterTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_REDIS_URL", matches = ".+")
    fun limitIsSharedByConcurrentReplicas() =
        runBlocking {
            val url = requireNotNull(System.getenv("TEST_REDIS_URL"))
            RedisClient.create(url).use { first ->
                RedisClient.create(url).use { second ->
                    val key = "braindance:test:${java.util.UUID.randomUUID()}"
                    val replicas = listOf(RedisRateLimiter(first, key), RedisRateLimiter(second, key))
                    val decisions =
                        (0 until 8)
                            .map { index ->
                                async { replicas[index % 2].takeSlot() }
                            }.awaitAll()
                    assertEquals(4, decisions.count { it })
                    kotlinx.coroutines.delay(1010)
                    assertTrue(replicas.first().takeSlot())
                }
            }
        }

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_REDIS_URL", matches = ".+")
    fun limiterRecoversWhenRedisDropsItsScriptCache() =
        runBlocking {
            val url = requireNotNull(System.getenv("TEST_REDIS_URL"))
            RedisClient.create(url).use { redis ->
                val limiter = RedisRateLimiter(redis, "braindance:test:${java.util.UUID.randomUUID()}")
                assertTrue(limiter.takeSlot())
                // Simulates a Redis restart: the cached SHA no longer names a loaded script.
                redis.scriptFlush()
                assertTrue(limiter.takeSlot())
                assertTrue(limiter.takeSlot())
            }
        }
}
