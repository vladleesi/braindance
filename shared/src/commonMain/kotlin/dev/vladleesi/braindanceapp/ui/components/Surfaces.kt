// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.surface

/*
 * The app's containers (docs/branding/README.md#in-the-product). Both are filled with the surface color and never
 * outlined; they differ only in size role. Padding belongs to the content, so lists and images can reach the edges.
 */

/** A container for grouped content, such as a card or a panel: the surface color with the large corner radius. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(modifier = modifier, shape = CornerShapes.large, color = surface, content = content)
}

/** A compact block, such as a tile in a grid or a calendar cell: the surface color with the small corner radius. */
@Composable
fun SurfaceTile(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier, shape = CornerShapes.small, color = surface, content = content)
}

@Preview
@Composable
internal fun SurfacesPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.medium),
        ) {
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Card",
                    modifier = Modifier.padding(Dimens.medium),
                    style = MaterialTheme.typography.subtitle1,
                )
            }
            SurfaceTile {
                Text(
                    text = "Tile",
                    modifier = Modifier.padding(Dimens.medium),
                    style = MaterialTheme.typography.subtitle1,
                )
            }
        }
    }
}
