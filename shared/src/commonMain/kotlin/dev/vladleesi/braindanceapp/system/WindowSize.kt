// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** Medium width in the Material window size classes: tablets, landscape phones, and wide browser windows. */
private val LARGE_WINDOW_WIDTH = 600.dp

/**
 * Whether the app's window is at least medium width. It follows the window rather than the device, so it updates when
 * a browser window is resized or a device rotates.
 */
@Composable
fun isLargeWindow(): Boolean {
    val widthPx = LocalWindowInfo.current.containerSize.width
    return with(LocalDensity.current) { widthPx.toDp() } >= LARGE_WINDOW_WIDTH
}
