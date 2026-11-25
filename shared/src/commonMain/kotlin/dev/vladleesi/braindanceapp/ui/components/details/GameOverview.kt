// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.domain.games.OverviewHighlight
import dev.vladleesi.braindanceapp.domain.games.OverviewHighlight.PlayStyle
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.adult_details_hidden
import dev.vladleesi.braindanceapp.resources.overview_co_op
import dev.vladleesi.braindanceapp.resources.overview_co_op_split_screen
import dev.vladleesi.braindanceapp.resources.overview_main_story
import dev.vladleesi.braindanceapp.resources.overview_mmo
import dev.vladleesi.braindanceapp.resources.overview_multiplayer
import dev.vladleesi.braindanceapp.resources.overview_solo
import dev.vladleesi.braindanceapp.resources.overview_solo_multiplayer
import dev.vladleesi.braindanceapp.resources.overview_split_screen
import dev.vladleesi.braindanceapp.resources.overview_title
import dev.vladleesi.braindanceapp.ui.components.ExpandableText
import dev.vladleesi.braindanceapp.ui.components.Tag
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.utils.placeholder
import org.jetbrains.compose.resources.stringResource

private const val OVERVIEW_LINES = 3

@Composable
internal fun GameOverview(
    summary: String,
    highlights: List<OverviewHighlight>,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    onCollapse: (suspend (Int) -> Boolean)? = null,
    adult: Boolean = false,
) {
    if (summary.isBlank() && highlights.isEmpty() && !adult) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.compact)) {
        Text(
            stringResource(Res.string.overview_title),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.semantics { heading() },
        )
        if (adult) {
            Text(
                stringResource(Res.string.adult_details_hidden),
                style = MaterialTheme.typography.body1,
                color = secondaryText,
            )
        } else if (summary.isNotBlank()) {
            ExpandableText(
                text = summary,
                collapsedMaxLines = OVERVIEW_LINES,
                placeholder = placeholder,
                onCollapse = onCollapse,
            )
        }
        if (highlights.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.small),
                verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
            ) {
                highlights.forEach { highlight ->
                    Tag(
                        text = highlight.label(),
                        modifier = Modifier.placeholder(visible = placeholder, shape = CornerShapes.pill),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewHighlight.label(): String =
    when (this) {
        is OverviewHighlight.MainStory -> stringResource(Res.string.overview_main_story, hours)
        is OverviewHighlight.Theme -> name
        is OverviewHighlight.Perspective -> name
        PlayStyle.SOLO -> stringResource(Res.string.overview_solo)
        PlayStyle.MULTIPLAYER -> stringResource(Res.string.overview_multiplayer)
        PlayStyle.SOLO_AND_MULTIPLAYER -> stringResource(Res.string.overview_solo_multiplayer)
        PlayStyle.MMO -> stringResource(Res.string.overview_mmo)
        PlayStyle.CO_OP -> stringResource(Res.string.overview_co_op)
        PlayStyle.SPLIT_SCREEN -> stringResource(Res.string.overview_split_screen)
        PlayStyle.CO_OP_AND_SPLIT_SCREEN -> stringResource(Res.string.overview_co_op_split_screen)
    }

@Preview
@Composable
internal fun GameOverviewAdultPreview() {
    BraindanceTheme {
        GameOverview(summary = "", highlights = listOf(OverviewHighlight.Theme("Erotic")), adult = true)
    }
}

@Preview
@Composable
internal fun GameOverviewPreview() {
    BraindanceTheme {
        GameOverview(
            "Explore a ruined city with a friend.",
            listOf(PlayStyle.CO_OP, OverviewHighlight.Theme("Survival")),
        )
    }
}
