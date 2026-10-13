// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun HorizontalListControls(
    listState: LazyListState,
    modifier: Modifier = Modifier,
)
