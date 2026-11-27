// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The app's shape system (docs/branding/README.md#shape): soft geometry, in three steps.
 * - [small] rounds imagery and compact elements: game covers, screenshots, thumbnails, tiles, and rows.
 * - [large] rounds containers: cards, panels, and banners.
 * - [pill] is for what can be pressed or what labels something: buttons, chips, tags, and the search field.
 * Icon buttons and the avatar are circles ([androidx.compose.foundation.shape.CircleShape]). The landing site keeps
 * its imagery square; that editorial treatment is for marketing, not for the app. The shapes are created once, so
 * composables reuse them.
 */
object CornerShapes {
    val small = RoundedCornerShape(Dimens.small)
    val large = RoundedCornerShape(Dimens.medium)
    val pill = RoundedCornerShape(percent = 50)
}

/** The outline of tags and outlined controls. Cards and panels are filled, not outlined. */
val HairlineBorder: BorderStroke
    @Composable @ReadOnlyComposable
    get() = BorderStroke(Dimens.one, secondaryVariant)
