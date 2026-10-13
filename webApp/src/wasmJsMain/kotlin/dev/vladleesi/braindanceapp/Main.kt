// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.vladleesi.braindanceapp.koin.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport(viewportContainerId = "webApp") {
        BraindanceApp()
    }
}
