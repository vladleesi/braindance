// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.white

@Composable
fun GlobalLoading(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .size(64.dp)
                    .align(Alignment.Center),
            color = white,
            strokeWidth = Dimens.tiny,
        )
    }
}

@Preview
@Composable
internal fun GlobalLoadingPreview() {
    BraindanceTheme {
        GlobalLoading()
    }
}
