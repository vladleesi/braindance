// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.components.Chip
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.GenreTag

/**
 * A game's genres as [Chip]s, wrapping onto further rows. With [onGenreClick] each opens its genre; without one, for
 * example while there is nothing to browse by genre, they are information and nothing looks pressable that does
 * nothing. While [placeholder], each chip is a shimmer of its own size.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreTags(
    genres: List<GenreTag>,
    onGenreClick: ((GenreTag) -> Unit)?,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        genres.forEach { genre ->
            key(genre.id) {
                Chip(
                    text = genre.name,
                    onClick = onGenreClick?.let { open -> { open(genre) } },
                    placeholder = placeholder,
                )
            }
        }
    }
}

private val PreviewGenres =
    listOf(
        GenreTag(id = 1, name = "Action"),
        GenreTag(id = 2, name = "Adventure"),
        GenreTag(id = 3, name = "Indie"),
        GenreTag(id = 4, name = "Role-playing (RPG)"),
        GenreTag(id = 5, name = "Strategy"),
    )

@Preview
@Composable
internal fun GenreTagsPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.medium),
        ) {
            GenreTags(genres = PreviewGenres, onGenreClick = null)
            GenreTags(genres = PreviewGenres, onGenreClick = {})
            GenreTags(genres = PreviewGenres, onGenreClick = null, placeholder = true)
        }
    }
}
