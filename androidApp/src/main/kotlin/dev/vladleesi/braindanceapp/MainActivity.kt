// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.navBar

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
    }
}

@Preview
@Composable
fun DefaultPreview() {
    BraindanceTheme {
        Text(text = Greeting().greet(), style = MaterialTheme.typography.h1)
    }
}
