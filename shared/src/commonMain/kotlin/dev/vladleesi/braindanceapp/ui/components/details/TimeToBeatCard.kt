// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.about_complete
import dev.vladleesi.braindanceapp.resources.about_duration
import dev.vladleesi.braindanceapp.resources.about_extra
import dev.vladleesi.braindanceapp.resources.about_hours
import dev.vladleesi.braindanceapp.resources.about_main
import dev.vladleesi.braindanceapp.resources.about_players
import dev.vladleesi.braindanceapp.ui.components.SurfaceCard
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.icons.Clock
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GamePlayTimes
import dev.vladleesi.braindanceapp.ui.viewmodels.PlayTime
import dev.vladleesi.braindanceapp.utils.placeholder
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TimeToBeatCard(
    playTimes: GamePlayTimes,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
) {
    SurfaceCard(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.compact),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Clock, contentDescription = null, tint = secondaryText)
                Text(
                    stringResource(Res.string.about_duration),
                    style = MaterialTheme.typography.h3,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Column(
                modifier = Modifier.placeholder(visible = placeholder, shape = CornerShapes.small),
                verticalArrangement = Arrangement.spacedBy(Dimens.compact),
            ) {
                playTimes.estimates.forEach { PlayTimeRow(it) }
                playTimes.sampleCount?.let { count ->
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

@Composable
private fun PlayTimeRow(playTime: PlayTime) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.tiny)) {
        Row(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            Text(
                stringResource(playTime.label),
                modifier = Modifier.weight(1f).alignByBaseline(),
                color = secondaryText,
                style = MaterialTheme.typography.body2,
            )
            Text(
                stringResource(Res.string.about_hours, playTime.hours),
                modifier = Modifier.alignByBaseline(),
                color = white,
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.End,
            )
        }
        Box(
            Modifier.fillMaxWidth().height(Dimens.tiny).background(secondaryVariant, CornerShapes.pill),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(playTime.relativeLength)
                    .fillMaxHeight()
                    .background(accent, CornerShapes.pill),
            )
        }
    }
}

private val PreviewMainStory = PlayTime(Res.string.about_main, hours = 24, relativeLength = 0.3f)
private val PreviewPlayTimes =
    GamePlayTimes(
        estimates =
            listOf(
                PreviewMainStory,
                PlayTime(Res.string.about_extra, hours = 52, relativeLength = 0.65f),
                PlayTime(Res.string.about_complete, hours = 80, relativeLength = 1f),
            ),
        sampleCount = 42,
    )

@Preview(widthDp = 300)
@Composable
internal fun TimeToBeatCardPreview() {
    BraindanceTheme { TimeToBeatCard(PreviewPlayTimes, modifier = Modifier.padding(Dimens.medium)) }
}

@Preview
@Composable
internal fun PlayTimeRowPreview() {
    BraindanceTheme { PlayTimeRow(PreviewMainStory) }
}
