// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.offset
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.LocalAppColors
import kotlin.math.roundToInt
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

/**
 * Reports whether each pointer event, including wheel scrolls, comes from a mouse or trackpad. It observes events
 * before its children without consuming them.
 */
fun Modifier.observePointerType(onPointerType: (isMouse: Boolean) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                onPointerType(event.changes.any { change -> change.type == PointerType.Mouse })
            }
        }
    }

/**
 * Pads the bottom by the part of the software keyboard that covers this layout, so it ends at the keyboard's top
 * edge. `imePadding()` counts the keyboard from the bottom of the window, so under the bottom navigation bar, which the
 * keyboard covers as well, it also counted the bar and left a gap of the bar's height above the keyboard.
 *
 * Apply it to a viewport, before a scroll modifier: inside scrolled content, the layout's position moves with the
 * scroll.
 */
@Composable
fun Modifier.keyboardPadding(): Modifier = coveredBottomPadding(WindowInsets.ime)

/** Bottom padding for the part of [insets] that overlaps this layout, with [insets] measured from the root's bottom. */
internal fun Modifier.coveredBottomPadding(insets: WindowInsets): Modifier =
    this then CoveredBottomPaddingElement(insets)

private data class CoveredBottomPaddingElement(
    val insets: WindowInsets,
) : ModifierNodeElement<CoveredBottomPaddingNode>() {
    override fun create() = CoveredBottomPaddingNode(insets)

    override fun update(node: CoveredBottomPaddingNode) {
        node.update(insets)
    }
}

private class CoveredBottomPaddingNode(
    private var insets: WindowInsets,
) : Modifier.Node(),
    LayoutModifierNode,
    LayoutAwareModifierNode {
    // From the last placement. It changes only when the layout moves, not while the keyboard animates; a change
    // measures again.
    private var distanceToRootBottom by mutableIntStateOf(0)

    fun update(insets: WindowInsets) {
        if (insets != this.insets) {
            this.insets = insets
            invalidateMeasurement()
        }
    }

    // Reading the insets while measuring keeps the keyboard animation to layout passes, without recomposition.
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val covered = (insets.getBottom(this) - distanceToRootBottom).coerceAtLeast(0)
        val placeable = measurable.measure(constraints.offset(vertical = -covered))
        return layout(placeable.width, constraints.constrainHeight(placeable.height + covered)) {
            placeable.place(0, 0)
        }
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val root = coordinates.findRootCoordinates()
        val bottom = root.localBoundingBoxOf(coordinates, clipBounds = false).bottom
        distanceToRootBottom = (root.size.height - bottom).roundToInt().coerceAtLeast(0)
    }
}

/**
 * The Braindance keyboard-focus outline in the theme's accent, drawn only while [visible] (see [isFocusVisible]). Like
 * a border, it stays inside the element's bounds.
 */
fun Modifier.focusRing(
    visible: Boolean,
    shape: Shape,
    width: Dp = Dimens.one,
): Modifier = if (visible) this then FocusRingElement(shape, width) else this

private data class FocusRingElement(
    val shape: Shape,
    val width: Dp,
) : ModifierNodeElement<FocusRingNode>() {
    override fun create() = FocusRingNode(shape, width)

    override fun update(node: FocusRingNode) {
        node.shape = shape
        node.width = width
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusRing"
        properties["shape"] = shape
        properties["width"] = width
    }
}

/** Reads the accent while drawing, so the ring follows the theme without anything recomposing. */
private class FocusRingNode(
    var shape: Shape,
    var width: Dp,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
        val stroke = width.toPx()
        val outline = shape.createOutline(Size(size.width - stroke, size.height - stroke), layoutDirection, this)
        translate(left = stroke / 2, top = stroke / 2) {
            drawOutline(outline, currentValueOf(LocalAppColors).accent, style = Stroke(stroke))
        }
    }
}
