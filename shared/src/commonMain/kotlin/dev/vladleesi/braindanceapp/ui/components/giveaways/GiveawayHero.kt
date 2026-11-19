// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.giveaways

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.navigation.externalLink
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.giveaway_details_ended
import dev.vladleesi.braindanceapp.resources.giveaway_details_ended_action
import dev.vladleesi.braindanceapp.resources.giveaway_details_free
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_get_giveaway
import dev.vladleesi.braindanceapp.resources.giveaway_details_worth
import dev.vladleesi.braindanceapp.ui.components.PlatformLogoList
import dev.vladleesi.braindanceapp.ui.components.PrimaryButton
import dev.vladleesi.braindanceapp.ui.components.SpacerTopBarWithStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.Tag
import dev.vladleesi.braindanceapp.ui.components.details.HeroBackdrop
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.icons.OpenInNew
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayEnding
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest
import dev.vladleesi.braindanceapp.utils.toContentDescription
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

// Beside the text in wide layouts, the image is about as wide as GamerPower made it.
private val ExpandedImageWidth = 400.dp
private val LogoSize = 18.dp

// The hero calls out the end only when it is close enough to act on; the details list always shows it.
private const val URGENT_DAYS = 7

/**
 * The top of the giveaway screen over its own artwork: the image, then what is given away and whether it ends soon,
 * the title, the price and platforms, and the action that opens the giveaway. [expanded] puts the text beside the
 * image, for tablets and wide windows; on phones it stacks under the image and the action spans the width.
 *
 * [onGetGiveaway] is null when there is no safe link. An ended giveaway shows the action disabled, so nobody follows
 * a link that no longer gives anything away.
 */
@Composable
fun GiveawayHero(
    id: Int,
    details: GiveawayDetails,
    expanded: Boolean,
    onGetGiveaway: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        HeroBackdrop(imageUrl = details.imageUrl.ifEmpty { null }, modifier = Modifier.matchParentSize())
        Column(modifier = Modifier.pageColumn(pageGutter()).fillMaxWidth()) {
            SpacerTopBarWithStatusBarInsets()
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GiveawayImage(id = id, details = details, modifier = Modifier.width(ExpandedImageWidth))
                    Spacer(Modifier.width(Dimens.large))
                    HeroText(
                        details = details,
                        expanded = true,
                        onGetGiveaway = onGetGiveaway,
                        modifier = Modifier.weight(1f, fill = false).widthIn(max = Dimens.readableTextWidth),
                    )
                }
            } else {
                GiveawayImage(id = id, details = details, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Dimens.medium))
                HeroText(details = details, expanded = false, onGetGiveaway = onGetGiveaway)
            }
            Spacer(Modifier.height(Dimens.large))
        }
    }
}

/** The image at GamerPower's ratio; its size is set before it loads, so nothing moves when it arrives. */
@Composable
private fun GiveawayImage(
    id: Int,
    details: GiveawayDetails,
    modifier: Modifier,
) {
    AsyncImage(
        // Its own memory key: the card shows the same image at another size.
        model = rememberCachedImageRequest(data = details.imageUrl, memoryCacheKey = "gdi-$id"),
        contentDescription = details.title.toContentDescription(),
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .aspectRatio(GIVEAWAY_IMAGE_ASPECT_RATIO)
                .clip(CornerShapes.small)
                .background(secondaryVariant),
    )
}

@Composable
private fun HeroText(
    details: GiveawayDetails,
    expanded: Boolean,
    onGetGiveaway: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        HeroTags(type = details.type, ending = details.ending)
        Text(
            text = details.title,
            style = if (expanded) MaterialTheme.typography.h1 else MaterialTheme.typography.h2,
            color = white,
            modifier = Modifier.semantics { heading() },
        )
        PriceAndPlatforms(worth = details.worth, platforms = details.platforms)
        val ended = details.ending is GiveawayEnding.Ended
        if (ended || onGetGiveaway != null) {
            val action =
                if (ended) Res.string.giveaway_details_ended_action else Res.string.giveaway_details_screen_get_giveaway
            PrimaryButton(
                text = stringResource(action),
                onClick = { onGetGiveaway?.invoke() },
                enabled = !ended,
                trailingIcon = if (ended) null else OpenInNew,
                modifier =
                    Modifier
                        .padding(top = Dimens.compact)
                        .then(if (expanded) Modifier else Modifier.fillMaxWidth())
                        .then(onGetGiveaway?.takeUnless { ended }?.let { Modifier.externalLink(it) } ?: Modifier),
            )
        }
    }
}

/** What is given away and, when it ends within a week or is over, when it ends. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroTags(
    type: String,
    ending: GiveawayEnding,
) {
    val endingTag =
        when (ending) {
            is GiveawayEnding.Ended -> stringResource(Res.string.giveaway_details_ended)
            is GiveawayEnding.EndsOn -> if (ending.daysLeft <= URGENT_DAYS) endingSummary(ending.daysLeft) else null
            GiveawayEnding.OpenEnded -> null
        }
    if (type.isEmpty() && endingTag == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        if (type.isNotEmpty()) Tag(text = type)
        endingTag?.let { text -> Tag(text = text) }
    }
}

/** "Free", the struck-through original price, and the platform logos, quieter than the title. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriceAndPlatforms(
    worth: String,
    platforms: List<ParentPlatformType>,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.compact),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.giveaway_details_free),
            style = MaterialTheme.typography.h3,
            color = white,
        )
        if (worth.isNotEmpty()) {
            val worthLabel = stringResource(Res.string.giveaway_details_worth, worth)
            Text(
                text = worth,
                style = MaterialTheme.typography.subtitle1,
                color = secondaryText,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.semantics { contentDescription = worthLabel },
            )
        }
        if (platforms.any { platform -> platform.iconRes != null }) {
            PlatformLogoList(platforms = platforms, imageSize = LogoSize, horizontalSpacing = Dimens.small)
        }
    }
}

private val PreviewDetails =
    GiveawayDetails(
        title = "A Sample Game (Epic Games) Giveaway",
        imageUrl = "",
        worth = "$24.99",
        type = "Game",
        claimedCount = 12_500,
        platforms = listOf(ParentPlatformType.PC),
        platformNames = listOf("PC", "Epic Games Store"),
        description = "Grab a free copy of A Sample Game and keep it forever.",
        claimSteps = listOf("Click the button to visit the giveaway page.", "Log in to your account."),
        ending = GiveawayEnding.EndsOn(date = LocalDate.parse("2026-11-20"), daysLeft = 2),
        addedOn = LocalDate.parse("2026-11-12"),
        openUrl = "https://example.com",
        sourceUrl = "https://example.com",
    )

@Preview
@Composable
internal fun GiveawayHeroCompactPreview() {
    BraindanceTheme {
        GiveawayHero(
            id = 1,
            details = PreviewDetails,
            expanded = false,
            onGetGiveaway = {},
            modifier = Modifier.background(background),
        )
    }
}

@Preview(widthDp = 1024)
@Composable
internal fun GiveawayHeroExpandedPreview() {
    BraindanceTheme {
        GiveawayHero(
            id = 1,
            details = PreviewDetails.copy(ending = GiveawayEnding.Ended(date = null)),
            expanded = true,
            onGetGiveaway = {},
            modifier = Modifier.background(background),
        )
    }
}
