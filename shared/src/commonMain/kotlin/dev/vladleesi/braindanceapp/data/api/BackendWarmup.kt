// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.retry
import io.ktor.client.request.prepareGet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

private val WARM_UP_TIMEOUT = 10.seconds

internal fun warmUpBackend(client: () -> HttpClient) {
    // Resolve the shared client inside the coroutine so startup never waits for its initialization.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope
        .launch {
            try {
                withTimeout(WARM_UP_TIMEOUT) {
                    client()
                        .prepareGet("health") { retry { maxRetries = 0 } }
                        .execute { }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best effort only; normal requests and UI readiness are independent of this request.
            }
        }.invokeOnCompletion { scope.cancel() }
}
