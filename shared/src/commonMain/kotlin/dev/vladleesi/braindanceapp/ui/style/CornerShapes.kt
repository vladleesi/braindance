// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Rounded-corner shapes named after the [Dimens] radius they use; for example, [small] rounds by [Dimens.small].
 * They are created once, so composables reuse the same instances instead of allocating a shape on every
 * recomposition. Use [androidx.compose.foundation.shape.CircleShape] for fully round shapes.
 */
object CornerShapes {
    val tiny = RoundedCornerShape(Dimens.tiny)
    val small = RoundedCornerShape(Dimens.small)
    val compact = RoundedCornerShape(Dimens.compact)
    val medium = RoundedCornerShape(Dimens.medium)
}
