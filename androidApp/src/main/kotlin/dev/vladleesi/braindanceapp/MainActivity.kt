// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import dev.vladleesi.braindanceapp.navigation.AppLinkInbox
import dev.vladleesi.braindanceapp.ui.style.DarkAppColors
import dev.vladleesi.braindanceapp.ui.style.LightAppColors
import dev.vladleesi.kmpsharekit.ShareKit

class MainActivity : ComponentActivity() {
    private val appLinks = AppLinkInbox()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarTheme(lightTheme = false)
        // A recreated activity keeps its launch intent but restores its own navigation, so only a fresh start opens
        // the link it was launched with.
        val launchLink = if (savedInstanceState == null) intent.appLink() else null
        setContent {
            BraindanceApp(launchLink = launchLink, appLinks = appLinks, onLightThemeChanged = ::applySystemBarTheme)
        }
        // The Compose root paints every pixel opaque, so the theme's window background would be one more hidden
        // full-screen fill each frame. The starting window still shows the theme background during launch.
        window.setBackgroundDrawable(null)
    }

    private fun applySystemBarTheme(lightTheme: Boolean) {
        val chrome = if (lightTheme) LightAppColors.navBar.toArgb() else DarkAppColors.navBar.toArgb()
        val style =
            if (lightTheme) {
                SystemBarStyle.light(
                    chrome,
                    DarkAppColors.navBar.toArgb(),
                )
            } else {
                SystemBarStyle.dark(chrome)
            }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    // A link opened while this activity is on top of its task, including from the background, keeps the running app
    // and its navigation; the shared code opens the link's screen on top of the current tab.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.appLink()?.let(appLinks::open)
    }

    override fun onResume() {
        super.onResume()
        ShareKit.setActivity(this)
    }
}

/** The link this intent opens, or null for a launch without one, including a relaunch from Recents. */
private fun Intent.appLink(): String? {
    val fromRecents = flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
    return dataString.takeIf { action == Intent.ACTION_VIEW && !fromRecents }
}
