// SPDX-License-Identifier: ISC AND MIT
// Adapted icon artwork; see THIRD_PARTY_NOTICES.md and LICENSES/.

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Lucide `gift`, drawn from its upstream path data with each shape written as a path. */
val Gift: ImageVector
    get() {
        gift?.let { return it }
        val imageVector =
            ImageVector
                .Builder(
                    name = "Gift",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    outlinePath("M4 8h16a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1Z")
                    outlinePath("M12 8v13")
                    outlinePath("M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7")
                    outlinePath("M7.5 8a2.5 2.5 0 0 1 0-5A4.8 8 0 0 1 12 8a4.8 8 0 0 1 4.5-5 2.5 2.5 0 0 1 0 5")
                }.build()
        gift = imageVector
        return imageVector
    }

private var gift: ImageVector? = null
