// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

/**
 * Opens the software keyboard ahead of a text field that takes focus a moment later, such as Discover's search field
 * after a search button opens the screen. Call it while handling the user's tap. Safari on iPhone and iPad opens the
 * keyboard only for focus given during a tap, not for focus a screen gives its field after it opens; other platforms
 * open it for the field itself, so there this does nothing.
 */
internal expect fun prepareSoftwareKeyboard()
