// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The window's size from the activity's current configuration. The activity handles rotation and resizing itself (see
 * the manifest), and Compose's `LocalWindowInfo.containerSize` is measured while that change is still being delivered,
 * before the window takes its new bounds, so it trails one rotation behind. The configuration is the new one by the
 * time the screen recomposes.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
internal actual fun windowSize(): DpSize {
    val configuration = LocalConfiguration.current
    return DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
}
