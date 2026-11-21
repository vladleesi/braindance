// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens

private const val PREVIEW_ROWS = 20

@Composable
internal actual fun ListScrollbar(
    state: LazyListState,
    modifier: Modifier,
    orientation: Orientation,
) {
    // A window used only by touch keeps the space for the list.
    if (!rememberMediaQueryMatches(FINE_POINTER_QUERY)) return
    val canScroll by remember(state) {
        derivedStateOf(structuralEqualityPolicy()) { state.canScrollForward || state.canScrollBackward }
    }
    val adapter = rememberScrollbarAdapter(state)
    // The gutter is always there, so the list keeps its size whether or not it overflows. The wheel over the
    // scrollbar scrolls the list, as over the list itself.
    if (orientation == Orientation.Vertical) {
        Box(
            modifier =
                modifier
                    .fillMaxHeight()
                    .padding(start = Dimens.tiny)
                    .width(PageScrollbarStyle.thickness),
        ) {
            if (canScroll) {
                VerticalScrollbar(
                    adapter = adapter,
                    modifier = Modifier.fillMaxHeight().scrollable(state, orientation, reverseDirection = true),
                    style = PageScrollbarStyle,
                )
            }
        }
    } else {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.tiny)
                    .height(PageScrollbarStyle.thickness),
        ) {
            if (canScroll) {
                HorizontalScrollbar(
                    adapter = adapter,
                    modifier = Modifier.fillMaxWidth().scrollable(state, orientation, reverseDirection = true),
                    style = PageScrollbarStyle,
                )
            }
        }
    }
}

@Preview
@Composable
internal fun ListScrollbarPreview() {
    BraindanceTheme {
        val state = rememberLazyListState()
        Row(modifier = Modifier.height(Dimens.extraLarge * 2)) {
            LazyColumn(state = state, modifier = Modifier.weight(1f)) {
                items(PREVIEW_ROWS) { row -> Text("Row ${row + 1}", modifier = Modifier.padding(Dimens.tiny)) }
            }
            ListScrollbar(state)
        }
    }
}
