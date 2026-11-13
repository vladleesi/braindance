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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white

private val IconSize = 14.dp

/**
 * A game's release date as a quiet chip: a calendar icon and the date on the raised surface color, so it reads as
 * information beside the platform logos and not as a button or tag.
 */
@Composable
fun ReleaseDateLabel(
    releaseDate: String,
    modifier: Modifier = Modifier,
    shape: Shape = CornerShapes.small,
) {
    Row(
        modifier =
            modifier
                .background(color = secondary, shape = shape)
                .padding(horizontal = Dimens.small, vertical = Dimens.tiny),
        horizontalArrangement = Arrangement.spacedBy(Dimens.tiny),
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
            color = white,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
internal fun ReleaseDateLabelPreview() {
    BraindanceTheme {
        ReleaseDateLabel(
            releaseDate = "Feb 3th, 2026",
        )
    }
}
