// SPDX-License-Identifier: ISC AND MIT
// Adapted icon artwork; see THIRD_PARTY_NOTICES.md and LICENSES/.

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Lucide `database`, drawn from its upstream path data with each shape written as a path. */
val Database: ImageVector
    get() {
        database?.let { return it }
        val imageVector =
            ImageVector
                .Builder(
                    name = "Database",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    outlinePath("M21 5a9 3 0 1 1-18 0 9 3 0 0 1 18 0Z")
                    outlinePath("M3 5V19A9 3 0 0 0 21 19V5")
                    outlinePath("M3 12A9 3 0 0 0 21 12")
                }.build()
        database = imageVector
        return imageVector
    }

private var database: ImageVector? = null
