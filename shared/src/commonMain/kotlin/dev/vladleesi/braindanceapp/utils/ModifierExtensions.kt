// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable as foundationClickable

// Foundation's clickable resolves LocalIndication itself, so no composed modifier is needed.
fun Modifier.clickable(
    rippleEnabled: Boolean = true,
    onClick: () -> Unit,
): Modifier =
    if (rippleEnabled) {
        foundationClickable(onClick = onClick)
    } else {
        foundationClickable(interactionSource = null, indication = null, onClick = onClick)
    }
