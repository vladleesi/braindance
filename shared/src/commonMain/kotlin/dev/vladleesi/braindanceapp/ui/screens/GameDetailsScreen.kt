// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_details_screen_similar_games
import dev.vladleesi.braindanceapp.system.isLargeDevice
import dev.vladleesi.braindanceapp.ui.components.ExpandableText
import dev.vladleesi.braindanceapp.ui.components.GlobalLoading
import dev.vladleesi.braindanceapp.ui.components.PlatformLogoList
import dev.vladleesi.braindanceapp.ui.components.SpacerTopBarWithStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.details.GenreTags
import dev.vladleesi.braindanceapp.ui.components.details.ReleaseDateLabel
import dev.vladleesi.braindanceapp.ui.components.details.storesBlockItem
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardList
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsState
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsViewModel
import dev.vladleesi.braindanceapp.utils.calculateScrollTopBarColors
import dev.vladleesi.braindanceapp.utils.toContentDescription
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GameDetailsScreen(
    id: Int,
    modifier: Modifier,
) {
    val viewModel: GameDetailsViewModel =
        koinViewModel(
            key = "game_details_$id",
            parameters = { parametersOf(id) },
        )

    val navigator = LocalNavigator.current

    val gameId by rememberSaveable {
        mutableStateOf(id)
    }
    val state by viewModel.gameDetailsState.collectAsState()

    LaunchedEffect(Unit) {
        gameId.let { viewModel.load() }
    }

    when (val currentState = state) {
        is GameDetailsState.Error ->
            ErrorScreen(errorMessage = currentState.message, modifier = modifier) {
                viewModel.refresh()
            }

        GameDetailsState.Loading ->
            GlobalLoading(modifier = modifier)

        is GameDetailsState.Success ->
            key(currentState.gameDetails.id) {
                GameDetailsScreen(
                    state = currentState,
                    modifier = modifier,
                    onBackButtonPressed = { navigator.goBack() },
                )
            }
    }
}

@Composable
private fun GameDetailsScreen(
    state: GameDetailsState.Success,
    modifier: Modifier,
    onBackButtonPressed: (() -> Unit),
) {
    val navigator = LocalNavigator.current

    val lazyListState = rememberLazyListState()
    val (topBarColor, backButtonColor, topBarTitleColor) =
        lazyListState.calculateScrollTopBarColors()
    val isLargeDevice = isLargeDevice()

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
            if (isLargeDevice) {
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
            val similarGames = state.gameDetails.similarGames
            if (similarGames.isNotEmpty()) {
                item { Spacer(modifier = Modifier.size(Dimens.large)) }
                item {
                    MiniGameCardList(
                        title = stringResource(Res.string.game_details_screen_similar_games),
                        titleTextStyle = MaterialTheme.typography.h3,
                        games = similarGames,
                        onItemClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.size(Dimens.large)) }
        }
        Column {
            StatusBarOverlay(backgroundColor = topBarColor)
            TopAppBar(
                tabBarColor = topBarColor,
                titleColor = topBarTitleColor,
                showBackButton = true,
                backButtonBackgroundColor = backButtonColor,
                onBackButtonPressed = onBackButtonPressed,
                title = state.gameDetails.name,
            )
        }
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
                        .width(Dimens.miniGameCardWidth)
                        .height(Dimens.miniGameCardHeight)
                        .clip(RoundedCornerShape(Dimens.small)),
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
                        .width(Dimens.miniGameCardWidth)
                        .height(Dimens.miniGameCardHeight)
                        .clip(RoundedCornerShape(Dimens.small)),
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
