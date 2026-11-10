// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.neonCyan
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

/**
 * Whether this source has focus that the keyboard moved there, like CSS `:focus-visible`. A click or tap also focuses
 * a component, but Compose switches to touch input mode on every pointer press, so the result is false after pointer
 * use and true again once the keyboard moves focus.
 */
@Composable
fun InteractionSource.isFocusVisible(): Boolean {
    val focused by collectIsFocusedAsState()
    return focused && LocalInputModeManager.current.inputMode == InputMode.Keyboard
}

/** The Braindance keyboard-focus outline, drawn only while [visible]; see [isFocusVisible]. */
fun Modifier.focusRing(
    visible: Boolean,
    shape: Shape,
    width: Dp = Dimens.one,
): Modifier = if (visible) border(width, neonCyan, shape) else this
