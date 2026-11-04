// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Why a Twitch token could not be obtained; logged so credential and upstream failures are distinguishable. */
internal class TokenUnavailable(
    val reason: Reason,
    val upstreamStatus: Int? = null,
) : Exception("Twitch token unavailable: ${reason.name.lowercase()}") {
    enum class Reason { REQUEST_FAILED, RESPONSE_INVALID }
}

/**
 * Structured failure lines that would otherwise be invisible behind a generic 502 or 429. Each line names the event,
 * its route or upstream operation, and the exception class or upstream status, and never contains visitor URLs,
 * query strings, headers, bodies, or exception messages, which can carry upstream URLs.
 *
 * Lines are rate-limited per event and subject, so an outage or abusive traffic cannot flood the log: within
 * [interval] only the first occurrence is written, and the next written line reports how many were suppressed.
 */
class FailureLog(
    private val log: (String) -> Unit = ::println,
    private val nanoTime: () -> Long = System::nanoTime,
    private val interval: Long = TimeUnit.SECONDS.toNanos(1),
) {
    private class Window(
        var writtenAt: Long,
        var suppressed: Int,
    )

    // Keys are an event name plus a registered route or a fixed operation name, so the map stays small.
    private val windows = ConcurrentHashMap<String, Window>()

    /** A request ended in an unexpected exception on [route], the registered route or `other`. */
    fun requestFailed(
        route: String,
        cause: Throwable,
    ) {
        write("request_failed", "route", route) {
            put("error", cause::class.simpleName ?: "unknown")
            if (cause is TokenUnavailable) {
                put("reason", cause.reason.name.lowercase())
                cause.upstreamStatus?.let { put("upstream_status", it) }
            }
        }
    }

    /** [operation] returned an unusable answer, such as a 5xx status or an unexpected content type. */
    fun upstreamFailed(
        operation: String,
        status: Int,
        reason: String = "status",
    ) {
        write("upstream_failed", "operation", operation) {
            put("upstream_status", status)
            put("reason", reason)
        }
    }

    /** The shared IGDB rate limiter refused a slot, so the request was answered with 429. */
    fun limiterRejected(operation: String) {
        write("limiter_rejected", "operation", operation) {}
    }

    private fun write(
        event: String,
        subjectField: String,
        subject: String,
        fields: JsonObjectBuilder.() -> Unit,
    ) {
        val now = nanoTime()
        val window = windows.computeIfAbsent("$event\n$subject") { Window(now - interval, 0) }
        val suppressed =
            synchronized(window) {
                if (now - window.writtenAt < interval) {
                    window.suppressed++
                    return
                }
                window.writtenAt = now
                window.suppressed.also { window.suppressed = 0 }
            }
        val line =
            buildJsonObject {
                put("event", JsonPrimitive(event))
                put(subjectField, JsonPrimitive(subject))
                fields()
                if (suppressed > 0) put("suppressed", suppressed)
            }
        log(line.toString())
    }
}
