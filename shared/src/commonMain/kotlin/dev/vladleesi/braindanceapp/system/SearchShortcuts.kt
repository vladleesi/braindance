// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable

/**
 * Calls [onShortcut] for the keyboard shortcuts that go to search, while this is in the composition: "/" when no text
 * field has the focus, and Ctrl+K or ⌘K anywhere, as on other sites with a search field in their header. Only the Web
 * has them. On Android and iOS this does nothing.
 */
@Composable
internal expect fun SearchShortcuts(onShortcut: () -> Unit)
