// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.navBar

@Composable
fun StatusBarOverlay(
    modifier: Modifier = Modifier,
    backgroundColor: Color = navBar,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Dimens.statusBarInsets)
            .background(backgroundColor),
    )
}
