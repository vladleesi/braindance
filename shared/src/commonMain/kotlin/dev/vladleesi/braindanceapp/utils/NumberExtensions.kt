// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

fun Int?.orZero(): Int = this ?: 0

private const val DIGITS_PER_GROUP = 3

/** This count with a comma between thousands, such as 12,500, as the app's English text writes numbers. */
fun Int.withThousandsSeparators(): String {
    val digits = toString().removePrefix("-")
    val grouped =
        digits
            .reversed()
            .chunked(DIGITS_PER_GROUP)
            .joinToString(",")
            .reversed()
    return if (this < 0) "-$grouped" else grouped
}
