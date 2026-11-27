// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Discover's game search: the typed [text] and the searches submitted with Enter or the keyboard's search key. It is
 * held above the screens, so the search field in the Web desktop header and Discover share one search, and typing in
 * the header shows its results on Discover. The text survives recreation, and submissions do not need to.
 */
@Stable
class SearchQueryState(
    initialText: String = "",
) {
    var text by mutableStateOf(initialText)

    /** Counts the searches submitted with [submit], so Discover searches [text] at once instead of after typing. */
    var submissions by mutableIntStateOf(0)
        private set

    fun submit() {
        submissions++
    }

    companion object {
        val Saver: Saver<SearchQueryState, String> = Saver(save = { it.text }, restore = { SearchQueryState(it) })
    }
}

/**
 * The search the navigation scaffold shares with its screens and the desktop header. Null outside the scaffold, as in
 * previews and tests, where [rememberSearchQuery] gives Discover a search of its own.
 */
val LocalSearchQuery = compositionLocalOf<SearchQueryState?> { null }

/** The scaffold's shared search, or one remembered here when no scaffold provides it. */
@Composable
fun rememberSearchQuery(): SearchQueryState =
    LocalSearchQuery.current ?: rememberSaveable(saver = SearchQueryState.Saver) { SearchQueryState() }
