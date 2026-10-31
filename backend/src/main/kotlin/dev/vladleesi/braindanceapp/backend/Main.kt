// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.server.application.ServerReady
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
        require(config.rateLimiterMode in setOf("redis", "in-memory")) { "Invalid IGDB_RATE_LIMITER" }
    }
    val redis =
        startup.measure("redis_pool") {
            if (config.rateLimiterMode == "redis") {
                require(!config.redisUrl.isNullOrBlank()) { "REDIS_URL is required for the global IGDB limit" }
                RedisClient.create(config.redisUrl)
            } else {
                null
            }
        }
    val limiter = startup.measure("rate_limiter_creation") { redis?.let(::RedisRateLimiter) ?: InMemoryRateLimiter() }
    val upstream =
        KtorUpstream {
            startup.measure("http_client_initialization") {
                HttpClient(CIO) {
                    expectSuccess = false
                    install(HttpTimeout)
                }
            }
        }
    val igdb = startup.measure("igdb_service_creation") { IgdbService(upstream, limiter, config) }
    val giveaways = startup.measure("giveaway_service_creation") { GiveawayService(upstream) }
    val recommendations = startup.measure("recommendation_service_creation") { RecommendationService(igdb) }
    val server =
        startup.measure("server_create") {
            embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
                monitor.subscribe(ServerReady) { startup.checkpoint("server_ready") }
                startup.measure("application_routes") { api(config, igdb, giveaways, recommendations) }
            }
        }
    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop()
            recommendations.close()
            upstream.close()
            redis?.close()
        },
    )
    try {
        startup.checkpoint("server_start")
        server.start(wait = true)
    } finally {
        recommendations.close()
        upstream.close()
        redis?.close()
    }
}
