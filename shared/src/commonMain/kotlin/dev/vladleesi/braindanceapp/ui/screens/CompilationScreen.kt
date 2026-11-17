// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.ShareTarget
import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.home_screen_popular_latest_giveaways
import dev.vladleesi.braindanceapp.resources.home_screen_popular_most_anticipated
import dev.vladleesi.braindanceapp.resources.home_screen_popular_right_now
import dev.vladleesi.braindanceapp.ui.components.PagedGrid
import dev.vladleesi.braindanceapp.ui.components.PagedGridCardWidth
import dev.vladleesi.braindanceapp.ui.components.PagedGridSkeleton
import dev.vladleesi.braindanceapp.ui.components.SectionEmpty
import dev.vladleesi.braindanceapp.ui.components.SectionError
import dev.vladleesi.braindanceapp.ui.components.ShareButton
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayCard
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardLabel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pagedGridMessage
import dev.vladleesi.braindanceapp.ui.components.showsShareActions
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.AnticipatedGamesViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawaysListViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.MoreResults
import dev.vladleesi.braindanceapp.ui.viewmodels.PagedListState
import dev.vladleesi.braindanceapp.ui.viewmodels.PagedListViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.PopularGamesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Titles tell games apart, and a narrow card cuts one line after a few words, so the lists show two. */
private const val TITLE_LINES = 2

/** The full list behind a Home section's See All action, loading further pages as it scrolls. */
@Composable
fun CompilationScreen(
    compilation: Compilation,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val onBack = { navigator.goBack() }
    val onGameClick = { id: Int -> navigator.navigate(GameDetailsRoute(id)) }
    when (compilation) {
        Compilation.Popular ->
            GameCompilation(
                compilation = compilation,
                title = stringResource(Res.string.home_screen_popular_right_now),
                viewModel = koinViewModel<PopularGamesViewModel>(),
                onBack = onBack,
                onGameClick = onGameClick,
                modifier = modifier,
            )
        Compilation.Anticipated ->
            GameCompilation(
                compilation = compilation,
                title = stringResource(Res.string.home_screen_popular_most_anticipated),
                viewModel = koinViewModel<AnticipatedGamesViewModel>(),
                onBack = onBack,
                onGameClick = onGameClick,
                modifier = modifier,
            )
        Compilation.Giveaways -> {
            val viewModel = koinViewModel<GiveawaysListViewModel>()
            LaunchedEffect(viewModel) { viewModel.load() }
            val state by viewModel.state.collectAsStateWithLifecycle()
            val title = stringResource(Res.string.home_screen_popular_latest_giveaways)
            CompilationContent(
                title = title,
                state = state,
                onBack = onBack,
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::retry,
                itemKey = { giveaway -> giveaway.id },
                cardWidth = PagedGridCardWidth.Giveaways,
                skeleton = { GiveawayCardSkeleton() },
                modifier = modifier,
                shareTarget = ShareTarget.CompilationList(compilation, name = title),
            ) { giveaway ->
                GiveawayCard(item = giveaway, onClick = { id -> navigator.navigate(GiveawayDetailsRoute(id)) })
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
internal fun GameCompilation(
    compilation: Compilation,
    title: String,
    viewModel: PagedListViewModel<GameCardModel>,
    onBack: () -> Unit,
    onGameClick: (id: Int) -> Unit,
    modifier: Modifier,
) {
    // Loads only on first navigation; returning from a game keeps the loaded pages and the scroll position.
    LaunchedEffect(viewModel) { viewModel.load() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CompilationContent(
        title = title,
        state = state,
        onBack = onBack,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        itemKey = GameCardModel::id,
        cardWidth = PagedGridCardWidth.Games,
        skeleton = { GameCardSkeleton(showTitle = true, titleLines = TITLE_LINES) },
        modifier = modifier,
        shareTarget = ShareTarget.CompilationList(compilation, name = title),
    ) { game ->
        GameCard(card = game, onClick = onGameClick, showTitle = true, titleLines = TITLE_LINES)
    }
}

/**
 * A titled list screen with a back button for any [PagedListState]: placeholders while the first page loads, then a
 * [PagedGrid] of [itemContent] cards, or a message when the list is empty or failed. The grid's scroll position is
 * kept across state changes and restored when the screen returns. With [shareTarget], the top bar shares a link to the
 * list in layouts that [showsShareActions].
 */
@Suppress("LongParameterList")
@Composable
internal fun <T> CompilationContent(
    title: String,
    state: PagedListState<T>,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    itemKey: (T) -> Any,
    cardWidth: PagedGridCardWidth,
    skeleton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shareTarget: ShareTarget? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val gridState = rememberLazyGridState()
    Column(modifier = modifier.fillMaxSize()) {
        SpacerStatusBarInsets()
        TopAppBar(
            title = title,
            titleStyle = MaterialTheme.typography.h2,
            showBackButton = true,
            onBackButtonPressed = onBack,
            actions =
                if (shareTarget != null && showsShareActions()) {
                    { ShareButton(target = shareTarget, visible = true) }
                } else {
                    null
                },
        )
        when (state) {
            PagedListState.Loading -> PagedGridSkeleton(skeleton = skeleton, cardWidth = cardWidth)
            is PagedListState.Loaded ->
                PagedGrid(
                    items = state.items,
                    more = state.more,
                    itemKey = itemKey,
                    onLoadMore = onLoadMore,
                    onRetry = onRetry,
                    skeleton = skeleton,
                    state = gridState,
                    cardWidth = cardWidth,
                    itemContent = itemContent,
                )
            PagedListState.Empty -> SectionEmpty(modifier = Modifier.pagedGridMessage(pageGutter()))
            is PagedListState.Error ->
                SectionError(
                    message = state.error.userMessage(),
                    onRetry = onRetry,
                    modifier = Modifier.pagedGridMessage(pageGutter()),
                )
        }
    }
}

private val PreviewHeight = 640.dp

@Preview
@Composable
internal fun CompilationContentPreview() {
    BraindanceTheme {
        CompilationContent(
            title = "Popular Right Now",
            state =
                PagedListState.Loaded(
                    items =
                        listOf(
                            GameCardModel(1, "Sample game", "", GameCardLabel.NewRelease),
                            GameCardModel(2, "Another game", "", GameCardLabel.Showcased),
                        ),
                    more = MoreResults.Available,
                ),
            onBack = {},
            onLoadMore = {},
            onRetry = {},
            itemKey = GameCardModel::id,
            cardWidth = PagedGridCardWidth.Games,
            skeleton = { GameCardSkeleton(showTitle = true, titleLines = TITLE_LINES) },
            modifier = Modifier.background(background).height(PreviewHeight),
            shareTarget = ShareTarget.CompilationList(Compilation.Popular, name = "Popular Right Now"),
        ) { game ->
            GameCard(card = game, onClick = {}, showTitle = true, titleLines = TITLE_LINES)
        }
    }
}

@Preview
@Composable
internal fun CompilationErrorPreview() {
    BraindanceTheme {
        CompilationContent<GameCardModel>(
            title = "Most Anticipated",
            state = PagedListState.Error(DataError.Network),
            onBack = {},
            onLoadMore = {},
            onRetry = {},
            itemKey = GameCardModel::id,
            cardWidth = PagedGridCardWidth.Games,
            skeleton = {},
            modifier = Modifier.background(background).height(PreviewHeight),
        ) {}
    }
}
