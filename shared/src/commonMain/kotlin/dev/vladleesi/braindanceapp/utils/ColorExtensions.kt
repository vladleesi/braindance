// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

fun Modifier.withCircleRippleEffect(color: Color) = then(Modifier.background(color = color, shape = CircleShape))
