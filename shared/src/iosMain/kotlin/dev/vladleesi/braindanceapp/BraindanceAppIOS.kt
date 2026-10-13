// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import androidx.compose.ui.window.ComposeUIViewController
import dev.vladleesi.braindanceapp.koin.initKoin
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
fun mainViewController(): UIViewController =
    ComposeUIViewController(configure = { initKoin(debugHttpLogging = Platform.isDebugBinary) }) {
        BraindanceApp()
    }
