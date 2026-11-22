// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.about_developer
import dev.vladleesi.braindanceapp.resources.about_duration
import dev.vladleesi.braindanceapp.resources.about_extra
import dev.vladleesi.braindanceapp.resources.about_hours
import dev.vladleesi.braindanceapp.resources.about_main
import dev.vladleesi.braindanceapp.resources.about_players
import dev.vladleesi.braindanceapp.resources.about_series
import dev.vladleesi.braindanceapp.resources.about_storyline
import dev.vladleesi.braindanceapp.resources.about_tags
import dev.vladleesi.braindanceapp.resources.game_facts_title
import dev.vladleesi.braindanceapp.ui.components.ExpandableText
import dev.vladleesi.braindanceapp.ui.components.Tag
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GameFact
import dev.vladleesi.braindanceapp.ui.viewmodels.GameFactsModel
import dev.vladleesi.braindanceapp.ui.viewmodels.PlayTime
import dev.vladleesi.braindanceapp.utils.placeholder
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val FACT_LABEL_WEIGHT = 2f
private const val FACT_VALUE_WEIGHT = 3f

@Composable
internal fun GameFacts(
    facts: GameFactsModel,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(surface, CornerShapes.large)
            .padding(Dimens.medium),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        Text(
            stringResource(Res.string.game_facts_title),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.semantics { heading() },
        )
        Column(
            modifier = Modifier.placeholder(visible = placeholder, shape = CornerShapes.small),
            verticalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            facts.rows.forEach { fact -> FactCell(fact) }
            if (facts.playTimes.isNotEmpty()) {
                if (facts.rows.isNotEmpty()) Divider(color = secondaryVariant, thickness = Dimens.one)
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
                    Text(
                        stringResource(Res.string.about_duration),
                        color = secondaryText,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.semantics { heading() },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
                        verticalArrangement = Arrangement.spacedBy(Dimens.small),
                    ) {
                        facts.playTimes.forEach { PlayTimeTile(it) }
                    }
                    facts.sampleCount?.let { count ->
                        Text(
                            pluralStringResource(Res.plurals.about_players, count, count),
                            color = secondaryText,
                            style = MaterialTheme.typography.caption,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FactCell(fact: GameFact) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        val labelModifier = Modifier.weight(FACT_LABEL_WEIGHT)
        Text(
            stringResource(fact.label),
            modifier =
                if (fact is GameFact.Text) {
                    labelModifier.alignByBaseline()
                } else {
                    labelModifier.padding(top = Dimens.micro)
                },
            color = secondaryText,
            style = MaterialTheme.typography.caption,
        )
        val valueModifier = Modifier.weight(FACT_VALUE_WEIGHT)
        when (fact) {
            is GameFact.Text ->
                Text(
                    fact.value,
                    modifier = valueModifier.alignByBaseline(),
                    style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold),
                    color = white,
                )

            is GameFact.Tags ->
                FlowRow(
                    modifier = valueModifier,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.tiny),
                    verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
                ) {
                    fact.values.forEach { Tag(it) }
                }
        }
    }
}

@Composable
private fun PlayTimeTile(playTime: PlayTime) {
    Column(
        Modifier
            .background(secondary, CornerShapes.small)
            .padding(horizontal = Dimens.small, vertical = Dimens.tiny)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
    ) {
        Text(stringResource(playTime.label), color = secondaryText, style = MaterialTheme.typography.caption)
        Text(
            stringResource(Res.string.about_hours, playTime.hours),
            style = MaterialTheme.typography.subtitle1,
            color = white,
        )
    }
}

@Composable
internal fun GameStoryline(
    storyline: String,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    onCollapse: (suspend (Int) -> Boolean)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        Text(
            stringResource(Res.string.about_storyline),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.semantics { heading() },
        )
        ExpandableText(text = storyline, placeholder = placeholder, onCollapse = onCollapse)
    }
}

private const val PREVIEW_MAIN_STORY_HOURS = 37L
private const val PREVIEW_MAIN_EXTRAS_HOURS = 82L
private const val PREVIEW_SAMPLE_COUNT = 42
private val PreviewPlayTime = PlayTime(Res.string.about_main, PREVIEW_MAIN_STORY_HOURS)

@Preview(widthDp = 300)
@Composable
internal fun GameFactsPreview() {
    BraindanceTheme {
        GameFacts(
            GameFactsModel(
                rows =
                    listOf(
                        GameFact.Text(Res.string.about_developer, "CD Projekt Red"),
                        GameFact.Text(Res.string.about_series, "The Witcher"),
                        GameFact.Tags(Res.string.about_tags, listOf("Single player", "Third person", "Fantasy")),
                    ),
                playTimes = listOf(PreviewPlayTime, PlayTime(Res.string.about_extra, PREVIEW_MAIN_EXTRAS_HOURS)),
                sampleCount = PREVIEW_SAMPLE_COUNT,
            ),
            Modifier.padding(Dimens.medium),
        )
    }
}

@Preview
@Composable
internal fun FactCellPreview() {
    BraindanceTheme { FactCell(GameFact.Tags(Res.string.about_tags, listOf("Single player", "Multiplayer"))) }
}

@Preview
@Composable
internal fun PlayTimeTilePreview() {
    BraindanceTheme { PlayTimeTile(PreviewPlayTime) }
}

@Preview
@Composable
internal fun GameStorylinePreview() {
    BraindanceTheme { GameStoryline("A cartographer must navigate a city whose streets change every night.") }
}
