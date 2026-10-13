// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.error_screen_error_message
import dev.vladleesi.braindanceapp.resources.error_screen_retry_button_text
import dev.vladleesi.braindanceapp.resources.error_screen_title
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import org.jetbrains.compose.resources.stringResource

private const val MAX_ERROR_MESSAGE_LINES = 10

@Composable
fun ErrorScreen(
    title: String = stringResource(Res.string.error_screen_title),
    errorMessage: String = stringResource(Res.string.error_screen_error_message),
    retryButtonText: String = stringResource(Res.string.error_screen_retry_button_text),
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    // TODO: Add back button
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(background)
                .padding(Dimens.medium),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h1,
                modifier = Modifier.padding(bottom = Dimens.small),
            )

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.caption,
                maxLines = MAX_ERROR_MESSAGE_LINES,
                modifier = Modifier.padding(horizontal = Dimens.medium, vertical = Dimens.small),
            )

            Spacer(modifier = Modifier.height(Dimens.medium))

            Button(onClick = onRetry) {
                Text(text = retryButtonText, style = MaterialTheme.typography.button)
            }
        }
    }
}
