// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.runtime.Composable
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.error_network
import dev.vladleesi.braindanceapp.resources.error_not_found
import dev.vladleesi.braindanceapp.resources.error_rate_limited
import dev.vladleesi.braindanceapp.resources.error_screen_error_message
import dev.vladleesi.braindanceapp.resources.error_server
import dev.vladleesi.braindanceapp.resources.error_timeout
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The localized explanation users see for this failure; transport details are logged, never shown. */
@Composable
fun DataError.userMessage(): String = stringResource(messageResource())

internal fun DataError.messageResource(): StringResource =
    when (this) {
        DataError.Network -> Res.string.error_network
        DataError.Timeout -> Res.string.error_timeout
        DataError.NotFound -> Res.string.error_not_found
        DataError.RateLimited -> Res.string.error_rate_limited
        DataError.Server -> Res.string.error_server
        DataError.Unknown -> Res.string.error_screen_error_message
    }
