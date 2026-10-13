// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.white

@Composable
fun ReleaseDateLabel(
    releaseDate: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = white,
    textColor: Color = background,
    textStyle: TextStyle = MaterialTheme.typography.subtitle1,
    shape: Shape = RoundedCornerShape(Dimens.small),
    paddingValues: PaddingValues = PaddingValues(start = Dimens.small, end = Dimens.small),
) {
    Box(
        modifier =
            modifier
                .background(color = backgroundColor, shape = shape)
                .padding(paddingValues),
    ) {
        Text(
            text = releaseDate,
            style = textStyle,
            color = textColor,
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
