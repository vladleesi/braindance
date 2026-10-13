// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
actual val screenSize: ScreenSize
    @Composable
    get() {
        val (width, height) = UIScreen.mainScreen.bounds.useContents { size.width to size.height }
        return ScreenSize(
            width = width.toFloat(),
            height = height.toFloat(),
        )
    }
