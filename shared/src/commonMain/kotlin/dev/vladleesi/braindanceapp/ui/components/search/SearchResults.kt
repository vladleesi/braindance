// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.discover_search_no_results
import dev.vladleesi.braindanceapp.ui.components.PagedGrid
import dev.vladleesi.braindanceapp.ui.components.PagedGridPadding
import dev.vladleesi.braindanceapp.ui.components.PagedGridSkeleton
import dev.vladleesi.braindanceapp.ui.components.SectionError
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardLabel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.MoreResults
import org.jetbrains.compose.resources.stringResource

/** Titles tell results apart, and a narrow card cuts one line after a few words, so results show two. */
private const val TITLE_LINES = 2

/**
 * Search results as a [PagedGrid] of game cards in ranked order: the next page loads as the grid nears its end, and a
 * failed page shows its error with [onRetry] below the loaded cards, which stay in place.
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
    PagedGrid(
        items = games,
        more = more,
        itemKey = GameCardModel::id,
        onLoadMore = onLoadMore,
        onRetry = onRetry,
        skeleton = { GameCardSkeleton(showTitle = true, titleLines = TITLE_LINES) },
        modifier = modifier,
        state = state,
    ) { game ->
        GameCard(card = game, onClick = onGameClick, showTitle = true, titleLines = TITLE_LINES)
    }
}

/** Placeholder cards with the geometry of [SearchResultsGrid], shown until the first results arrive. */
@Composable
fun SearchResultsSkeleton(modifier: Modifier = Modifier) {
    PagedGridSkeleton(skeleton = { GameCardSkeleton(showTitle = true, titleLines = TITLE_LINES) }, modifier = modifier)
}

/** A search that found nothing. */
@Composable
fun SearchNoResults(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.discover_search_no_results),
        style = MaterialTheme.typography.body2,
        modifier = modifier.padding(PagedGridPadding),
    )
}

/** A search whose first results failed, with [onRetry]. */
@Composable
fun SearchError(
    error: DataError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionError(message = error.userMessage(), onRetry = onRetry, modifier = modifier.padding(PagedGridPadding))
}

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
