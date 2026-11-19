// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * Shows a scrollbar while a column, lazy list, or lazy grid scrolls in the desktop Web layout, and on the Web lets
 * middle-click autoscrolling move the page. Call it once per scrolling page, in the box that holds the page.
 */
@Composable
internal expect fun BoxScope.PageScrollbar(state: ScrollableState)
