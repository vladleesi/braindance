// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val DAYS_OF_MONTH = 30

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

/** The UTC year of these Unix seconds, such as a game's first release date, or null when out of range. */
@OptIn(ExperimentalTime::class)
fun Long.utcYearOrNull(): Int? =
    runCatching { Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.UTC).year }.getOrNull()

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

// A date as "Dec 31, 2026", the way the app writes release dates.
private val ShortDateFormat =
    LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        day(padding = Padding.NONE)
        chars(", ")
        year()
    }

/** This date as "Dec 31, 2026". */
fun LocalDate.formatShortDate(): String = format(ShortDateFormat)

/**
 * The date of a GamerPower timestamp such as "2026-12-31 23:59:00", or null for "N/A", an empty value, or anything
 * else it cannot read.
 */
fun String.gamerPowerDateOrNull(): LocalDate? = LocalDateTime.parseOrNull(trim().replace(" ", "T"))?.date

/** Today in the device's time zone. */
@OptIn(ExperimentalTime::class)
fun currentLocalDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
