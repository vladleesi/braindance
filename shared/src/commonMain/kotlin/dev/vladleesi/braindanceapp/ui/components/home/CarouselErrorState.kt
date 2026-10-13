// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.components.StaticText
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant

@Composable
fun CarouselErrorState(
    title: String,
    errorMessage: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        StaticText(
            text = title,
            modifier =
                Modifier
                    .padding(horizontal = Dimens.medium)
                    .padding(top = 9.dp),
            style = MaterialTheme.typography.h2,
        )
        Text(
            text = errorMessage,
            modifier =
                Modifier
                    .padding(horizontal = Dimens.medium)
                    .padding(top = Dimens.small),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.caption,
        )
        Button(
            colors = ButtonDefaults.buttonColors(backgroundColor = secondaryVariant),
            modifier = Modifier.padding(horizontal = Dimens.medium),
            onClick = onRefresh,
        ) {
            Text(text = "Refresh")
        }
    }
}
