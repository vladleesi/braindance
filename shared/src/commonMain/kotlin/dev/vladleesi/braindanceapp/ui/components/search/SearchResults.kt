// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.discover_search_no_results
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.components.SectionError
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardLabel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.MoreResults
import dev.vladleesi.braindanceapp.utils.keyboardPadding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

// Three columns on phones; wider windows show larger covers, like the desktop Home grids.
private val CardMinWidth = 100.dp
private val LargeWindowCardMinWidth = 150.dp
private val ColumnGap = Dimens.compact
private val RowGap = Dimens.medium

/** Titles tell results apart, and a narrow card cuts one line after a few words, so results show two. */
private const val TITLE_LINES = 2

/** Placeholder rows while the first results load; the grid shows only those that fit. */
private const val LOADING_ROWS = 6

/** The next page starts loading this many cards before the end, about two rows, so scrolling rarely reaches it. */
private const val PREFETCH_DISTANCE = 10

private const val GAME_CARD_CONTENT_TYPE = "game_card"
private const val SKELETON_CONTENT_TYPE = "game_card_skeleton"
private const val FOOTER_CONTENT_TYPE = "footer"

private val GridPadding =
    PaddingValues(start = Dimens.medium, top = Dimens.tiny, end = Dimens.medium, bottom = Dimens.large)

/**
 * Search results as a grid of game cards in ranked order. While [more] results are available, placeholder cards
 * complete the last row and add one more, and the next page is requested through [onLoadMore] as the grid nears its
 * end. A failed page shows its error with [onRetry] below the loaded cards, which stay in place.
 */
@Composable
fun SearchResultsGrid(
    games: List<GameCardModel>,
    more: MoreResults,
    onGameClick: (id: Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
) {
    val loadMore by rememberUpdatedState(onLoadMore)
    val gameCount = games.size
    // Restarts only when cards are added or the paging state changes; scrolling is read outside composition.
    LaunchedEffect(state, gameCount, more) {
        if (more != MoreResults.Available) return@LaunchedEffect
        snapshotFlow {
            state.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
        }.map { lastVisible -> lastVisible >= gameCount - PREFETCH_DISTANCE }
            .distinctUntilChanged()
            .filter { nearEnd -> nearEnd }
            .collect { loadMore() }
    }
    SearchGrid(modifier = modifier, state = state) { columns ->
        items(games, key = { game -> game.id }, contentType = { GAME_CARD_CONTENT_TYPE }) { game ->
            GameCard(card = game, onClick = onGameClick, showTitle = true, titleLines = TITLE_LINES)
        }
        when (more) {
            MoreResults.Available, MoreResults.Loading -> {
                val placeholders = (columns - gameCount % columns) % columns + columns
                items(placeholders, key = { index -> "more-$index" }, contentType = { SKELETON_CONTENT_TYPE }) {
                    GameCardSkeleton(showTitle = true, titleLines = TITLE_LINES)
                }
            }
            is MoreResults.Failed ->
                item(key = "more-error", span = { GridItemSpan(maxLineSpan) }, contentType = FOOTER_CONTENT_TYPE) {
                    SectionError(
                        message = more.error.userMessage(),
                        onRetry = onRetry,
                        modifier = Modifier.padding(top = Dimens.small),
                    )
                }
            MoreResults.None -> Unit
        }
    }
}

/** Placeholder cards with the geometry of [SearchResultsGrid], shown until the first results arrive. */
@Composable
fun SearchResultsSkeleton(modifier: Modifier = Modifier) {
    SearchGrid(modifier = modifier, userScrollEnabled = false) { columns ->
        items(columns * LOADING_ROWS, contentType = { SKELETON_CONTENT_TYPE }) {
            GameCardSkeleton(showTitle = true, titleLines = TITLE_LINES)
        }
    }
}

/** A search that found nothing. */
@Composable
fun SearchNoResults(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.discover_search_no_results),
        style = MaterialTheme.typography.body2,
        modifier = modifier.padding(GridPadding),
    )
}

/** A search whose first results failed, with [onRetry]. */
@Composable
fun SearchError(
    error: DataError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionError(message = error.userMessage(), onRetry = onRetry, modifier = modifier.padding(GridPadding))
}

/**
 * The grid every search state shares, so placeholders and cards line up. It fits as many columns as the window allows
 * and passes their count to [content]. While the keyboard is open, the last rows can still scroll clear of it.
 */
@Composable
private fun SearchGrid(
    modifier: Modifier,
    state: LazyGridState = rememberLazyGridState(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.(columns: Int) -> Unit,
) {
    val minCardWidth = if (isLargeWindow()) LargeWindowCardMinWidth else CardMinWidth
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = columnsFor(maxWidth - Dimens.medium * 2, minCardWidth)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = state,
            modifier = Modifier.fillMaxSize().keyboardPadding(),
            contentPadding = GridPadding,
            horizontalArrangement = Arrangement.spacedBy(ColumnGap),
            verticalArrangement = Arrangement.spacedBy(RowGap),
            userScrollEnabled = userScrollEnabled,
        ) {
            content(columns)
        }
    }
}

private fun columnsFor(
    width: Dp,
    minCardWidth: Dp,
): Int = ((width + ColumnGap) / (minCardWidth + ColumnGap)).toInt().coerceAtLeast(1)

private const val PREVIEW_GAME_COUNT = 7
private const val PREVIEW_FIRST_YEAR = 2010

private val PreviewGames =
    List(PREVIEW_GAME_COUNT) { index ->
        GameCardModel(
            id = index + 1,
            title = "Sample game ${index + 1}",
            backgroundImageUrl = "",
            label = GameCardLabel.ReleaseYear(PREVIEW_FIRST_YEAR + index),
        )
    }

private val PreviewHeight = 640.dp

@Preview
@Composable
internal fun SearchResultsGridPreview() {
    BraindanceTheme {
        SearchResultsGrid(
            games = PreviewGames,
            more = MoreResults.Loading,
            onGameClick = {},
            onLoadMore = {},
            onRetry = {},
            modifier = Modifier.background(background).height(PreviewHeight),
        )
    }
}

@Preview
@Composable
internal fun SearchResultsStatesPreview() {
    BraindanceTheme {
        Column(modifier = Modifier.background(background)) {
            SearchNoResults()
            SearchError(error = DataError.Network, onRetry = {})
            SearchResultsSkeleton(modifier = Modifier.height(PreviewHeight))
        }
    }
}
