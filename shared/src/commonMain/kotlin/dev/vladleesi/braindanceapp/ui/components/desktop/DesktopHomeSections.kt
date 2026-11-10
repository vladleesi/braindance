// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.components.SectionEmpty
import dev.vladleesi.braindanceapp.ui.components.SectionError
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardLabel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState

private val CardGap = Dimens.medium

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
 * A single, non-scrolling row of [columns] cards for one section [state]. Loading shows [skeleton]s with the loaded
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
                SectionEmpty()
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

private val PreviewSectionWidth = 840.dp

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
            val game =
                GameCardModel(
                    id = 1,
                    title = "Sample game",
                    backgroundImageUrl = "",
                    label = GameCardLabel.NewRelease,
                )
            DesktopSection(title = "Popular Right Now") {
                DesktopCardRow(
                    state = SectionState.Success(listOf(game, game.copy(id = 2, label = null))),
                    columns = DesktopGridColumns.forWidth(PreviewSectionWidth).games,
                    onRetry = {},
                    skeleton = { itemModifier -> GameCardSkeleton(showTitle = true, modifier = itemModifier) },
                ) { card, itemModifier ->
                    GameCard(card = card, onClick = {}, showTitle = true, modifier = itemModifier)
                }
            }
            DesktopSection(title = "Most Anticipated") {
                DesktopCardRow<GameCardModel>(
                    state = SectionState.Loading,
                    columns = DesktopGridColumns.forWidth(PreviewSectionWidth).anticipated,
                    onRetry = {},
                    skeleton = { itemModifier -> GameCardSkeleton(showTitle = false, modifier = itemModifier) },
                ) { card, itemModifier ->
                    GameCard(card = card, onClick = {}, showTitle = false, modifier = itemModifier)
                }
            }
        }
    }
}
