// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import io.ktor.util.date.GMTDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val DAYS_OF_MONTH = 30

val currentYear: Int
    get() = GMTDate().year

val lastYear: Int
    get() = GMTDate().year.dec()

// TODO: Add reusable formats
@OptIn(ExperimentalTime::class)
fun Long.formatDate(): String? {
    val format =
        DateTimeComponents.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            day()
            char(',')
            char(' ')
            year()
        }

    return runCatching { Instant.fromEpochSeconds(this).format(format) }.getOrNull()
}

@OptIn(ExperimentalTime::class)
fun String.formatEndDate(): String? {
    val formattedEndDate = this.replace(" ", "T")
    val endDateTime = LocalDateTime.parseOrNull(formattedEndDate) ?: return null
    val daysUntilEnd =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .daysUntil(endDateTime.date)

    return when {
        daysUntilEnd > DAYS_OF_MONTH -> null
        daysUntilEnd == DAYS_OF_MONTH -> "1 month"
        daysUntilEnd > 1 -> "$daysUntilEnd days"
        daysUntilEnd == 1 -> "$daysUntilEnd day"
        else -> "Ends soon"
    }
}

private fun LocalDateTime.Companion.parseOrNull(value: String): LocalDateTime? =
    runCatching { parse(value) }.getOrNull()
