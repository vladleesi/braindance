// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.kmpsharekit.ShareKit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(navBar.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(navBar.toArgb()),
        )
        setContent {
            BraindanceApp()
        }
        // The Compose root paints every pixel opaque, so the theme's window background would be one more hidden
        // full-screen fill each frame. The starting window still shows the theme background during launch.
        window.setBackgroundDrawable(null)
    }

    override fun onResume() {
        super.onResume()
        ShareKit.setActivity(this)
    }
}
