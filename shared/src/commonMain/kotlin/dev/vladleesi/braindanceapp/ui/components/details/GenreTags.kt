// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GenreTag

@OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Composable
fun GenreTags(
    genres: List<GenreTag>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.medium),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(Dimens.tiny),
    // TODO: Improve vertical spacing
    @Suppress("MagicNumber")
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy((-10).dp),
    backgroundColor: Color = secondaryVariant,
    textStyle: TextStyle = MaterialTheme.typography.caption,
    textColor: Color = white,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
    ) {
        genres.forEach { genre ->
            Chip(
                colors = ChipDefaults.chipColors(backgroundColor = backgroundColor),
                onClick = {
                    onClick.invoke(genre.id)
                },
            ) {
                Text(
                    style = textStyle,
                    text = genre.name,
                    color = textColor,
                )
            }
        }
    }
}

@Preview
@Composable
internal fun GenreTagsPreview() {
    BraindanceTheme {
        GenreTags(
            genres =
                listOf(
                    GenreTag(id = 1, name = "Action"),
                    GenreTag(id = 2, name = "Adventure"),
                    GenreTag(id = 3, name = "Indie"),
                    GenreTag(id = 4, name = "Strategy"),
                    GenreTag(id = 5, name = "RPG"),
                    GenreTag(id = 6, name = "Simulation"),
                ),
            onClick = {},
        )
    }
}
