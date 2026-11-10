// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Medium width in the Material window size classes: tablets, landscape phones, and wide browser windows. */
private val LARGE_WINDOW_WIDTH = 600.dp

// Desktop tiers. They follow the cards' minimum widths: below the first one the grids would drop to two narrow cards.
private val DESKTOP_COMPACT_WIDTH = 768.dp
private val DESKTOP_SIDEBAR_WIDTH = 1100.dp
private val DESKTOP_WIDE_WIDTH = 1440.dp

/**
 * Whether the host shows the desktop presentation in wide windows. Only the Web host enables it, so Android and iOS
 * keep their layouts at every size.
 */
val LocalDesktopLayoutEnabled = staticCompositionLocalOf { false }

/** The desktop presentation for the current window width; the narrowest tier keeps the collapsed navigation rail. */
enum class DesktopLayout {
    /** The navigation rail with desktop content grids. */
    Compact,

    /** The navigation sidebar with desktop content grids. */
    Sidebar,

    /** The navigation sidebar, the content grids, and a column of contextual modules. */
    Wide,
    ;

    val hasSidebar: Boolean get() = this != Compact
    val hasContextColumn: Boolean get() = this == Wide
}

/**
 * Whether the app's window is at least medium width. It follows the window rather than the device, so it updates when
 * a browser window is resized or a device rotates.
 */
@Composable
fun isLargeWindow(): Boolean = windowWidth() >= LARGE_WINDOW_WIDTH

/**
 * The desktop presentation for the current window, or null when the host does not enable it or the window is too
 * narrow, in which case screens keep their standard layout. Like [isLargeWindow], it updates on resize.
 */
@Composable
fun desktopLayout(): DesktopLayout? {
    if (!LocalDesktopLayoutEnabled.current) return null
    val width = windowWidth()
    return when {
        width >= DESKTOP_WIDE_WIDTH -> DesktopLayout.Wide
        width >= DESKTOP_SIDEBAR_WIDTH -> DesktopLayout.Sidebar
        width >= DESKTOP_COMPACT_WIDTH -> DesktopLayout.Compact
        else -> null
    }
}

@Composable
private fun windowWidth(): Dp {
    val widthPx = LocalWindowInfo.current.containerSize.width
    return with(LocalDensity.current) { widthPx.toDp() }
}
