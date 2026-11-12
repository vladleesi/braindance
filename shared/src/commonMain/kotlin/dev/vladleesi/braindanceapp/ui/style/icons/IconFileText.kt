// SPDX-License-Identifier: ISC AND MIT
// Adapted icon artwork; see THIRD_PARTY_NOTICES.md and LICENSES/.

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Lucide `file-text`, drawn from its upstream path data with each shape written as a path. */
val FileText: ImageVector
    get() {
        fileText?.let { return it }
        val imageVector =
            ImageVector
                .Builder(
                    name = "FileText",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    outlinePath("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z")
                    outlinePath("M14 2v4a2 2 0 0 0 2 2h4")
                    outlinePath("M10 9H8")
                    outlinePath("M16 13H8")
                    outlinePath("M16 17H8")
                }.build()
        fileText = imageVector
        return imageVector
    }

private var fileText: ImageVector? = null
