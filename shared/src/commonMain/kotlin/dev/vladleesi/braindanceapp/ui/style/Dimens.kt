// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.system.desktopLayout

object Dimens {
    private val mobileTopBarHeight = 82.dp
    private val desktopTopBarHeight = 56.dp
    val one = 1.dp
    val micro = 2.dp
    val tiny = 4.dp
    val small = 8.dp
    val compact = 12.dp
    val medium = 16.dp
    val large = 32.dp
    val extraLarge = 64.dp

    val gameCardWidth = 144.dp
    val gameCardHeight = 192.dp

    val giveAwayCardWidth = 200.dp

    val giveAwayCardDetailsWidth = 300.dp
    val giveAwayCardDetailsHeight = 141.dp

    // A screen's own bar. In the desktop layout it sits under the header, so it is as compact as the header. Phones and
    // tablets give their large titles more room.
    val topBarHeight: Dp
        @Composable
        get() = if (desktopLayout() != null) desktopTopBarHeight else mobileTopBarHeight

    // The bar over a page's hero (game, giveaway, and event pages) and over their loading and error states: the same on
    // every layout, so Back stays in one place while a page loads.
    val heroTopBarHeight = mobileTopBarHeight

    // The widest a block of running text gets: about 80 characters a line, however wide the window is. Pages that are
    // a single column of text or rows, such as Profile, use it as their width.
    val readableTextWidth = 720.dp

    // The widest the content of a page gets. Grids add columns up to it, so desktop windows show more games rather
    // than empty space, and only windows wider than about 2200 dp have spare width. It is centered beside the
    // navigation, so on ultrawide monitors the top bar's title and actions stay at the content's edges.
    val pageMaxWidth = 1920.dp

    val statusBarInsets
        @Composable
        get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val topBarHeightWithInsets: Dp
        @Composable
        get() = statusBarInsets + topBarHeight

    val heroTopBarHeightWithInsets: Dp
        @Composable
        get() = statusBarInsets + heroTopBarHeight
}
