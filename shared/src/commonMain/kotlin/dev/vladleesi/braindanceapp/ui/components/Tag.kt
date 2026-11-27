// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.HairlineBorder
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.surface

/**
 * A small label for a status or category, such as "Coming soon": an outlined pill, like the landing site's tags. It
 * is information, not a control; use a chip or a button for anything that can be pressed.
 */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = foreground,
) {
    Text(
        text = text,
        modifier =
            modifier
                .border(HairlineBorder, CornerShapes.pill)
                .padding(horizontal = Dimens.small, vertical = Dimens.micro),
        style = MaterialTheme.typography.caption,
        color = color,
    )
}

@Preview
@Composable
internal fun TagPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(surface).padding(Dimens.medium)) {
            Tag(text = "Coming soon")
        }
    }
}
