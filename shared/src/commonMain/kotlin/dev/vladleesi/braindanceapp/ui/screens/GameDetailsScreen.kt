// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.ShareTarget
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_details_not_found
import dev.vladleesi.braindanceapp.resources.game_details_screen_similar_games
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.components.CarouselSection
import dev.vladleesi.braindanceapp.ui.components.ExpandableText
import dev.vladleesi.braindanceapp.ui.components.GlobalLoading
import dev.vladleesi.braindanceapp.ui.components.ShareButton
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.details.GameDetailsHero
import dev.vladleesi.braindanceapp.ui.components.details.LibraryActionsState
import dev.vladleesi.braindanceapp.ui.components.details.storesBlockItem
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageWidth
import dev.vladleesi.braindanceapp.ui.components.showsShareActions
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsState
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.RecommendationsState
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.utils.calculateScrollTopBarColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val HERO_KEY = "hero"
private const val SIMILAR_GAMES_KEY = "similar_games"
private const val SIMILAR_GAME_CONTENT_TYPE = "similar_game"

@Composable
fun GameDetailsScreen(
    id: Int,
    modifier: Modifier,
) {
    val viewModel: GameDetailsViewModel =
        // Scoped to this navigation entry, which clears it when the screen is popped.
        koinViewModel(parameters = { parametersOf(id) })

    val navigator = LocalNavigator.current

    val state by viewModel.gameDetailsState.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()

    // The ViewModel loads once per entry; later calls do nothing until a refresh.
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    when (val currentState = state) {
        is GameDetailsState.Error ->
            ErrorScreen(errorMessage = currentState.error.userMessage(), modifier = modifier) {
                viewModel.refresh()
            }

        GameDetailsState.NotFound ->
            ErrorScreen(errorMessage = stringResource(Res.string.game_details_not_found), modifier = modifier) {
                viewModel.refresh()
            }

        GameDetailsState.Loading ->
            GlobalLoading(modifier = modifier)

        is GameDetailsState.Success ->
            key(currentState.gameDetails.id) {
                GameDetailsScreen(
                    state = currentState,
                    recommendations = recommendations,
                    modifier = modifier,
                    onBackButtonPressed = { navigator.goBack() },
                )
            }
    }
}

@Composable
private fun GameDetailsScreen(
    state: GameDetailsState.Success,
    recommendations: RecommendationsState,
    modifier: Modifier,
    onBackButtonPressed: (() -> Unit),
) {
    val navigator = LocalNavigator.current

    val lazyListState = rememberLazyListState()
    val (topBarColor, backButtonColor, topBarTitleColor) =
        lazyListState.calculateScrollTopBarColors()
    val isLargeWindow = isLargeWindow()
    val gutter = pageGutter()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(background),
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = HERO_KEY) {
                GameDetailsHero(
                    details = state.gameDetails,
                    expanded = isLargeWindow,
                    // Accounts and collections do not exist yet, so the actions stay disabled and never call back.
                    // The library will provide this state and handle onLibraryStatusChange.
                    libraryActions = LibraryActionsState.Unavailable,
                    onLibraryStatusChange = {},
                    // Genres become chips once Discover can browse by genre; until then they are plain tags.
                    onGenreClick = null,
                )
            }
            if (state.gameDetails.summary.isNotEmpty()) {
                item {
                    ExpandableText(
                        text = state.gameDetails.summary,
                        modifier =
                            Modifier
                                .pageColumn(gutter)
                                .widthIn(max = Dimens.readableTextWidth)
                                .fillMaxWidth(),
                    )
                }
                item { Spacer(modifier = Modifier.size(Dimens.large)) }
            }
            val stores = state.gameDetails.stores
            if (stores.isNotEmpty()) {
                storesBlockItem(stores)
            }
            val showRecommendations =
                recommendations is RecommendationsState.Loading ||
                    (recommendations is RecommendationsState.Success && recommendations.games.isNotEmpty())
            if (showRecommendations) {
                item { Spacer(modifier = Modifier.size(Dimens.large)) }
                item(key = SIMILAR_GAMES_KEY) {
                    val title = stringResource(Res.string.game_details_screen_similar_games)
                    // Only loading and loaded recommendations reach this item; a failure hides the section instead.
                    val sectionState =
                        remember(recommendations) {
                            when (recommendations) {
                                is RecommendationsState.Success -> SectionState.Success(recommendations.games)
                                else -> SectionState.Loading
                            }
                        }
                    val cardModifier = Modifier.width(Dimens.gameCardWidth)
                    CarouselSection(
                        title = title,
                        state = sectionState,
                        // Never shown: the section has no error state.
                        onRetry = {},
                        skeleton = { GameCardSkeleton(showTitle = true, modifier = cardModifier) },
                        modifier = Modifier.pageWidth(gutter),
                        titleStyle = MaterialTheme.typography.h3,
                        horizontalPadding = gutter,
                    ) { games ->
                        items(games, key = { game -> game.id }, contentType = { SIMILAR_GAME_CONTENT_TYPE }) { game ->
                            GameCard(
                                card = game,
                                onClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                                showTitle = true,
                                modifier = cardModifier,
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.size(Dimens.large)) }
        }
        GameDetailsTopBar(
            gameDetails = state.gameDetails,
            topBarColor = topBarColor,
            backButtonColor = backButtonColor,
            titleColor = topBarTitleColor,
            onBackButtonPressed = onBackButtonPressed,
        )
    }
}

/**
 * The bar over the scrolling content: back, the game's name, and, in layouts that [showsShareActions], Share, which
 * links to the game's page. The title fades in as the bar fills, but Share is always visible: like the back button it
 * is a white icon on a circular backdrop that is a dark chip over the artwork and blends into the bar once it fills.
 */
@Composable
private fun GameDetailsTopBar(
    gameDetails: GameDetails,
    topBarColor: Color,
    backButtonColor: Color,
    titleColor: Color,
    onBackButtonPressed: () -> Unit,
) {
    val shareTarget = ShareTarget.Game(id = gameDetails.id, name = gameDetails.name)
    Column {
        StatusBarOverlay(backgroundColor = topBarColor)
        TopAppBar(
            tabBarColor = topBarColor,
            titleColor = titleColor,
            showBackButton = true,
            backButtonBackgroundColor = backButtonColor,
            onBackButtonPressed = onBackButtonPressed,
            title = gameDetails.name,
            actions =
                if (showsShareActions()) {
                    { ShareButton(target = shareTarget, backgroundColor = backButtonColor, visible = true) }
                } else {
                    null
                },
        )
    }
}
