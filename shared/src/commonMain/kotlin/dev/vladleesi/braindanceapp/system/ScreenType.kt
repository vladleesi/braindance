// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable

/**
 * Determines if the device is classified as a large device (e.g., tablet or iPad).
 *
 * @return `true` if the device is a tablet or has a large screen, otherwise `false`.
 */
@Composable
expect fun isLargeDevice(): Boolean
