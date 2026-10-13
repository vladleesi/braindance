// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.ui.unit.Dp
import platform.UIKit.UIScreen

actual fun Dp.toPx(): Float = this.value * UIScreen.mainScreen.scale.toFloat()
