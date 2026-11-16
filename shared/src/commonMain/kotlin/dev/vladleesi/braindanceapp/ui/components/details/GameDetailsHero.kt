// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.ui.components.COVER_ASPECT_RATIO
import dev.vladleesi.braindanceapp.ui.components.PlatformLogoList
import dev.vladleesi.braindanceapp.ui.components.SpacerTopBarWithStatusBarInsets
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.viewmodels.BackdropImage
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GenreTag
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest
import dev.vladleesi.braindanceapp.utils.toContentDescription

/** How the hero arranges itself in the space it has; the cover stays on the left in both. */
private class HeroMetrics(
    val coverWidth: Dp,
    val titleStyle: TextStyle,
    val titleMinFontSize: TextUnit,
    val titleMaxLines: Int,
    val logoSize: Dp,
)

// Phones: a cover small enough to leave the title a comfortable column, and a title that steps down to fit 4 lines.
private val CompactMetrics
    @Composable get() =
        HeroMetrics(
            coverWidth = 120.dp,
            titleStyle = MaterialTheme.typography.h2,
            titleMinFontSize = 18.sp,
            titleMaxLines = 4,
            logoSize = 18.dp,
        )

// Tablets and wide windows: a poster-sized cover with everything, actions included, in the column beside it.
private val ExpandedMetrics
    @Composable get() =
        HeroMetrics(
            coverWidth = 200.dp,
            titleStyle = MaterialTheme.typography.h1,
            titleMinFontSize = 24.sp,
            titleMaxLines = 3,
            logoSize = 20.dp,
        )

/**
 * The top of the game details screen: the cover on the left, then the title, the release date and platforms, the
 * library actions, and the genres, over the game's first screenshot. It starts under the top bar and status bar, so
 * the artwork reaches the top edge.
 *
 * [expanded] is for tablets and wide windows: the cover grows to poster size and the actions and genres join the
 * column beside it. On phones the genres and then the actions span the width under the cover, where the actions are
 * easy to reach. The hero has no state of its own; [libraryActions] and the callbacks come from the screen.
 */
@Composable
fun GameDetailsHero(
    details: GameDetails,
    expanded: Boolean,
    libraryActions: LibraryActionsState,
    onLibraryStatusChange: (LibraryStatus) -> Unit,
    onGenreClick: ((GenreTag) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val metrics = if (expanded) ExpandedMetrics else CompactMetrics
    Box(modifier = modifier.fillMaxWidth()) {
        HeroBackdrop(
            imageUrl = details.backdrop?.let { if (expanded) it.wideUrl else it.compactUrl },
            modifier = Modifier.matchParentSize(),
        )
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.medium)) {
            SpacerTopBarWithStatusBarInsets()
            Row(verticalAlignment = Alignment.Bottom) {
                HeroCover(details = details, modifier = Modifier.width(metrics.coverWidth))
                Spacer(Modifier.width(if (expanded) Dimens.large else Dimens.medium))
                Column(
                    modifier = Modifier.weight(1f, fill = false).widthIn(max = Dimens.readableTextWidth),
                    verticalArrangement = Arrangement.spacedBy(Dimens.small),
                ) {
                    HeroTitle(name = details.name, metrics = metrics)
                    HeroFacts(releaseDate = details.releaseDate, platforms = details.platforms, metrics = metrics)
                    if (expanded) {
                        if (details.genres.isNotEmpty()) {
                            GenreTags(
                                genres = details.genres,
                                onGenreClick = onGenreClick,
                                modifier = Modifier.padding(top = Dimens.tiny),
                            )
                        }
                        LibraryActions(
                            state = libraryActions,
                            fillWidth = false,
                            onStatusChange = onLibraryStatusChange,
                            modifier = Modifier.padding(top = Dimens.compact),
                        )
                    }
                }
            }
            if (!expanded) {
                // Genres first, so a "Coming soon" tag under the actions reads as theirs and not as another genre.
                if (details.genres.isNotEmpty()) {
                    GenreTags(
                        genres = details.genres,
                        onGenreClick = onGenreClick,
                        modifier = Modifier.padding(top = Dimens.medium),
                    )
                }
                LibraryActions(
                    state = libraryActions,
                    fillWidth = true,
                    onStatusChange = onLibraryStatusChange,
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.medium),
                )
            }
            Spacer(Modifier.height(Dimens.large))
        }
    }
}

/**
 * The cover at the hero's width and the covers' 3:4 ratio. Its size is fixed before the image loads, so nothing
 * moves when it arrives, and the raised neutral shows until then or when the game has no cover.
 */
@Composable
private fun HeroCover(
    details: GameDetails,
    modifier: Modifier,
) {
    AsyncImage(
        // Its own memory key: the hero shows the cover at another size than the cards, which would otherwise swap the
        // decoded size in the memory cache back and forth. The download itself is shared with the card's.
        model = rememberCachedImageRequest(data = details.coverImageUrl, memoryCacheKey = "gdc-${details.id}"),
        contentDescription = details.name.toContentDescription(),
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .aspectRatio(COVER_ASPECT_RATIO)
                .clip(CornerShapes.small)
                .background(secondaryVariant),
    )
}

/**
 * The game's name as the screen's heading. It starts at the layout's heading size and steps down to fit in
 * [HeroMetrics.titleMaxLines], so a long name still reads in full; only names too long even at the smallest size are
 * cut short. The line height scales with the size.
 */
@Composable
private fun HeroTitle(
    name: String,
    metrics: HeroMetrics,
) {
    val style =
        remember(metrics.titleStyle) {
            val titleStyle = metrics.titleStyle
            titleStyle.copy(lineHeight = (titleStyle.lineHeight.value / titleStyle.fontSize.value).em)
        }
    BasicText(
        text = name,
        style = style,
        maxLines = metrics.titleMaxLines,
        overflow = TextOverflow.Ellipsis,
        autoSize =
            remember(metrics.titleMinFontSize, metrics.titleStyle.fontSize) {
                FitMaxLines(minFontSize = metrics.titleMinFontSize, maxFontSize = metrics.titleStyle.fontSize)
            },
        modifier = Modifier.semantics { heading() },
    )
}

/**
 * Steps the font size down from [maxFontSize] by 1 sp until the text fits its maximum lines, ending at [minFontSize].
 * [TextAutoSize.StepBased] detects ellipsized text through `isLineEllipsized`, which the Skia text layout of iOS and
 * Web does not report, so it never shrinks there; exceeding the maximum lines sets `didOverflowHeight` everywhere.
 */
private class FitMaxLines(
    private val minFontSize: TextUnit,
    private val maxFontSize: TextUnit,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit {
        var size = maxFontSize.value
        while (size > minFontSize.value && performLayout(constraints, text, size.sp).didOverflowHeight) {
            size -= 1f
        }
        return size.coerceAtLeast(minFontSize.value).sp
    }

    override fun equals(other: Any?): Boolean =
        other is FitMaxLines && other.minFontSize == minFontSize && other.maxFontSize == maxFontSize

    override fun hashCode(): Int = 31 * minFontSize.hashCode() + maxFontSize.hashCode()
}

/** The release date and platform logos, quieter than the title; either can be missing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroFacts(
    releaseDate: String?,
    platforms: List<ParentPlatformType>,
    metrics: HeroMetrics,
) {
    val hasLogos = platforms.any { platform -> platform.iconRes != null }
    if (releaseDate == null && !hasLogos) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.compact),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        releaseDate?.let { date -> ReleaseDateLabel(releaseDate = date) }
        if (hasLogos) {
            PlatformLogoList(
                platforms = platforms,
                imageSize = metrics.logoSize,
                horizontalSpacing = Dimens.small,
            )
        }
    }
}

private val PreviewDetails =
    GameDetails(
        id = 1,
        name = "A Considerably Long Game Title: The Definitive Edition",
        summary = "",
        storyline = "",
        coverImageUrl = "",
        backdrop = null,
        releaseDate = "Feb 3, 2026",
        platforms = listOf(ParentPlatformType.PC, ParentPlatformType.PlayStation, ParentPlatformType.Xbox),
        stores = emptyList(),
        genres = listOf(GenreTag(id = 1, name = "Adventure"), GenreTag(id = 2, name = "Role-playing (RPG)")),
    )

@Preview
@Composable
internal fun GameDetailsHeroCompactPreview() {
    BraindanceTheme {
        GameDetailsHero(
            details = PreviewDetails,
            expanded = false,
            libraryActions = LibraryActionsState.Unavailable,
            onLibraryStatusChange = {},
            onGenreClick = null,
            modifier = Modifier.background(background),
        )
    }
}

@Preview(widthDp = 1024)
@Composable
internal fun GameDetailsHeroExpandedPreview() {
    BraindanceTheme {
        GameDetailsHero(
            details = PreviewDetails.copy(backdrop = BackdropImage(compactUrl = "", wideUrl = "")),
            expanded = true,
            libraryActions = LibraryActionsState.Ready(LibraryStatus.Wishlist),
            onLibraryStatusChange = {},
            onGenreClick = {},
            modifier = Modifier.background(background),
        )
    }
}
