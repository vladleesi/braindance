// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement

// Long enough for a screen to open and focus its field; a primer still focused after it closes the keyboard again.
private const val PRIMER_TIMEOUT_MILLIS = 1_500

// One invisible input for the whole app, created on first use, and the timeout of its latest use.
private var primer: HTMLInputElement? = null
private var primerTimeout: Int? = null

/**
 * On touch devices, focuses an invisible input while the tap is still being handled, so iOS opens the keyboard. When
 * the real field takes focus a moment later, the keyboard stays open and types into it. A primer that is still focused
 * after [PRIMER_TIMEOUT_MILLIS], because no field took the focus, gives it up, which closes the keyboard; a new tap
 * restarts that wait. Nothing happens when a text field already has the focus, which the primer would take from it
 * while Compose still sees the field as focused, nor on devices without a touch screen, which have no software
 * keyboard.
 */
internal actual fun prepareSoftwareKeyboard() {
    if (!hasTouchScreen() || isEditingText()) return
    val input = primer ?: createPrimer().also { primer = it }
    input.focus()
    primerTimeout?.let(window::clearTimeout)
    primerTimeout =
        window.setTimeout(
            {
                primerTimeout = null
                if (isFocused(input)) input.blur()
                null
            },
            PRIMER_TIMEOUT_MILLIS,
        )
}

private fun createPrimer(): HTMLInputElement =
    (document.createElement("input") as HTMLInputElement).apply {
        type = "text"
        tabIndex = -1
        setAttribute("aria-hidden", "true")
        setAttribute("autocomplete", "off")
        // Fixed at the top so focusing it never scrolls the page; 16 px text keeps iOS from zooming in.
        style.cssText =
            "position:fixed;top:0;left:0;width:1px;height:1px;opacity:0;border:0;padding:0;margin:0;" +
            "font-size:16px;pointer-events:none;"
        document.body?.appendChild(this)
    }

private fun isFocused(element: HTMLInputElement): Boolean = js("document.activeElement === element")

/** Whether a text field has the focus, also inside a shadow root such as Compose's. */
private fun isEditingText(): Boolean =
    js(
        """(() => {
        let element = document.activeElement;
        while (element && element.shadowRoot && element.shadowRoot.activeElement) {
            element = element.shadowRoot.activeElement;
        }
        return !!element && (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA' || element.isContentEditable);
    })()""",
    )
