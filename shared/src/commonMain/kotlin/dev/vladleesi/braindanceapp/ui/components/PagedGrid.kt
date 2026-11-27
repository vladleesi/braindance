// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.MoreResults
import dev.vladleesi.braindanceapp.utils.keyboardPadding
import dev.vladleesi.braindanceapp.utils.rememberCachedLazyGridState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private val ColumnGap = Dimens.compact
private val RowGap = Dimens.medium

/** Placeholder rows while the first page loads; the grid shows only those that fit. */
internal const val LOADING_ROWS = 6

/** The next page starts loading this many cards before the end, about two rows, so scrolling rarely reaches it. */
private const val PREFETCH_DISTANCE = 10

private const val CARD_CONTENT_TYPE = "card"
private const val SKELETON_CONTENT_TYPE = "card_skeleton"
private const val FOOTER_CONTENT_TYPE = "footer"

private val GridTopPadding = Dimens.tiny
private val GridBottomPadding = Dimens.large

/**
 * Places a message shown in a [PagedGrid]'s place, such as an empty list or an error, where its first card would
 * start: in the page column with the [gutter], below the same top padding.
 */
fun Modifier.pagedGridMessage(gutter: Dp): Modifier =
    pageColumn(gutter).padding(top = GridTopPadding, bottom = GridBottomPadding)

/** Cards in a [PagedGrid] are at least this wide; the grid fits as many columns as the window allows. */
data class PagedGridCardWidth(
    val compact: Dp,
    val large: Dp,
) {
    companion object {
        /** Game covers: three columns on phones; wider windows show larger covers, like the desktop Home grids. */
        val Games = PagedGridCardWidth(compact = 100.dp, large = 150.dp)

        /** Landscape giveaway cards: two columns on phones. */
        val Giveaways = PagedGridCardWidth(compact = 150.dp, large = 240.dp)
    }
}

/**
 * [items] as a grid of cards in order, for lists that load page by page. While [more] items are available,
 * [skeleton] placeholders complete the last row and add one more, and the next page is requested through [onLoadMore]
 * as the grid nears its end, so scrolling rarely waits for it. A failed page shows its error with [onRetry] below the
 * loaded cards, which stay in place. Cards need unique, stable keys from [itemKey]. [contentTopInset] leaves room
 * above the cards for a bar that scrolls away with them (see [ScrollAwayBarLayout]).
 */
@Suppress("LongParameterList")
@Composable
fun <T> PagedGrid(
    items: List<T>,
    more: MoreResults,
    itemKey: (T) -> Any,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    skeleton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberCachedLazyGridState(),
    cardWidth: PagedGridCardWidth = PagedGridCardWidth.Games,
    contentTopInset: Dp = 0.dp,
    itemContent: @Composable (T) -> Unit,
) {
    val loadMore by rememberUpdatedState(onLoadMore)
    val itemCount = items.size
    // Restarts only when cards are added or the paging state changes; scrolling is read outside composition.
    LaunchedEffect(state, itemCount, more) {
        if (more != MoreResults.Available) return@LaunchedEffect
        snapshotFlow {
            state.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
        }.map { lastVisible -> lastVisible >= itemCount - PREFETCH_DISTANCE }
            .distinctUntilChanged()
            .filter { nearEnd -> nearEnd }
            .collect { loadMore() }
    }
    CardGrid(
        modifier = modifier,
        cardWidth = cardWidth,
        state = state,
        topPadding = GridTopPadding + contentTopInset,
    ) { columns ->
        items(items, key = itemKey, contentType = { CARD_CONTENT_TYPE }) { item -> itemContent(item) }
        when (more) {
            MoreResults.Available, MoreResults.Loading -> {
                val placeholders = (columns - itemCount % columns) % columns + columns
                items(placeholders, key = { index -> "more-$index" }, contentType = { SKELETON_CONTENT_TYPE }) {
                    skeleton()
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

/** [skeleton] placeholders with the geometry of a [PagedGrid], shown until its first page arrives. */
@Composable
fun PagedGridSkeleton(
    skeleton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: PagedGridCardWidth = PagedGridCardWidth.Games,
    contentTopInset: Dp = 0.dp,
) {
    CardGrid(
        modifier = modifier,
        cardWidth = cardWidth,
        userScrollEnabled = false,
        topPadding = GridTopPadding + contentTopInset,
    ) { columns ->
        items(columns * LOADING_ROWS, contentType = { SKELETON_CONTENT_TYPE }) { skeleton() }
    }
}

/**
 * The grid every paged state shares, so placeholders and cards line up. It fits as many columns as the page column
 * allows and passes their count to [content]. The grid spans the whole width, so it also scrolls from the margins,
 * and insets its cards to the page column. While the keyboard is open, the last rows can still scroll clear of it.
 */
@Composable
@Suppress("LongParameterList")
internal fun CardGrid(
    modifier: Modifier,
    cardWidth: PagedGridCardWidth,
    state: LazyGridState = rememberLazyGridState(),
    userScrollEnabled: Boolean = true,
    topPadding: Dp = GridTopPadding,
    scrollbarTopInset: Dp = 0.dp,
    content: LazyGridScope.(columns: Int) -> Unit,
) {
    val minCardWidth = if (isLargeWindow()) cardWidth.large else cardWidth.compact
    val gutter = pageGutter()
    BoxWithConstraints(modifier = modifier.fillMaxSize().keyboardPadding()) {
        val columns = columnsFor(pageContentWidth(maxWidth, gutter), minCardWidth)
        val sidePadding = pageSidePadding(maxWidth, gutter)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(start = sidePadding, top = topPadding, end = sidePadding, bottom = GridBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(ColumnGap),
            verticalArrangement = Arrangement.spacedBy(RowGap),
            userScrollEnabled = userScrollEnabled,
        ) {
            content(columns)
        }
        if (userScrollEnabled) PageScrollbar(state, topInset = scrollbarTopInset)
    }
}

private fun columnsFor(
    width: Dp,
    minCardWidth: Dp,
): Int = ((width + ColumnGap) / (minCardWidth + ColumnGap)).toInt().coerceAtLeast(1)

private const val PREVIEW_GAME_COUNT = 7
private val PreviewHeight = 640.dp

@Preview
@Composable
internal fun PagedGridPreview() {
    BraindanceTheme {
        PagedGrid(
            items = List(PREVIEW_GAME_COUNT) { index -> GameCardModel(index + 1, "Sample game ${index + 1}", "") },
            more = MoreResults.Loading,
            itemKey = GameCardModel::id,
            onLoadMore = {},
            onRetry = {},
            skeleton = { GameCardSkeleton(showTitle = true) },
            modifier = Modifier.background(background).height(PreviewHeight),
        ) { game ->
            GameCard(card = game, onClick = {}, showTitle = true)
        }
    }
}
