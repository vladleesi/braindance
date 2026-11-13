// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import Share
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_details_not_found
import dev.vladleesi.braindanceapp.resources.game_details_screen_similar_games
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.components.CarouselSection
import dev.vladleesi.braindanceapp.ui.components.ExpandableText
import dev.vladleesi.braindanceapp.ui.components.GlobalLoading
import dev.vladleesi.braindanceapp.ui.components.PlatformLogoList
import dev.vladleesi.braindanceapp.ui.components.SpacerTopBarWithStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.details.GenreTags
import dev.vladleesi.braindanceapp.ui.components.details.ReleaseDateLabel
import dev.vladleesi.braindanceapp.ui.components.details.storesBlockItem
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.rememberShareGameAction
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsState
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.RecommendationsState
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.utils.calculateScrollTopBarColors
import dev.vladleesi.braindanceapp.utils.toContentDescription
import dev.vladleesi.braindanceapp.utils.withCircleRippleEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
            item { SpacerTopBarWithStatusBarInsets() }
            if (isLargeWindow) {
                gameInfoHeaderLargeScreen(state)
            } else {
                gameInfoHeaderSmallScreen(state)
            }
            if (state.gameDetails.summary.isNotEmpty()) {
                item {
                    ExpandableText(
                        text = state.gameDetails.summary,
                        modifier = Modifier.fillMaxWidth().padding(start = Dimens.medium, end = Dimens.medium),
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
                        titleStyle = MaterialTheme.typography.h3,
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
 * The bar over the scrolling content: back, the game's name, and Share, which links to the game's page. The title fades
 * in as the bar fills, but Share is always visible: like the back button it is a white icon on a circular backdrop
 * that is a dark chip over the artwork and blends into the bar once it fills.
 */
@Composable
private fun GameDetailsTopBar(
    gameDetails: GameDetails,
    topBarColor: Color,
    backButtonColor: Color,
    titleColor: Color,
    onBackButtonPressed: () -> Unit,
) {
    val shareGame = rememberShareGameAction(gameId = gameDetails.id, gameName = gameDetails.name)
    Column {
        StatusBarOverlay(backgroundColor = topBarColor)
        TopAppBar(
            tabBarColor = topBarColor,
            titleColor = titleColor,
            showBackButton = true,
            backButtonBackgroundColor = backButtonColor,
            onBackButtonPressed = onBackButtonPressed,
            title = gameDetails.name,
            actions = {
                IconButton(
                    modifier = Modifier.withCircleRippleEffect(backButtonColor),
                    onClick = shareGame.share,
                ) {
                    Icon(
                        imageVector = Share,
                        contentDescription = stringResource(shareGame.label),
                        tint = white,
                    )
                }
            },
        )
    }
}

@Composable
private fun GameDetailsInfo(gameDetails: GameDetails) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Dimens.medium, end = Dimens.medium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        gameDetails.releaseDate?.let { releaseDate ->
            ReleaseDateLabel(releaseDate)
        }
        PlatformLogoList(
            platforms = gameDetails.platforms,
            imageSize = 18.dp,
            horizontalSpacing = Dimens.tiny,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun LazyListScope.gameInfoHeaderLargeScreen(state: GameDetailsState.Success) =
    item {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = Dimens.medium)
                    .padding(bottom = Dimens.medium),
        ) {
            AsyncImage(
                modifier =
                    Modifier
                        .width(Dimens.gameCardWidth)
                        .height(Dimens.gameCardHeight)
                        .clip(CornerShapes.small),
                model = state.gameDetails.coverImageUrl,
                contentScale = ContentScale.Crop,
                contentDescription = state.gameDetails.name.toContentDescription(),
            )
            Spacer(modifier = Modifier.size(Dimens.medium))
            Column {
                GameDetailsInfo(gameDetails = state.gameDetails)
                Spacer(modifier = Modifier.size(Dimens.small))
                Text(
                    text = state.gameDetails.name,
                    style = MaterialTheme.typography.h2,
                    modifier = Modifier.fillMaxWidth().padding(start = Dimens.medium, end = Dimens.medium),
                )
                GenreTags(state.gameDetails.genres, onClick = {})
            }
        }
    }

private fun LazyListScope.gameInfoHeaderSmallScreen(state: GameDetailsState.Success) {
    item {
        Box(modifier = Modifier.padding(horizontal = Dimens.medium)) {
            AsyncImage(
                modifier =
                    Modifier
                        .width(Dimens.gameCardWidth)
                        .height(Dimens.gameCardHeight)
                        .clip(CornerShapes.small),
                model = state.gameDetails.coverImageUrl,
                contentScale = ContentScale.Crop,
                contentDescription = state.gameDetails.name.toContentDescription(),
            )
        }
    }
    item { Spacer(modifier = Modifier.size(Dimens.medium)) }
    item { GameDetailsInfo(gameDetails = state.gameDetails) }
    item { Spacer(modifier = Modifier.size(Dimens.small)) }
    item {
        Text(
            text = state.gameDetails.name,
            style = MaterialTheme.typography.h2,
            modifier = Modifier.fillMaxWidth().padding(start = Dimens.medium, end = Dimens.medium),
        )
    }
    item { GenreTags(state.gameDetails.genres, onClick = {}) }
    item { Spacer(modifier = Modifier.size(Dimens.medium)) }
}
