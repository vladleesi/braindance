// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.ui.style.Dimens

@Composable
fun SpacerStatusBarInsets() {
    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
}

@Composable
fun SpacerNavigationBarInsets() {
    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

@Composable
fun SpacerTopBarWithStatusBarInsets() {
    Spacer(Modifier.height(Dimens.topBarHeightWithInsets))
}
