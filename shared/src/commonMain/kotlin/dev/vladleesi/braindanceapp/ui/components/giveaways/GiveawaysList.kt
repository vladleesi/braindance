// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.giveaways

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
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.system.isLargeDevice
import dev.vladleesi.braindanceapp.ui.components.HorizontalListControls
import dev.vladleesi.braindanceapp.ui.components.StaticText
import dev.vladleesi.braindanceapp.ui.components.home.CarouselErrorState
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeState
import dev.vladleesi.braindanceapp.utils.shimmerEffect

private const val GIVEAWAYS_SKELETON_COUNT = 3
private const val GIVEAWAY_ITEM_CONTENT_TYPE = "giveaway_item"

@Composable
fun GiveawaysList(
    title: String,
    state: HomeState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: (Int) -> Unit,
) {
    Crossfade(targetState = state) { targetState ->
        when (targetState) {
            HomeState.Loading ->
                GiveawaysListSkeleton(
                    title = title,
                    modifier = modifier,
                )

            is HomeState.Success<*> ->
                GiveawaysList(
                    title = title,
                    giveaways = targetState.entities.filterIsInstance<GiveawayItemModel>(),
                    modifier = modifier,
                    onItemClick = onItemClick,
                )

            is HomeState.Error ->
                CarouselErrorState(
                    title = title,
                    errorMessage = targetState.message,
                    onRefresh = onRefresh,
                )
        }
    }
}

@Composable
private fun GiveawaysListSkeleton(
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
        val skeletonCount = if (isLargeDevice()) GIVEAWAYS_SKELETON_COUNT * 2 else GIVEAWAYS_SKELETON_COUNT
        LazyRow(
            modifier = Modifier.padding(top = Dimens.small),
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = Dimens.medium),
        ) {
            items(skeletonCount) {
                GiveawayItemSkeleton()
            }
        }
    }
}

@Composable
private fun GiveawaysList(
    title: String,
    giveaways: List<GiveawayItemModel>,
    modifier: Modifier,
    onItemClick: (Int) -> Unit,
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
                style = MaterialTheme.typography.h2,
            )
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalListControls(listState = listState)
                Button(
                    onClick = {},
                    elevation = null,
                ) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.subtitle2,
                    )
                }
            }
        }
        LazyRow(
            state = listState,
            modifier = Modifier.padding(top = Dimens.small),
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
            contentPadding = PaddingValues(horizontal = Dimens.medium),
        ) {
            items(
                items = giveaways,
                key = { model -> model.id },
                contentType = { GIVEAWAY_ITEM_CONTENT_TYPE },
            ) { item ->
                GiveawayItem(
                    item = item,
                    onClick = onItemClick,
                )
            }
        }
    }
}
