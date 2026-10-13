// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable

private const val LARGE_SCREEN_WIDTH_DP = 600

@Composable
actual fun isLargeDevice(): Boolean = screenSize.width >= LARGE_SCREEN_WIDTH_DP
