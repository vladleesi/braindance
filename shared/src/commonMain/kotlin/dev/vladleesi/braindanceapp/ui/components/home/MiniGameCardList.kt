// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.system.isLargeDevice
import dev.vladleesi.braindanceapp.ui.components.HorizontalListControls
import dev.vladleesi.braindanceapp.ui.components.StaticText
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeState
import dev.vladleesi.braindanceapp.utils.shimmerEffect

private const val MINI_GAME_CARD_SKELETON_COUNT = 4

@Composable
fun MiniGameCardList(
    modifier: Modifier = Modifier,
    title: String,
    state: HomeState,
    onRefresh: () -> Unit,
    onItemClick: (Int) -> Unit,
) {
    Crossfade(targetState = state) {
        when (state) {
            HomeState.Loading ->
                MiniGameCardListSkeleton(
                    title = title,
                    modifier = modifier,
                )

            is HomeState.Success<*> ->
                MiniGameCardList(
                    title = title,
                    titleTextStyle = MaterialTheme.typography.h2,
                    games = state.entities.filterIsInstance<MiniGameCardModel>(),
                    modifier = modifier,
                    onItemClick = onItemClick,
                    onExpandClick = {},
                )

            is HomeState.Error ->
                CarouselErrorState(
                    title = title,
                    errorMessage = state.message,
                    onRefresh = onRefresh,
                    modifier = modifier,
                )
        }
    }
}

@Composable
fun MiniGameCardList(
    title: String,
    titleTextStyle: TextStyle,
    games: List<MiniGameCardModel>,
    modifier: Modifier = Modifier,
    onItemClick: (Int) -> Unit,
    onExpandClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxWidth()) {
            StaticText(
                text = title,
                modifier =
                    Modifier
                        .padding(horizontal = Dimens.medium)
                        .align(Alignment.CenterStart),
                style = titleTextStyle,
            )
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalListControls(listState = listState)
                onExpandClick?.let {
                    Button(
                        onClick = onExpandClick,
                        elevation = null,
                    ) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.subtitle2,
                        )
                    }
                }
            }
        }
        LazyRow(
            state = listState,
            modifier = Modifier.padding(top = Dimens.small),
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
            contentPadding = PaddingValues(horizontal = Dimens.medium),
        ) {
            items(games, key = { it.id }, contentType = { it }) { card ->
                MiniGameCard(card = card, onClick = onItemClick)
            }
        }
    }
}

@Composable
private fun MiniGameCardListSkeleton(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            StaticText(
                text = title,
                modifier =
                    Modifier
                        .padding(horizontal = Dimens.medium)
                        .align(Alignment.CenterStart),
                style = MaterialTheme.typography.h2,
            )
            Button(
                onClick = {},
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.subtitle2,
                    modifier =
                        Modifier
                            .width(50.dp)
                            .clip(RoundedCornerShape(Dimens.small))
                            .shimmerEffect(),
                )
            }
        }
        val skeletonCount = if (isLargeDevice()) MINI_GAME_CARD_SKELETON_COUNT * 2 else MINI_GAME_CARD_SKELETON_COUNT
        LazyRow(
            modifier = Modifier.padding(top = Dimens.small),
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = Dimens.medium),
        ) {
            items(skeletonCount) {
                MiniGameCardSkeleton()
            }
        }
    }
}
