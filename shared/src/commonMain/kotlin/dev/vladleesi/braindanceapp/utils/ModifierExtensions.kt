// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.clickable(
    rippleEnabled: Boolean = true,
    onClick: () -> Unit,
): Modifier =
    composed {
        clickable(
            indication = if (rippleEnabled) LocalIndication.current else null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
        )
    }
