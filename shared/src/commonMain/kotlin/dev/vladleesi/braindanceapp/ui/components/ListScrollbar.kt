// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A scrollbar for a list of fixed size inside a page, such as the media gallery's thumbnails, for mouse users on the
 * Web: beside a vertical list, placed after it in a row, or under a horizontal one, placed after it in a column.
 * Unlike [PageScrollbar], it stays visible while the list
 * can scroll, so the list shows that it holds more and where it is. Like CSS `scrollbar-gutter: stable`, it keeps its
 * space even when the list fits, so the list's width never changes. Touch screens, Android, and iOS scroll the list by
 * swiping, and there it takes no space.
 */
@Composable
internal expect fun ListScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
)
