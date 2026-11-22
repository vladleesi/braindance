// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

@Composable
internal actual fun BoxScope.PageScrollbar(
    state: ScrollableState,
    topInset: Dp,
) = Unit
