// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import kotlinx.datetime.toNSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import kotlin.time.Instant

internal actual fun formatEventDate(
    epochSeconds: Long,
    timeOnly: Boolean,
): String =
    NSDateFormatter.localizedStringFromDate(
        Instant.fromEpochSeconds(epochSeconds).toNSDate(),
        if (timeOnly) NSDateFormatterNoStyle else NSDateFormatterMediumStyle,
        NSDateFormatterShortStyle,
    )
