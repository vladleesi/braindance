// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.error_screen_retry_button_text
import dev.vladleesi.braindanceapp.resources.home_section_empty
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import org.jetbrains.compose.resources.stringResource

private const val ERROR_MESSAGE_LINES = 3

/** A section that failed to load: the error [message] and a Retry button that calls [onRetry]. */
@Composable
fun SectionError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        Text(
            text = message,
            style = MaterialTheme.typography.caption,
            maxLines = ERROR_MESSAGE_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(backgroundColor = secondaryVariant),
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(text = stringResource(Res.string.error_screen_retry_button_text))
        }
    }
}

/** A section that loaded without any items. */
@Composable
fun SectionEmpty(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.home_section_empty),
        style = MaterialTheme.typography.caption,
        modifier = modifier,
    )
}

@Preview
@Composable
internal fun SectionStatusPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.medium),
        ) {
            SectionError(message = "Can't reach the server. Check your connection and try again.", onRetry = {})
            SectionEmpty()
        }
    }
}
