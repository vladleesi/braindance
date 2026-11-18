// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import ChevronLeft
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.data.models.games.GameKind
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_details_related
import dev.vladleesi.braindanceapp.resources.game_details_related_contents
import dev.vladleesi.braindanceapp.resources.game_details_related_extras
import dev.vladleesi.braindanceapp.resources.game_details_related_included_in
import dev.vladleesi.braindanceapp.resources.game_details_related_other_versions
import dev.vladleesi.braindanceapp.resources.game_details_related_tab
import dev.vladleesi.braindanceapp.resources.game_details_show_fewer
import dev.vladleesi.braindanceapp.resources.game_details_show_more_choices
import dev.vladleesi.braindanceapp.resources.game_details_version_base
import dev.vladleesi.braindanceapp.resources.game_details_versions
import dev.vladleesi.braindanceapp.resources.game_relation_for
import dev.vladleesi.braindanceapp.resources.game_relation_of
import dev.vladleesi.braindanceapp.resources.game_relation_part_of
import dev.vladleesi.braindanceapp.ui.components.CardCover
import dev.vladleesi.braindanceapp.ui.components.Chip
import dev.vladleesi.braindanceapp.ui.components.cardClickable
import dev.vladleesi.braindanceapp.ui.components.text
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GameOrigin
import dev.vladleesi.braindanceapp.ui.viewmodels.OriginRelation
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedCategory
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedGame
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedGroup
import dev.vladleesi.braindanceapp.ui.viewmodels.VersionOption
import dev.vladleesi.braindanceapp.ui.viewmodels.VersionSelector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Choices a phone shows before "+N more": the selected one. */
private const val COMPACT_SHOWN_CHOICES = 1

/** Versions a wide layout shows before "+N more"; the selected one is always among them. */
private const val WIDE_SHOWN_CHOICES = 6
private const val COVER_ASPECT_RATIO = 3f / 4f
private val ChevronSize = 14.dp
private val RowCoverWidth = 48.dp

/** Turns the left chevron to point into the row it opens. */
private const val CHEVRON_RIGHT_DEGREES = 180f
private const val PREVIEW_YEAR = 2015

/**
 * Says what the entry is and links the game it belongs to, in one sentence under the title: "Expansion for The
 * Witcher 3: Wild Hunt". Only the game's name is the link, so it reads as a sentence and wraps like one.
 */
@Composable
fun OriginLine(
    origin: GameOrigin,
    onOpenGame: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = origin.kind.text()
    val openGame by rememberUpdatedState(onOpenGame)
    val sentence =
        when (origin.relation) {
            OriginRelation.FOR -> stringResource(Res.string.game_relation_for, kind, origin.gameName)
            OriginRelation.OF -> stringResource(Res.string.game_relation_of, kind, origin.gameName)
            OriginRelation.PART_OF -> stringResource(Res.string.game_relation_part_of, origin.gameName)
        }
    val linkStyles =
        remember {
            TextLinkStyles(
                style = SpanStyle(color = accent),
                hoveredStyle = SpanStyle(color = accent, textDecoration = TextDecoration.Underline),
                focusedStyle = SpanStyle(color = accent, textDecoration = TextDecoration.Underline),
            )
        }
    val text =
        remember(sentence, origin) {
            // The name is found in the formatted sentence, so translations may place it anywhere.
            val start = sentence.lastIndexOf(origin.gameName)
            buildAnnotatedString {
                if (start < 0) {
                    append(sentence)
                } else {
                    append(sentence.substring(0, start))
                    val link = LinkAnnotation.Clickable(tag = "game", styles = linkStyles) { openGame(origin.gameId) }
                    withLink(link) { append(origin.gameName) }
                    append(sentence.substring(start + origin.gameName.length))
                }
            }
        }
    Text(text = text, style = MaterialTheme.typography.subtitle1, color = secondaryText, modifier = modifier)
}

/**
 * The game's versions as one row of chips: the base game and its editions and updates. The selected chip is the
 * version the page shows, marked by a check, an accent outline, and the selected state for screen readers; the line
 * under the chips says what kind of version it is. Every other chip opens its version, so each keeps its own address.
 *
 * In a [compact] layout, more than two versions collapse to the selected one and a chip that shows the rest, so the
 * row stays one line on a phone; wide layouts show up to [WIDE_SHOWN_CHOICES] before collapsing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameVersionSelector(
    selector: VersionSelector,
    compact: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        Text(
            text = stringResource(Res.string.game_details_versions),
            style = MaterialTheme.typography.subtitle2,
            color = secondaryText,
        )
        CollapsibleChoices(
            choices = selector.options,
            isSelected = { option -> option.id == selector.selectedId },
            maxShown = if (compact) COMPACT_SHOWN_CHOICES else WIDE_SHOWN_CHOICES,
            key = VersionOption::id,
        ) { option, selected ->
            Chip(
                text = option.text(),
                selected = selected,
                onClick = { if (!selected) onSelect(option.id) },
            )
        }
        // What the selected version is; its release date is the page's, shown under the title.
        selector.selected?.takeIf { it.kind != GameKind.GAME }?.let { selected ->
            Text(text = selected.kind.text(), style = MaterialTheme.typography.subtitle2, color = white)
        }
    }
}

/**
 * [choices] as a wrapping row of chips drawn by [chip], with the selected one always shown. More than one choice
 * beyond [maxShown] collapses the rest behind a "+N more" chip, which shows them all, and "Show fewer" collapses them
 * again; hiding a single choice would save nothing. Only the shown chips are composed. Whether the row is expanded
 * survives configuration changes and the selection changing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> CollapsibleChoices(
    choices: List<T>,
    isSelected: (T) -> Boolean,
    maxShown: Int,
    key: (T) -> Any,
    chip: @Composable (choice: T, selected: Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedIndex = choices.indexOfFirst(isSelected)
    val collapsed = remember(choices, maxShown, selectedIndex) { collapsedChoices(choices, maxShown, selectedIndex) }
    val shown = if (expanded) choices else collapsed
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        shown.forEach { choice ->
            key(key(choice)) { chip(choice, isSelected(choice)) }
        }
        if (collapsed.size < choices.size) {
            Chip(
                text =
                    if (expanded) {
                        stringResource(Res.string.game_details_show_fewer)
                    } else {
                        stringResource(Res.string.game_details_show_more_choices, choices.size - collapsed.size)
                    },
                onClick = { expanded = !expanded },
            )
        }
    }
}

/**
 * The choices a collapsed row shows: all of them when at most one would be hidden, otherwise the first [maxShown] in
 * order, with the one at [selectedIndex] in the last place if it is not among them.
 */
internal fun <T> collapsedChoices(
    choices: List<T>,
    maxShown: Int,
    selectedIndex: Int,
): List<T> {
    if (choices.size <= maxShown + 1) return choices
    val indices = (0 until maxShown).toMutableList()
    if (selectedIndex >= maxShown) indices[indices.lastIndex] = selectedIndex
    return indices.map(choices::get)
}

@Composable
private fun VersionOption.text(): String = label ?: stringResource(Res.string.game_details_version_base)

/**
 * The heading of a game's related releases and, when there are several kinds, tabs that switch between them. Each tab
 * names its kind and how many there are, so the whole section reads at a glance without opening every tab. In a
 * [compact] layout, more than two tabs collapse to the selected one and a chip that shows the rest.
 */
@Composable
fun RelatedHeader(
    groups: List<RelatedGroup>,
    selected: RelatedCategory,
    compact: Boolean,
    onSelect: (RelatedCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        val single = groups.singleOrNull()
        Text(
            text = stringResource(single?.category?.title ?: Res.string.game_details_related),
            style = MaterialTheme.typography.h3,
        )
        if (single == null) {
            CollapsibleChoices(
                choices = groups,
                isSelected = { group -> group.category == selected },
                maxShown = if (compact) COMPACT_SHOWN_CHOICES else groups.size,
                key = RelatedGroup::category,
            ) { group, isSelected ->
                val title = stringResource(group.category.title)
                Chip(
                    text = "$title ${group.games.size}",
                    selected = isSelected,
                    accessibleText = stringResource(Res.string.game_details_related_tab, title, group.games.size),
                    onClick = { onSelect(group.category) },
                )
            }
        }
    }
}

private val RelatedCategory.title: StringResource
    get() =
        when (this) {
            RelatedCategory.EXTRAS -> Res.string.game_details_related_extras
            RelatedCategory.OTHER_VERSIONS -> Res.string.game_details_related_other_versions
            RelatedCategory.INCLUDED_IN -> Res.string.game_details_related_included_in
            RelatedCategory.CONTENTS -> Res.string.game_details_related_contents
        }

/**
 * A related release as a compact row: a small cover, its name, and what it is with its year. The whole row opens it;
 * the cover shows the press, hover, and focus feedback like the app's cards.
 */
@Composable
fun RelatedGameRow(
    game: RelatedGame,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val kind = game.kind.takeIf { it != GameKind.GAME && it != GameKind.OTHER }?.text()
    val meta = listOfNotNull(kind, game.year?.toString()).joinToString(" · ")
    Row(
        modifier = modifier.cardClickable(interactionSource) { onClick(game.id) }.padding(vertical = Dimens.tiny),
        horizontalArrangement = Arrangement.spacedBy(Dimens.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardCover(
            imageUrl = game.coverUrl,
            memoryCacheKey = game.coverUrl,
            contentDescription = game.title,
            aspectRatio = COVER_ASPECT_RATIO,
            interactionSource = interactionSource,
            modifier = Modifier.width(RowCoverWidth),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.micro)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.subtitle1,
                color = white,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Text(text = meta, style = MaterialTheme.typography.subtitle2, color = secondaryText, maxLines = 1)
            }
        }
        Icon(
            imageVector = ChevronLeft,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(ChevronSize).rotate(CHEVRON_RIGHT_DEGREES),
        )
    }
}

private val PreviewSelector =
    VersionSelector(
        options =
            listOf(
                VersionOption(id = 1, label = null, kind = GameKind.GAME),
                VersionOption(id = 2, label = "Game of the Year Edition", kind = GameKind.EDITION),
                VersionOption(id = 3, label = "Complete Edition", kind = GameKind.EDITION),
                VersionOption(id = 4, label = "Free Next-Gen Update", kind = GameKind.UPDATE),
            ),
        selectedId = 4,
    )

private val PreviewRelated =
    listOf(
        RelatedGroup(
            category = RelatedCategory.EXTRAS,
            games =
                listOf("Sample Expansion", "Another Expansion").mapIndexed { index, title ->
                    RelatedGame(id = index, title = title, kind = GameKind.EXPANSION, year = PREVIEW_YEAR + index, "")
                },
        ),
        RelatedGroup(
            category = RelatedCategory.INCLUDED_IN,
            games = listOf(RelatedGame(2, "Sample Game + Another Game", GameKind.BUNDLE, PREVIEW_YEAR + 2, "")),
        ),
    )

@Preview
@Composable
internal fun GameRelationsPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.large),
        ) {
            OriginLine(
                origin = GameOrigin(GameKind.EXPANSION, OriginRelation.FOR, gameId = 1, gameName = "Sample Game"),
                onOpenGame = {},
            )
            GameVersionSelector(selector = PreviewSelector, compact = true, onSelect = {})
            GameVersionSelector(selector = PreviewSelector, compact = false, onSelect = {})
            RelatedHeader(groups = PreviewRelated, selected = RelatedCategory.EXTRAS, compact = true, onSelect = {})
            PreviewRelated.first().games.forEach { game -> RelatedGameRow(game = game, onClick = {}) }
        }
    }
}
