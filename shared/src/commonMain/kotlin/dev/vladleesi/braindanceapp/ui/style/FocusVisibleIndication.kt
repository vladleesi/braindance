// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalInputModeManager
import kotlinx.coroutines.launch

/**
 * Wraps [indication] so that it shows focus only while the keyboard moves it, like CSS `:focus-visible` and the app's
 * focus ring. Press, hover, and drag feedback pass through unchanged.
 *
 * Compose switches to touch input mode on every pointer press, before the press is handled. So a component that a
 * click focuses never shows the highlight, and a keyboard-focused one hides it as soon as the pointer is used.
 */
@Stable
class FocusVisibleIndication(
    private val indication: IndicationNodeFactory,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        FocusVisibleIndicationNode(interactionSource, indication)

    override fun equals(other: Any?): Boolean = other is FocusVisibleIndication && other.indication == indication

    override fun hashCode(): Int = indication.hashCode()
}

private class FocusVisibleIndicationNode(
    private val source: InteractionSource,
    indication: IndicationNodeFactory,
) : DelegatingNode(),
    CompositionLocalConsumerModifierNode {
    // What the wrapped indication sees: every interaction except focus that is not visible.
    private val visibleInteractions = MutableInteractionSource()
    private var sourceFocus: FocusInteraction.Focus? = null
    private var shownFocus: FocusInteraction.Focus? = null

    init {
        delegate(indication.create(visibleInteractions))
    }

    override fun onAttach() {
        val inputModeManager = currentValueOf(LocalInputModeManager)
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> sourceFocus = interaction
                    is FocusInteraction.Unfocus -> sourceFocus = null
                    else -> visibleInteractions.emit(interaction)
                }
                updateShownFocus(inputModeManager)
            }
        }
        coroutineScope.launch {
            snapshotFlow { inputModeManager.inputMode }.collect { updateShownFocus(inputModeManager) }
        }
    }

    override fun onDetach() {
        sourceFocus = null
        shownFocus = null
    }

    private suspend fun updateShownFocus(inputModeManager: InputModeManager) {
        val visible = sourceFocus != null && inputModeManager.inputMode == InputMode.Keyboard
        val shown = shownFocus
        if (visible && shown == null) {
            val focus = FocusInteraction.Focus()
            shownFocus = focus
            visibleInteractions.emit(focus)
        } else if (!visible && shown != null) {
            shownFocus = null
            visibleInteractions.emit(FocusInteraction.Unfocus(shown))
        }
    }
}
