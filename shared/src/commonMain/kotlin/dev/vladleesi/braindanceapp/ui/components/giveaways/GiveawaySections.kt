// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.giveaways

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.giveaway_details_added
import dev.vladleesi.braindanceapp.resources.giveaway_details_claimed_by
import dev.vladleesi.braindanceapp.resources.giveaway_details_claimed_count
import dev.vladleesi.braindanceapp.resources.giveaway_details_days_left
import dev.vladleesi.braindanceapp.resources.giveaway_details_ended
import dev.vladleesi.braindanceapp.resources.giveaway_details_ends
import dev.vladleesi.braindanceapp.resources.giveaway_details_ends_today
import dev.vladleesi.braindanceapp.resources.giveaway_details_ends_tomorrow
import dev.vladleesi.braindanceapp.resources.giveaway_details_no_end_date
import dev.vladleesi.braindanceapp.resources.giveaway_details_platforms
import dev.vladleesi.braindanceapp.resources.giveaway_details_view_on_gamerpower
import dev.vladleesi.braindanceapp.ui.components.SurfaceCard
import dev.vladleesi.braindanceapp.ui.components.TonalButton
import dev.vladleesi.braindanceapp.ui.components.profile.ProfileDivider
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.icons.OpenInNew
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayEnding
import dev.vladleesi.braindanceapp.utils.formatShortDate
import dev.vladleesi.braindanceapp.utils.withThousandsSeparators
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// The details list counts the days only within a month; further out, the date says enough.
private const val COUNTDOWN_DAYS = 30

private val StepNumberSize = 24.dp
private val SourceIconSize = 18.dp

/** How soon a giveaway that ends in [daysLeft] days ends: today, tomorrow, or in days. */
@Composable
internal fun endingSummary(daysLeft: Int): String =
    when (daysLeft) {
        0 -> stringResource(Res.string.giveaway_details_ends_today)
        1 -> stringResource(Res.string.giveaway_details_ends_tomorrow)
        else -> pluralStringResource(Res.plurals.giveaway_details_days_left, daysLeft, daysLeft)
    }

/**
 * The giveaway's facts as a list, the way Profile lists its rows: when it ends, how many players claimed it, when it
 * was added, and every platform and store it is for. Rows with nothing to say are left out.
 */
@Composable
fun GiveawayFactsCard(
    ending: GiveawayEnding,
    claimedCount: Int,
    addedOn: LocalDate?,
    platformNames: List<String>,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier.fillMaxWidth()) {
        Column {
            EndingRow(ending = ending)
            if (claimedCount > 0) {
                ProfileDivider()
                FactRow(
                    label = stringResource(Res.string.giveaway_details_claimed_by),
                    value =
                        stringResource(
                            Res.string.giveaway_details_claimed_count,
                            claimedCount.withThousandsSeparators(),
                        ),
                )
            }
            addedOn?.let { date ->
                ProfileDivider()
                FactRow(label = stringResource(Res.string.giveaway_details_added), value = date.formatShortDate())
            }
            if (platformNames.isNotEmpty()) {
                ProfileDivider()
                FactRow(
                    label = stringResource(Res.string.giveaway_details_platforms),
                    value = platformNames.joinToString(", "),
                )
            }
        }
    }
}

@Composable
private fun EndingRow(ending: GiveawayEnding) {
    val label = stringResource(Res.string.giveaway_details_ends)
    when (ending) {
        GiveawayEnding.OpenEnded ->
            FactRow(
                label = label,
                value = stringResource(Res.string.giveaway_details_no_end_date),
            )
        is GiveawayEnding.EndsOn ->
            FactRow(
                label = label,
                value = ending.date.formatShortDate(),
                supportingText = if (ending.daysLeft <= COUNTDOWN_DAYS) endingSummary(ending.daysLeft) else null,
            )
        is GiveawayEnding.Ended ->
            FactRow(
                label = label,
                value = stringResource(Res.string.giveaway_details_ended),
                supportingText = ending.date?.formatShortDate(),
            )
    }
}

/** A [label] on the start and its [value] on the end, with optional [supportingText] under the value. */
@Composable
private fun FactRow(
    label: String,
    value: String,
    supportingText: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = Dimens.medium, vertical = Dimens.compact),
    ) {
        // The label and the value share a baseline; the supporting text hangs under the value.
        Text(
            text = label,
            style = MaterialTheme.typography.subtitle2,
            color = secondaryText,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(Dimens.medium))
        Column(modifier = Modifier.weight(1f).alignByBaseline(), horizontalAlignment = Alignment.End) {
            Text(text = value, style = MaterialTheme.typography.subtitle1, color = white, textAlign = TextAlign.End)
            supportingText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.caption,
                    color = secondaryText,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/** A titled block of the page, such as the description or the claim steps. */
@Composable
fun GiveawaySection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.compact)) {
        Text(
            text = title,
            style = MaterialTheme.typography.h3,
            color = white,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/**
 * The claim instructions as numbered steps. A single step is plain text, since a lone number would suggest more to
 * come. The text can be selected, for example to copy a code.
 */
@Composable
fun ClaimSteps(
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    SelectionContainer(modifier = modifier) {
        if (steps.size == 1) {
            Text(text = steps.single(), style = MaterialTheme.typography.body2, color = white)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.compact)) {
                steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.compact)) {
                        StepNumber(number = index + 1)
                        Text(
                            text = step,
                            style = MaterialTheme.typography.body2,
                            color = white,
                            modifier = Modifier.weight(1f).padding(top = Dimens.micro),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepNumber(number: Int) {
    Box(
        modifier = Modifier.size(StepNumberSize).background(secondary, CornerShapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = number.toString(), style = MaterialTheme.typography.caption, color = white)
    }
}

/** Opens the giveaway's page on GamerPower, where its data comes from, so the source is credited where it is used. */
@Composable
fun GiveawaySourceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalButton(onClick = onClick, modifier = modifier) {
        Text(
            text = stringResource(Res.string.giveaway_details_view_on_gamerpower),
            style = MaterialTheme.typography.subtitle1,
        )
        Spacer(Modifier.width(Dimens.small))
        // The label already says where it goes; the icon only marks that it leaves the app.
        Icon(imageVector = OpenInNew, contentDescription = null, modifier = Modifier.size(SourceIconSize))
    }
}

@Preview
@Composable
internal fun GiveawayFactsCardPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(background).padding(Dimens.medium)) {
            GiveawayFactsCard(
                ending = GiveawayEnding.EndsOn(date = LocalDate.parse("2026-11-20"), daysLeft = 3),
                claimedCount = 12_500,
                addedOn = LocalDate.parse("2026-11-12"),
                platformNames = listOf("PC", "Epic Games Store"),
            )
        }
    }
}

@Preview
@Composable
internal fun ClaimStepsPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.large),
        ) {
            GiveawaySection(title = "How to claim") {
                ClaimSteps(
                    steps =
                        listOf(
                            "Click the button to visit the giveaway page.",
                            "Log in to your Epic Games Store account.",
                            "Click the button to add the game to your library.",
                        ),
                )
            }
            GiveawaySourceButton(onClick = {})
        }
    }
}
