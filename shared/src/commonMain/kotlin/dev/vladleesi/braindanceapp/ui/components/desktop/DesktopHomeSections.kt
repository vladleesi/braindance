// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.error_screen_retry_button_text
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_free
import dev.vladleesi.braindanceapp.resources.home_section_empty
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayItemModel
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardLabel
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.text
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith70Alpha
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith80Alpha
import dev.vladleesi.braindanceapp.ui.style.overlayWhiteWith80Alpha
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest
import dev.vladleesi.braindanceapp.utils.shimmerEffect
import dev.vladleesi.braindanceapp.utils.toContentDescription
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** IGDB covers are portrait 3:4; the cards keep that ratio so covers are never stretched. */
const val COVER_ASPECT_RATIO = 3f / 4f

// GamerPower images are 460 x 215.
private const val GIVEAWAY_IMAGE_ASPECT_RATIO = 460f / 215f
private const val HOVER_IMAGE_SCALE = 1.04f
private const val HOVER_ANIMATION_MILLIS = 150
private const val ANTICIPATED_TITLE_LINES = 2
private const val ERROR_MESSAGE_LINES = 3
private const val SKELETON_TITLE_FRACTION = 0.7f
private const val SKELETON_CAPTION_FRACTION = 0.4f

private val CardGap = Dimens.medium
private val AccentWidth = 16.dp

// Fills only the lower part of the cover, where the title sits, so the artwork stays the focus.
private val CoverTitleScrim = Brush.verticalGradient(0f to Color.Transparent, 1f to overlayBlackWith80Alpha)

// Minimum card widths: the grids show as many cards as fit, between two and their maximum.
private val GameCardMinWidth = 150.dp
private val AnticipatedCardMinWidth = 120.dp
private val GiveawayCardMinWidth = 150.dp
private const val GAME_COLUMNS_MAX = 5
private const val ANTICIPATED_COLUMNS_MAX = 6
private const val GIVEAWAY_COLUMNS_MAX = 5
private const val COLUMNS_MIN = 2

/** How many cards of each section fit in one row of [width]. */
data class DesktopGridColumns(
    val games: Int,
    val anticipated: Int,
    val giveaways: Int,
) {
    companion object {
        fun forWidth(width: Dp): DesktopGridColumns =
            DesktopGridColumns(
                games = columnsFor(width, GameCardMinWidth, GAME_COLUMNS_MAX),
                anticipated = columnsFor(width, AnticipatedCardMinWidth, ANTICIPATED_COLUMNS_MAX),
                giveaways = columnsFor(width, GiveawayCardMinWidth, GIVEAWAY_COLUMNS_MAX),
            )

        private fun columnsFor(
            width: Dp,
            minCardWidth: Dp,
            maxColumns: Int,
        ): Int = ((width + CardGap) / (minCardWidth + CardGap)).toInt().coerceIn(COLUMNS_MIN, maxColumns)
    }
}

/** A titled Home section; [content] is usually a [DesktopCardRow]. */
@Composable
fun DesktopSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.compact)) {
        Text(
            text = title,
            style = MaterialTheme.typography.h3,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/**
 * A single, non-scrolling row of [columns] cards for one section [state]. Loading shows skeletons with the loaded
 * cards' geometry, an error offers [onRetry], and only the items that fit are composed.
 */
@Composable
fun <T> DesktopCardRow(
    state: SectionState<T>,
    columns: Int,
    onRetry: () -> Unit,
    skeleton: @Composable (Modifier) -> Unit,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    when (state) {
        SectionState.Loading ->
            Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
                repeat(columns) { skeleton(Modifier.weight(1f)) }
            }

        is SectionState.Success ->
            if (state.items.isEmpty()) {
                Text(text = stringResource(Res.string.home_section_empty), style = MaterialTheme.typography.caption)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
                    repeat(columns) { index ->
                        val item = state.items.getOrNull(index)
                        if (item == null) {
                            // Keeps the other cards at their full-row width when fewer items arrive.
                            Box(Modifier.weight(1f))
                        } else {
                            itemContent(item, Modifier.weight(1f))
                        }
                    }
                }
            }

        is SectionState.Error -> SectionError(message = state.error.userMessage(), onRetry = onRetry)
    }
}

@Composable
private fun SectionError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        Text(
            text = message,
            style = MaterialTheme.typography.caption,
            maxLines = ERROR_MESSAGE_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(backgroundColor = secondaryVariant),
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(text = stringResource(Res.string.error_screen_retry_button_text))
        }
    }
}

/** A Popular Right Now card: the cover, then the title and the trending reason when the backend provides one. */
@Composable
fun DesktopGameCard(
    card: MiniGameCardModel,
    onClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier.cardClickable(interactionSource) { onClick(card.id) }) {
        CardCover(
            imageUrl = card.backgroundImageUrl,
            imageCacheKey = "dgc-${card.id}",
            contentDescription = card.title,
            aspectRatio = COVER_ASPECT_RATIO,
            interactionSource = interactionSource,
        )
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

@Composable
fun DesktopGameCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(aspectRatio = COVER_ASPECT_RATIO)
        SkeletonLine(
            style = MaterialTheme.typography.subtitle1,
            widthFraction = SKELETON_TITLE_FRACTION,
            modifier = Modifier.padding(top = Dimens.small),
        )
        SkeletonLine(
            style = MaterialTheme.typography.caption,
            widthFraction = SKELETON_CAPTION_FRACTION,
            modifier = Modifier.padding(top = Dimens.micro),
        )
    }
}

/** A Most Anticipated card: the cover with its title over a dark scrim and a small magenta accent. */
@Composable
fun DesktopAnticipatedCard(
    card: MiniGameCardModel,
    onClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    CardCover(
        imageUrl = card.backgroundImageUrl,
        imageCacheKey = "dac-${card.id}",
        contentDescription = card.title,
        aspectRatio = COVER_ASPECT_RATIO,
        interactionSource = interactionSource,
        modifier = modifier.cardClickable(interactionSource) { onClick(card.id) },
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(CoverTitleScrim)
                    .padding(start = Dimens.compact, end = Dimens.compact, top = Dimens.large, bottom = Dimens.compact),
            verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = AccentWidth, height = Dimens.micro)
                        .background(hotMagenta, CornerShapes.tiny),
            )
            Text(
                text = card.title,
                style = MaterialTheme.typography.subtitle1,
                color = white,
                maxLines = ANTICIPATED_TITLE_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun DesktopAnticipatedCardSkeleton(modifier: Modifier = Modifier) {
    SkeletonBox(aspectRatio = COVER_ASPECT_RATIO, modifier = modifier)
}

/** A giveaway card: the image with its end date, platforms, and type, then the title and the original price. */
@Composable
fun DesktopGiveawayCard(
    item: GiveawayItemModel,
    onClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier.cardClickable(interactionSource) { onClick(item.id) }) {
        CardCover(
            imageUrl = item.image,
            imageCacheKey = "dga-${item.id}",
            contentDescription = item.title,
            aspectRatio = GIVEAWAY_IMAGE_ASPECT_RATIO,
            interactionSource = interactionSource,
        ) {
            if (item.endDate.isNotEmpty()) {
                CoverTag(text = item.endDate, modifier = Modifier.align(Alignment.TopStart))
            }
            GiveawayPlatforms(item = item, modifier = Modifier.align(Alignment.TopEnd))
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

@Composable
fun DesktopGiveawayCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(aspectRatio = GIVEAWAY_IMAGE_ASPECT_RATIO)
        SkeletonLine(
            style = MaterialTheme.typography.subtitle1,
            widthFraction = SKELETON_TITLE_FRACTION,
            modifier = Modifier.padding(top = Dimens.small),
        )
        SkeletonLine(
            style = MaterialTheme.typography.caption,
            widthFraction = SKELETON_CAPTION_FRACTION,
            modifier = Modifier.padding(top = Dimens.micro),
        )
    }
}

@Composable
private fun GiveawayPlatforms(
    item: GiveawayItemModel,
    modifier: Modifier = Modifier,
) {
    if (item.platforms.none { platform -> platform.iconRes != null }) return
    Row(
        modifier =
            modifier
                .padding(Dimens.small)
                .background(overlayBlackWith70Alpha, CornerShapes.small)
                .padding(Dimens.tiny),
        horizontalArrangement = Arrangement.spacedBy(Dimens.micro),
    ) {
        item.platforms.forEach { platform ->
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

@Composable
private fun CoverTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        color = white,
        maxLines = 1,
        modifier =
            modifier
                .padding(Dimens.small)
                .background(overlayBlackWith70Alpha, CornerShapes.small)
                .padding(horizontal = Dimens.tiny, vertical = Dimens.micro),
    )
}

/**
 * Card artwork at a fixed [aspectRatio], decoded at its displayed size. Hovering scales the image slightly inside its
 * rounded bounds, and keyboard focus outlines the card; [overlay] draws on top of the image.
 */
@Composable
private fun CardCover(
    imageUrl: String,
    imageCacheKey: String,
    contentDescription: String,
    aspectRatio: Float,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val imageScale by animateFloatAsState(
        targetValue = if (hovered) HOVER_IMAGE_SCALE else 1f,
        animationSpec = tween(HOVER_ANIMATION_MILLIS),
    )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(CornerShapes.small)
                .background(secondaryVariant)
                .border(Dimens.micro, if (focused) neonCyan else Color.Transparent, CornerShapes.small),
    ) {
        AsyncImage(
            model = rememberCachedImageRequest(data = imageUrl, imageCacheKey = imageCacheKey),
            contentDescription = contentDescription.toContentDescription(),
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                    },
        )
        overlay()
    }
}

/** A whole card is one click target, so its title opens it too; the cover shows the hover and focus feedback. */
private fun Modifier.cardClickable(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier =
    pointerHoverIcon(PointerIcon.Hand)
        .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)

@Composable
private fun SkeletonBox(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(CornerShapes.small)
                .shimmerEffect(),
    )
}

@Composable
private fun SkeletonLine(
    style: TextStyle,
    widthFraction: Float,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "",
        style = style,
        modifier =
            modifier
                .fillMaxWidth(widthFraction)
                .clip(CornerShapes.small)
                .shimmerEffect(),
    )
}

private val PreviewCardWidth = 180.dp
private val PreviewGiveawayWidth = 260.dp
private val PreviewSectionWidth = 840.dp

private val PreviewGame =
    MiniGameCardModel(id = 1, title = "Sample game", backgroundImageUrl = "", label = MiniGameCardLabel.NewRelease)

private val PreviewGiveaway =
    GiveawayItemModel(
        id = 1,
        image = "",
        title = "Sample giveaway",
        worth = "$19.99",
        endDate = "Ends in 3 days",
        platforms = listOf(ParentPlatformType.PC),
        type = "Game",
    )

@Preview
@Composable
internal fun DesktopSectionPreview() {
    BraindanceTheme {
        Column(
            modifier =
                Modifier
                    .background(background)
                    .padding(Dimens.large)
                    .width(PreviewSectionWidth),
            verticalArrangement = Arrangement.spacedBy(Dimens.large),
        ) {
            DesktopSection(title = "Popular Right Now") {
                DesktopCardRow(
                    state = SectionState.Success(listOf(PreviewGame, PreviewGame.copy(id = 2, label = null))),
                    columns = DesktopGridColumns.forWidth(PreviewSectionWidth).games,
                    onRetry = {},
                    skeleton = { itemModifier -> DesktopGameCardSkeleton(itemModifier) },
                ) { card, itemModifier ->
                    DesktopGameCard(card = card, onClick = {}, modifier = itemModifier)
                }
            }
            DesktopSection(title = "Most Anticipated") {
                DesktopCardRow(
                    state = SectionState.Loading,
                    columns = DesktopGridColumns.forWidth(PreviewSectionWidth).anticipated,
                    onRetry = {},
                    skeleton = { itemModifier -> DesktopAnticipatedCardSkeleton(itemModifier) },
                ) { card, itemModifier ->
                    DesktopAnticipatedCard(card = card, onClick = {}, modifier = itemModifier)
                }
            }
        }
    }
}

@Preview
@Composable
internal fun DesktopGameCardPreview() {
    BraindanceTheme {
        Row(
            modifier = Modifier.background(background).padding(Dimens.medium),
            horizontalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            DesktopGameCard(card = PreviewGame, onClick = {}, modifier = Modifier.width(PreviewCardWidth))
            DesktopGameCardSkeleton(modifier = Modifier.width(PreviewCardWidth))
        }
    }
}

@Preview
@Composable
internal fun DesktopAnticipatedCardPreview() {
    BraindanceTheme {
        Row(
            modifier = Modifier.background(background).padding(Dimens.medium),
            horizontalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            DesktopAnticipatedCard(card = PreviewGame, onClick = {}, modifier = Modifier.width(PreviewCardWidth))
            DesktopAnticipatedCardSkeleton(modifier = Modifier.width(PreviewCardWidth))
        }
    }
}

@Preview
@Composable
internal fun DesktopGiveawayCardPreview() {
    BraindanceTheme {
        Row(
            modifier = Modifier.background(background).padding(Dimens.medium),
            horizontalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            DesktopGiveawayCard(item = PreviewGiveaway, onClick = {}, modifier = Modifier.width(PreviewGiveawayWidth))
            DesktopGiveawayCardSkeleton(modifier = Modifier.width(PreviewGiveawayWidth))
        }
    }
}
