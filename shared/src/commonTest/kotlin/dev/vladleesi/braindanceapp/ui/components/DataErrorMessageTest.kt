// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.error_screen_error_message
import kotlin.test.Test
import kotlin.test.assertEquals

class DataErrorMessageTest {
    private val allErrors =
        listOf(
            DataError.Network,
            DataError.Timeout,
            DataError.NotFound,
            DataError.RateLimited,
            DataError.Server,
            DataError.Unknown,
        )

    @Test
    fun everyErrorHasItsOwnMessage() {
        assertEquals(allErrors.size, allErrors.map { it.messageResource() }.toSet().size)
    }

    @Test
    fun unknownFailuresUseTheGenericMessage() {
        assertEquals(Res.string.error_screen_error_message, DataError.Unknown.messageResource())
    }
}
