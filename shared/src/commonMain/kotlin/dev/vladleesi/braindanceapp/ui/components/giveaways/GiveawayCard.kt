// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.giveaways

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_free
import dev.vladleesi.braindanceapp.ui.components.CardCover
import dev.vladleesi.braindanceapp.ui.components.CardCoverSkeleton
import dev.vladleesi.braindanceapp.ui.components.TextLineSkeleton
import dev.vladleesi.braindanceapp.ui.components.cardClickable
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith70Alpha
import dev.vladleesi.braindanceapp.ui.style.overlayWhiteWith80Alpha
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.toContentDescription
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// GamerPower images are 460 x 215.
private const val GIVEAWAY_IMAGE_ASPECT_RATIO = 460f / 215f
private const val SKELETON_TITLE_FRACTION = 0.7f
private const val SKELETON_CAPTION_FRACTION = 0.4f

/**
 * A giveaway card for every platform: the image with its end date, platforms, and type, then the title and the
 * original price. The caller sets the width; the image keeps the GamerPower aspect ratio inside it.
 */
@Composable
fun GiveawayCard(
    item: GiveawayItemModel,
    onClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier.cardClickable(interactionSource) { onClick(item.id) }) {
        CardCover(
            imageUrl = item.image,
            imageCacheKey = "gwc-${item.id}",
            contentDescription = item.title,
            aspectRatio = GIVEAWAY_IMAGE_ASPECT_RATIO,
            interactionSource = interactionSource,
        ) {
            if (item.endDate.isNotEmpty()) {
                Text(
                    text = item.endDate,
                    style = MaterialTheme.typography.caption,
                    color = white,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(Dimens.small)
                            .background(overlayBlackWith70Alpha, CornerShapes.small)
                            .padding(horizontal = Dimens.tiny, vertical = Dimens.micro),
                )
            }
            GiveawayPlatforms(platforms = item.platforms, modifier = Modifier.align(Alignment.TopEnd))
            if (item.type.isNotEmpty()) {
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.caption,
                    color = background,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Dimens.small)
                            .background(overlayWhiteWith80Alpha, CornerShapes.small)
                            .padding(horizontal = Dimens.tiny),
                )
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.subtitle1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Dimens.small),
        )
        Row(
            modifier = Modifier.padding(top = Dimens.micro),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.tiny),
        ) {
            Text(
                text = stringResource(Res.string.giveaway_details_screen_free),
                style = MaterialTheme.typography.caption,
                color = white,
            )
            Text(
                text = item.worth,
                style = MaterialTheme.typography.caption,
                color = secondaryText,
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
            )
        }
    }
}

/** The loading placeholder for a [GiveawayCard] of the same width. */
@Composable
fun GiveawayCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CardCoverSkeleton(aspectRatio = GIVEAWAY_IMAGE_ASPECT_RATIO)
        TextLineSkeleton(
            style = MaterialTheme.typography.subtitle1,
            widthFraction = SKELETON_TITLE_FRACTION,
            modifier = Modifier.padding(top = Dimens.small),
        )
        TextLineSkeleton(
            style = MaterialTheme.typography.caption,
            widthFraction = SKELETON_CAPTION_FRACTION,
            modifier = Modifier.padding(top = Dimens.micro),
        )
    }
}

@Composable
private fun GiveawayPlatforms(
    platforms: List<ParentPlatformType>,
    modifier: Modifier = Modifier,
) {
    if (platforms.none { platform -> platform.iconRes != null }) return
    Row(
        modifier =
            modifier
                .padding(Dimens.small)
                .background(overlayBlackWith70Alpha, CornerShapes.small)
                .padding(Dimens.tiny),
        horizontalArrangement = Arrangement.spacedBy(Dimens.micro),
    ) {
        platforms.forEach { platform ->
            val iconRes = platform.iconRes ?: return@forEach
            Image(
                painter = painterResource(iconRes),
                contentDescription = platform.name.toContentDescription(),
                colorFilter = ColorFilter.tint(white),
                modifier = Modifier.size(Dimens.compact),
            )
        }
    }
}

data class GiveawayItemModel(
    val id: Int,
    val image: String,
    val title: String,
    val worth: String,
    val endDate: String,
    val platforms: List<ParentPlatformType>,
    val type: String,
)

@Preview
@Composable
internal fun GiveawayCardPreview() {
    BraindanceTheme {
        Row(
            modifier = Modifier.background(background).padding(Dimens.medium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            GiveawayCard(
                item =
                    GiveawayItemModel(
                        id = 1,
                        image = "",
                        title = "Sample giveaway",
                        worth = "$19.99",
                        endDate = "3 days",
                        platforms = listOf(ParentPlatformType.PC),
                        type = "Game",
                    ),
                onClick = {},
                modifier = Modifier.width(Dimens.giveAwayCardWidth),
            )
            GiveawayCardSkeleton(modifier = Modifier.width(Dimens.giveAwayCardWidth))
        }
    }
}
