// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.foundation.clickable as foundationClickable

// Foundation's clickable resolves LocalIndication itself, so no composed modifier is needed. A mouse pointer shows
// the hand cursor, as over a link; touch input is unaffected.
fun Modifier.clickable(
    rippleEnabled: Boolean = true,
    onClick: () -> Unit,
): Modifier =
    if (rippleEnabled) {
        pointerHoverIcon(PointerIcon.Hand)
            .foundationClickable(onClick = onClick)
    } else {
        pointerHoverIcon(PointerIcon.Hand)
            .foundationClickable(interactionSource = null, indication = null, onClick = onClick)
    }
