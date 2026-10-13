// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

private const val LARGE_SCREEN_WIDTH_DP = 600

/**
 * Determines if the device is classified as a large device (e.g., tablet or iPad).
 *
 * - 600dp: Represents medium width, typically tablets in portrait.
 * - 840dp: Represents expanded width, typically tablets in landscape.
 *
 * For more details, refer to the Android Developer guide:
 * https://developer.android.com/develop/ui/views/layout/use-window-size-classes
 *
 * @return `true` if the device is a tablet or has a large screen, otherwise `false`.
 */
@Composable
actual fun isLargeDevice(): Boolean {
    val configuration = LocalConfiguration.current
    val screenWidthDp = remember(configuration) { configuration.screenWidthDp }
    val smallestScreenWidthDp = remember(configuration) { configuration.smallestScreenWidthDp }

    // Check screen width and smallest screen width to classify large devices
    return screenWidthDp >= LARGE_SCREEN_WIDTH_DP || smallestScreenWidthDp >= LARGE_SCREEN_WIDTH_DP
}
