// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Lists scroll by swiping here; the scrollbar takes no space.
@Composable
internal actual fun ListScrollbar(
    state: LazyListState,
    modifier: Modifier,
) = Unit
