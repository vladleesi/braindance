// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeStateEntity
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.clickable
import dev.vladleesi.braindanceapp.utils.rememberCachedImagePainter
import dev.vladleesi.braindanceapp.utils.shimmerEffect
import dev.vladleesi.braindanceapp.utils.toContentDescription

@Composable
fun MiniGameCard(
    card: MiniGameCardModel,
    modifier: Modifier = Modifier,
    onClick: (id: Int) -> Unit,
) {
    val imagePainter =
        rememberCachedImagePainter(
            data = card.backgroundImageUrl,
            imageCacheKey = "mgc-${card.id}",
        )
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .height(Dimens.miniGameCardHeight)
                    .width(Dimens.miniGameCardWidth)
                    .clip(RoundedCornerShape(Dimens.small))
                    .clickable { onClick(card.id) },
        ) {
            Image(
                painter = imagePainter,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                contentDescription = card.title.toContentDescription(),
            )
        }
    }
}

@Composable
fun MiniGameCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .height(Dimens.miniGameCardHeight)
                    .width(Dimens.miniGameCardWidth)
                    .clip(RoundedCornerShape(Dimens.small))
                    .shimmerEffect(),
        )
    }
}

data class MiniGameCardModel(
    val id: Int,
    val title: String,
    val backgroundImageUrl: String,
    val platforms: List<ParentPlatformType>,
) : HomeStateEntity
