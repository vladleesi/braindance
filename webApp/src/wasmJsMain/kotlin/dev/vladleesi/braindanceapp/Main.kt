// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.ComposeViewport
import dev.vladleesi.braindanceapp.koin.initKoin
import dev.vladleesi.braindanceapp.ui.style.DarkAppColors
import dev.vladleesi.braindanceapp.ui.style.LightAppColors
import dev.vladleesi.braindanceapp.utils.configureVersionedWebResources
import kotlinx.browser.document

private const val RGB_HEX_DIGITS = 6

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    configureVersionedWebResources()
    initKoin()
    ComposeViewport(viewportContainerId = "webApp") {
        WebAppHost {
            BraindanceApp(desktopLayoutEnabled = true, onLightThemeChanged = ::applyBrowserTheme)
        }
    }
}

private fun applyBrowserTheme(lightTheme: Boolean) {
    document.documentElement?.setAttribute("data-theme", if (lightTheme) "light" else "dark")
    val chrome = if (lightTheme) LightAppColors.navBar else DarkAppColors.navBar
    val hex = chrome.toArgb().toHexString().takeLast(RGB_HEX_DIGITS)
    document.querySelector("meta[name='theme-color']")?.setAttribute("content", "#$hex")
}
