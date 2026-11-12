// SPDX-License-Identifier: CC0-1.0
// Simple Icons artwork; the logo is a trademark of its owner. See THIRD_PARTY_NOTICES.md and LICENSES/.

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private const val X_PATH =
    "M18.901 1.153h3.68l-8.04 9.19L24 22.846h-7.406l-5.8-7.584-6.638 7.584H.474l8.6-9.83L0 " +
        "1.154h7.594l5.243 6.932ZM17.61 20.644h2.039L6.486 3.24H4.298Z"

/** The X logo from Simple Icons, for linking to the project's X profile. */
val XLogo: ImageVector
    get() {
        xLogo?.let { return it }
        val imageVector =
            ImageVector
                .Builder(
                    name = "XLogo",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    filledPath(X_PATH)
                }.build()
        xLogo = imageVector
        return imageVector
    }

private var xLogo: ImageVector? = null
