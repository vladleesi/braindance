// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.vladleesi.braindanceapp.koin.initKoin
import dev.vladleesi.braindanceapp.utils.configureVersionedWebResources

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    configureVersionedWebResources()
    initKoin()
    ComposeViewport(viewportContainerId = "webApp") {
        BraindanceApp(desktopLayoutEnabled = true)
    }
}
