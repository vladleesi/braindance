// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Dimens {
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

    val topBarHeight = 82.dp

    // The widest a block of running text gets: about 80 characters a line, however wide the window is. Pages that are
    // a single column of text or rows, such as Profile, use it as their width.
    val readableTextWidth = 720.dp

    // The widest the content of a page gets. Wider windows center it, so on ultrawide monitors the grids keep their
    // card sizes and the top bar's title and actions stay at the content's edges.
    val pageMaxWidth = 1440.dp

    val statusBarInsets
        @Composable
        get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val topBarHeightWithInsets: Dp
        @Composable
        get() = statusBarInsets + topBarHeight
}
