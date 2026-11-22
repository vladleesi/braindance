// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

/** Formats Unix seconds with the platform's current locale and time zone. */
internal expect fun formatEventDate(
    epochSeconds: Long,
    timeOnly: Boolean,
): String
