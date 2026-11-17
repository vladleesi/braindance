// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import androidx.compose.ui.window.ComposeUIViewController
import dev.vladleesi.braindanceapp.koin.initKoin
import dev.vladleesi.braindanceapp.navigation.AppLinkInbox
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

// The app has a single scene, so one inbox serves its only view controller.
private val appLinks = AppLinkInbox()

@OptIn(ExperimentalNativeApi::class)
fun mainViewController(): UIViewController =
    ComposeUIViewController(configure = { initKoin(isDebugBuild = Platform.isDebugBinary) }) {
        BraindanceApp(appLinks = appLinks)
    }

/**
 * Opens the screen of a Universal Link to the Web app that iOS handed to the app, whether it launched the app or
 * reached it while running. Links this app does not serve are ignored.
 */
fun openAppLink(url: String) {
    appLinks.open(url)
}
