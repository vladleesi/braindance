// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import java.text.DateFormat
import java.util.Date
import kotlin.time.Instant

internal actual fun formatEventDate(
    epochSeconds: Long,
    timeOnly: Boolean,
): String {
    val formatter =
        if (timeOnly) {
            DateFormat.getTimeInstance(DateFormat.SHORT)
        } else {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        }
    return formatter.format(Date(Instant.fromEpochSeconds(epochSeconds).toEpochMilliseconds()))
}
