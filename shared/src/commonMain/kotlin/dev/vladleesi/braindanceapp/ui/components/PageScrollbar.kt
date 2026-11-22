// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shows a scrollbar while a column, lazy list, or lazy grid scrolls in the desktop Web layout, and on the Web lets
 * middle-click autoscrolling move the page. Use [topInset] when a pinned toolbar covers the scrollable content.
 */
@Composable
internal expect fun BoxScope.PageScrollbar(
    state: ScrollableState,
    topInset: Dp = 0.dp,
)
