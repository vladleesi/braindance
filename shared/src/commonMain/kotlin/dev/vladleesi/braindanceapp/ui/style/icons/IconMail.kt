// SPDX-License-Identifier: ISC AND MIT
// Adapted icon artwork; see THIRD_PARTY_NOTICES.md and LICENSES/.

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Lucide `mail`, drawn from its upstream path data with its rectangle written as a path. */
val Mail: ImageVector
    get() {
        mail?.let { return it }
        val imageVector =
            ImageVector
                .Builder(
                    name = "Mail",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    outlinePath("M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z")
                    outlinePath("m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7")
                }.build()
        mail = imageVector
        return imageVector
    }

private var mail: ImageVector? = null
