// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.about_developer_publisher
import dev.vladleesi.braindanceapp.resources.about_series
import dev.vladleesi.braindanceapp.resources.about_storyline
import dev.vladleesi.braindanceapp.resources.game_facts_title
import dev.vladleesi.braindanceapp.ui.components.ExpandableText
import dev.vladleesi.braindanceapp.ui.components.Tag
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GameFact
import dev.vladleesi.braindanceapp.ui.viewmodels.GameFactsModel
import dev.vladleesi.braindanceapp.utils.placeholder
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
            if (facts.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.tiny),
                    verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
                ) {
                    facts.tags.forEach { Tag(it) }
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
        Text(
            stringResource(fact.label),
            modifier = Modifier.weight(FACT_LABEL_WEIGHT).alignByBaseline(),
            color = secondaryText,
            style = MaterialTheme.typography.caption,
        )
        Text(
            fact.value,
            modifier = Modifier.weight(FACT_VALUE_WEIGHT).alignByBaseline(),
            style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold),
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

@Preview(widthDp = 300)
@Composable
internal fun GameFactsPreview() {
    BraindanceTheme {
        GameFacts(
            GameFactsModel(
                rows =
                    listOf(
                        GameFact(Res.string.about_developer_publisher, "CD Projekt Red"),
                        GameFact(Res.string.about_series, "The Witcher"),
                    ),
                tags = listOf("Single player", "Third person", "Fantasy"),
            ),
            Modifier.padding(Dimens.medium),
        )
    }
}

@Preview
@Composable
internal fun FactCellPreview() {
    BraindanceTheme { FactCell(GameFact(Res.string.about_developer_publisher, "CD Projekt Red")) }
}

@Preview
@Composable
internal fun GameStorylinePreview() {
    BraindanceTheme { GameStoryline("A cartographer must navigate a city whose streets change every night.") }
}
