// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ShutdownTest {
    @Test
    fun stoppingTheApplicationClosesEachResourceOnceInOrder() {
        val closed = mutableListOf<String>()
        val first = AutoCloseable { closed += "first" }
        val failing =
            AutoCloseable {
                closed += "failing"
                error("close failed")
            }
        val last = AutoCloseable { closed += "last" }

        testApplication {
            application { closeOnStop(first, failing, null, last) }
            startApplication()
        }

        assertEquals(listOf("first", "failing", "last"), closed)
    }
}
