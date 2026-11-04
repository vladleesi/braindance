// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.trending_reason_full_release
import dev.vladleesi.braindanceapp.resources.trending_reason_new_platform
import dev.vladleesi.braindanceapp.resources.trending_reason_new_release
import dev.vladleesi.braindanceapp.resources.trending_reason_now_on_platform
import dev.vladleesi.braindanceapp.resources.trending_reason_showcased
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith70Alpha
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.clickable
import dev.vladleesi.braindanceapp.utils.rememberCachedImagePainter
import dev.vladleesi.braindanceapp.utils.shimmerEffect
import dev.vladleesi.braindanceapp.utils.toContentDescription
import org.jetbrains.compose.resources.stringResource

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
            card.label?.let { label ->
                // Drawn over the fixed-size cover, so labeled and unlabeled cards keep the same layout.
                Text(
                    text = label.text(),
                    style = MaterialTheme.typography.caption,
                    color = white,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(Dimens.tiny)
                            .background(
                                color = overlayBlackWith70Alpha,
                                shape = RoundedCornerShape(Dimens.small),
                            ).padding(horizontal = Dimens.tiny, vertical = Dimens.micro),
                )
            }
        }
    }
}

@Composable
private fun MiniGameCardLabel.text(): String =
    when (this) {
        MiniGameCardLabel.NewRelease -> stringResource(Res.string.trending_reason_new_release)
        MiniGameCardLabel.FullRelease -> stringResource(Res.string.trending_reason_full_release)
        MiniGameCardLabel.Showcased -> stringResource(Res.string.trending_reason_showcased)
        is MiniGameCardLabel.NewPlatform ->
            platform?.let { stringResource(Res.string.trending_reason_now_on_platform, it) }
                ?: stringResource(Res.string.trending_reason_new_platform)
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
    val label: MiniGameCardLabel? = null,
)

/** A short verified fact shown on a card's cover, such as why a game is trending. */
sealed interface MiniGameCardLabel {
    data object NewRelease : MiniGameCardLabel

    data object FullRelease : MiniGameCardLabel

    data object Showcased : MiniGameCardLabel

    /** [platform] is a short platform name, or null when the backend did not provide one. */
    data class NewPlatform(
        val platform: String?,
    ) : MiniGameCardLabel
}
