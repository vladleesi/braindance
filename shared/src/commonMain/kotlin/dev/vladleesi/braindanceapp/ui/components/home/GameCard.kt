// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.trending_reason_full_release
import dev.vladleesi.braindanceapp.resources.trending_reason_new_platform
import dev.vladleesi.braindanceapp.resources.trending_reason_new_release
import dev.vladleesi.braindanceapp.resources.trending_reason_now_on_platform
import dev.vladleesi.braindanceapp.resources.trending_reason_showcased
import dev.vladleesi.braindanceapp.ui.components.COVER_ASPECT_RATIO
import dev.vladleesi.braindanceapp.ui.components.CardCover
import dev.vladleesi.braindanceapp.ui.components.CardCoverSkeleton
import dev.vladleesi.braindanceapp.ui.components.TextLineSkeleton
import dev.vladleesi.braindanceapp.ui.components.cardClickable
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import org.jetbrains.compose.resources.stringResource

private const val SKELETON_TITLE_FRACTION = 0.7f
private const val SKELETON_CAPTION_FRACTION = 0.4f

/**
 * A game card for every platform: the cover, and with [showTitle] the title and the trending reason below it. The
 * caller sets the width, such as a fixed width in a carousel or a weighted cell in a desktop row; the cover keeps the
 * 3:4 ratio inside it. The cover's image description names the game either way.
 */
@Composable
fun GameCard(
    card: GameCardModel,
    onClick: (id: Int) -> Unit,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier.cardClickable(interactionSource) { onClick(card.id) }) {
        CardCover(
            imageUrl = card.backgroundImageUrl,
            // Each style has its own key: a desktop row shows them at different sizes, and a shared key would make
            // the memory cache swap one decoded size for the other.
            imageCacheKey = if (showTitle) "gct-${card.id}" else "gcc-${card.id}",
            contentDescription = card.title,
            aspectRatio = COVER_ASPECT_RATIO,
            interactionSource = interactionSource,
        )
        if (showTitle) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Dimens.small),
            )
            // Always present, so cards with and without a reason keep the same height.
            Text(
                text = card.label?.text().orEmpty(),
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Dimens.micro),
            )
        }
    }
}

/** The loading placeholder for a [GameCard] with the same [showTitle] and width. */
@Composable
fun GameCardSkeleton(
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CardCoverSkeleton(aspectRatio = COVER_ASPECT_RATIO)
        if (showTitle) {
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
}

@Composable
internal fun GameCardLabel.text(): String =
    when (this) {
        GameCardLabel.NewRelease -> stringResource(Res.string.trending_reason_new_release)
        GameCardLabel.FullRelease -> stringResource(Res.string.trending_reason_full_release)
        GameCardLabel.Showcased -> stringResource(Res.string.trending_reason_showcased)
        is GameCardLabel.NewPlatform ->
            platform?.let { stringResource(Res.string.trending_reason_now_on_platform, it) }
                ?: stringResource(Res.string.trending_reason_new_platform)
    }

data class GameCardModel(
    val id: Int,
    val title: String,
    val backgroundImageUrl: String,
    val label: GameCardLabel? = null,
)

/** A short verified fact shown under a card's title, such as why a game is trending. */
sealed interface GameCardLabel {
    data object NewRelease : GameCardLabel

    data object FullRelease : GameCardLabel

    data object Showcased : GameCardLabel

    /** [platform] is a short platform name, or null when the backend did not provide one. */
    data class NewPlatform(
        val platform: String?,
    ) : GameCardLabel
}

private val PreviewGame =
    GameCardModel(id = 1, title = "Sample game", backgroundImageUrl = "", label = GameCardLabel.NewRelease)

@Preview
@Composable
internal fun GameCardPreview() {
    BraindanceTheme {
        Row(
            modifier = Modifier.background(background).padding(Dimens.medium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            GameCard(
                card = PreviewGame,
                onClick = {},
                showTitle = true,
                modifier = Modifier.width(Dimens.gameCardWidth),
            )
            GameCard(
                card = PreviewGame,
                onClick = {},
                showTitle = false,
                modifier = Modifier.width(Dimens.gameCardWidth),
            )
            GameCardSkeleton(showTitle = true, modifier = Modifier.width(Dimens.gameCardWidth))
        }
    }
}
