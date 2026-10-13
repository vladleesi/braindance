// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

actual val screenSize: ScreenSize
    @Composable
    get() {
        val configuration = LocalConfiguration.current
        return ScreenSize(
            width = configuration.screenWidthDp.toFloat(),
            height = configuration.screenHeightDp.toFloat(),
        )
    }
