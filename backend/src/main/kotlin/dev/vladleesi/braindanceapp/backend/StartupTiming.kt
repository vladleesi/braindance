// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.application.install
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal class StartupTiming {
    private val started = System.nanoTime()

    init {
        val uptime = ManagementFactory.getRuntimeMXBean().uptime
        println("{\"event\":\"backend_startup\",\"phase\":\"jvm_to_main\",\"duration_ms\":$uptime}")
    }

    fun <T> measure(
        phase: String,
        block: () -> T,
    ): T {
        val phaseStarted = System.nanoTime()
        var succeeded = false
        try {
            return block().also { succeeded = true }
        } finally {
            write(phase, phaseStarted, succeeded)
        }
    }

    fun checkpoint(phase: String) {
        write(phase, null, true)
    }

    private fun write(
        phase: String,
        phaseStarted: Long?,
        succeeded: Boolean,
    ) {
        val now = System.nanoTime()
        val duration = phaseStarted?.let { ",\"duration_ms\":${TimeUnit.NANOSECONDS.toMillis(now - it)}" }.orEmpty()
        val elapsed = TimeUnit.NANOSECONDS.toMillis(now - started)
        println(
            "{\"event\":\"backend_startup\",\"phase\":\"$phase\"$duration," +
                "\"elapsed_ms\":$elapsed,\"succeeded\":$succeeded}",
        )
    }
}

internal fun Application.installRequestTiming() {
    val started = AttributeKey<Long>("RequestStarted")
    val logged = ConcurrentHashMap.newKeySet<String>()
    install(
        createApplicationPlugin("RequestTiming") {
            onCall { call -> call.attributes.put(started, System.nanoTime()) }
            on(ResponseSent) { call ->
                val path = call.request.path()
                val route =
                    when {
                        path in gameQueryParameters -> path
                        path in setOf("/health", "/healthz", "/v1/giveaways", "/v1/giveaways/image") -> path
                        validGiveawayId(path) -> "/v1/giveaways/{id}"
                        else -> "other"
                    }
                // One completed request per known route and process; never log visitor URLs, headers or bodies.
                if (logged.add(route)) {
                    val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - call.attributes[started])
                    println(
                        "{\"event\":\"backend_first_request\",\"route\":\"$route\"," +
                            "\"status\":${call.response.status()?.value},\"duration_ms\":$duration}",
                    )
                }
            }
        },
    )
}
