// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/** Shows a scrollbar while a column, lazy list, or lazy grid scrolls in the desktop Web layout. */
@Composable
internal expect fun BoxScope.PageScrollbar(state: ScrollableState)
