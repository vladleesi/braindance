// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

internal val Clock: ImageVector by lazy {
    ImageVector
        .Builder(name = "Clock", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .outlinePath("M 12,3 A 9,9 0 1,1 12,21 A 9,9 0 1,1 12,3 Z M 12,7 V 12 L 15,14")
        .build()
}
