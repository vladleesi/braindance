// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable

/**
 * Whether the person asked the system for less motion: Android's "Remove animations", iOS's Reduce Motion, or the
 * browser's `prefers-reduced-motion`. Decorative motion, such as parallax and layout transitions, then stays still or
 * changes at once.
 */
@Composable
internal expect fun prefersReducedMotion(): Boolean
