// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.engine.js.JsError
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** On the Web, a request the browser cannot complete must end as a network failure, not escape its caller. */
class BrowserFetchFailureTest {
    @Test
    fun rejectedFetchIsNetworkFailure() =
        runTest {
            // Nothing listens on port 1, so the browser rejects the fetch without reaching any server.
            val client = HttpClient(Js) { expectSuccess = true }
            val outcome = safeCall { client.get("http://127.0.0.1:1/health") }
            client.close()

            assertEquals(DataError.Network, assertIs<Outcome.Failure>(outcome).error)
        }

    @Test
    fun engineFetchErrorIsNetworkFailure() =
        runTest {
            val outcome = safeCall { throw Error("Fail to fetch", JsError("TypeError".toJsString())) }

            assertEquals(DataError.Network, assertIs<Outcome.Failure>(outcome).error)
        }

    @Test
    fun otherErrorsStillPropagate() =
        runTest {
            assertFailsWith<Error> { safeCall { throw Error("bug") } }
        }
}
