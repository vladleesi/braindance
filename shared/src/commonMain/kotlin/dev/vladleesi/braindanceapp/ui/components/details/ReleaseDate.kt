// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import CalendarDays
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.secondaryText

private val IconSize = 14.dp

/**
 * A game's release date as quiet text: a calendar icon and the date, with no fill, so it reads as information beside
 * the platform logos and not as a button or tag, and stays secondary to the title above it.
 */
@Composable
fun ReleaseDateLabel(
    releaseDate: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = CalendarDays,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(IconSize),
        )
        Text(
            text = releaseDate,
            style = MaterialTheme.typography.subtitle2,
            color = foreground,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
internal fun ReleaseDateLabelPreview() {
    BraindanceTheme {
        ReleaseDateLabel(
            releaseDate = "Feb 3, 2026",
            modifier = Modifier.background(background).padding(Dimens.medium),
        )
    }
}
