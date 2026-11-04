// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ServerReady
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import redis.clients.jedis.RedisClient

fun main() {
    val startup = StartupTiming()
    val config = startup.measure("environment") { BackendConfig.fromEnvironment() }
    startup.measure("config_validation") {
        config.publicBaseUrl?.let { base ->
            val uri = java.net.URI(base)
            require(
                uri.scheme in setOf("http", "https") &&
                    uri.host != null &&
                    uri.userInfo == null &&
                    uri.path.isNullOrEmpty() &&
                    uri.query == null &&
                    uri.fragment == null,
            ) { "PUBLIC_BASE_URL must be an HTTP origin" }
        }
    }
    val redis =
        startup.measure("redis_pool") {
            if (config.rateLimiterMode == RateLimiterMode.REDIS) {
                require(!config.redisUrl.isNullOrBlank()) { "REDIS_URL is required for the global IGDB limit" }
                RedisClient.create(config.redisUrl)
            } else {
                null
            }
        }
    val limiter = startup.measure("rate_limiter_creation") { redis?.let(::RedisRateLimiter) ?: InMemoryRateLimiter() }
    val failures = FailureLog()
    val upstream =
        KtorUpstream {
            startup.measure("http_client_initialization") {
                HttpClient(CIO) { upstreamDefaults() }
            }
        }
    val perGameLimiter =
        redis?.let { RedisRateLimiter(it, PER_GAME_RATE_KEY, PER_GAME_REQUESTS_PER_SECOND) }
            ?: InMemoryRateLimiter(PER_GAME_REQUESTS_PER_SECOND)
    val igdb =
        startup.measure("igdb_service_creation") {
            IgdbService(upstream, limiter, config, failures = failures, perGameLimiter = perGameLimiter)
        }
    if (config.cloudflareOriginSecret == null) {
        // Behind a proxy without the origin guard, clients cannot be told apart; the edge must limit them.
        println("{\"event\":\"backend_config\",\"per_client_rate_limit\":\"off\",\"reason\":\"no_origin_guard\"}")
    }
    val giveaways = startup.measure("giveaway_service_creation") { GiveawayService(upstream) }
    val recommendations = startup.measure("recommendation_service_creation") { RecommendationService(igdb) }
    val trending = TrendingService(igdb)
    val server =
        startup.measure("server_create") {
            embeddedServer(
                Netty,
                configure = {
                    connector {
                        port = config.port
                        host = "0.0.0.0"
                    }
                    // Ktor's own shutdown hook stops the server with these limits. Requests in flight get two seconds
                    // to finish, and shutdown completes within Cloud Run's ten-second SIGTERM window.
                    shutdownGracePeriod = SHUTDOWN_GRACE_MILLIS
                    shutdownTimeout = SHUTDOWN_TIMEOUT_MILLIS
                },
            ) {
                monitor.subscribe(ServerReady) { startup.checkpoint("server_ready") }
                // The single owner of shutdown: background work first, then the clients it uses.
                closeOnStop(recommendations, trending, upstream, redis)
                startup.measure("application_routes") {
                    api(config, igdb, giveaways, recommendations, trending, failures)
                }
            }
        }
    startup.checkpoint("server_start")
    server.start(wait = true)
}

private const val PER_GAME_RATE_KEY = "braindance:igdb:rate:per-game"
private const val SHUTDOWN_GRACE_MILLIS = 2_000L
private const val SHUTDOWN_TIMEOUT_MILLIS = 8_000L

/**
 * Closes [resources] in order when the application stops. A failure to close one does not leave the others open; the
 * process is exiting, so it is not reported further.
 */
internal fun Application.closeOnStop(vararg resources: AutoCloseable?) {
    monitor.subscribe(ApplicationStopped) {
        resources.forEach { resource -> runCatching { resource?.close() } }
    }
}
