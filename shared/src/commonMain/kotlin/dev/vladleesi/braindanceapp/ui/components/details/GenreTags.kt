// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.components.Tag
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GenreTag
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible

/**
 * A game's genres, wrapping onto further rows. With [onGenreClick] each genre is a filled chip that opens it; without
 * one, for example while there is nothing to browse by genre, each is an informational [Tag], so nothing looks
 * pressable that does nothing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreTags(
    genres: List<GenreTag>,
    onGenreClick: ((GenreTag) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        genres.forEach { genre ->
            key(genre.id) {
                if (onGenreClick == null) {
                    Tag(text = genre.name)
                } else {
                    GenreChip(text = genre.name, onClick = { onGenreClick(genre) })
                }
            }
        }
    }
}

/** A pressable genre: a filled pill with the press, hover, and keyboard focus feedback of the app's controls. */
@Composable
private fun GenreChip(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        color = white,
        modifier =
            Modifier
                // No minimumInteractiveComponentSize: it would add its 48 dp to the layout and space the rows far
                // apart. Compose already extends touch input on small clickable elements to the minimum touch target.
                .clip(CornerShapes.pill)
                .background(secondary)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.pill, Dimens.micro)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                ).pointerHoverIcon(PointerIcon.Hand)
                // 32 dp tall: taller than an informational Tag, as fits something pressable.
                .padding(horizontal = Dimens.compact, vertical = Dimens.small),
    )
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
        }
    }
}
