// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import ChevronLeft
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.icons.OpenInNew
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.signalYellow
import dev.vladleesi.braindanceapp.ui.style.void
import dev.vladleesi.braindanceapp.ui.style.white

// Comfortable for touch, and the same height for every text button so rows of them align.
private val ButtonMinHeight = 48.dp
private val ButtonContentPadding = PaddingValues(horizontal = 24.dp, vertical = Dimens.compact)
private val TrailingIconSize = 18.dp

// The Echo trail, as on the landing site's primary button: the button lifts by one step, and flat cyan, magenta, and
// yellow copies appear one, two, and three steps behind it.
private val EchoStep = 3.dp
private val EchoColors = listOf(neonCyan, hotMagenta, signalYellow)
private const val ECHO_MILLIS = 120

private const val OUTLINE_ALPHA = 0.4f
private val OutlineArrowSize = 16.dp

// The chevron icon points left; turned around, it is the forward arrow.
private const val OUTLINE_ARROW_ROTATION = 180f

/**
 * The main action of a view: a white pill with dark text, the logo in miniature. Under a mouse pointer it lifts and
 * the Echo trail appears behind it, the same effect as the landing site's primary button; touch screens have no hover,
 * so there it stays flat. Use it once per view, for the action the view exists for. [trailingIcon] marks what the
 * action does, for example opening another site; [trailingIconDescription] says so to screen readers.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    trailingIconDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered = interactionSource.collectIsHoveredAsState()
    // Read only while drawing, so hovering redraws the button without recomposing it.
    val lift = animateFloatAsState(targetValue = if (hovered.value) 1f else 0f, animationSpec = tween(ECHO_MILLIS))
    Button(
        onClick = onClick,
        modifier =
            modifier
                .heightIn(min = ButtonMinHeight)
                .graphicsLayer {
                    val shift = -EchoStep.toPx() * lift.value
                    translationX = shift
                    translationY = shift
                }.drawBehind {
                    val step = EchoStep.toPx() * lift.value
                    if (step > 0f) {
                        val corner = CornerRadius(size.height / 2)
                        EchoColors.indices.reversed().forEach { index ->
                            val offset = step * (index + 1)
                            drawRoundRect(EchoColors[index], Offset(offset, offset), size, corner)
                        }
                    }
                }.pointerHoverIcon(PointerIcon.Hand),
        interactionSource = interactionSource,
        elevation = null,
        colors = ButtonDefaults.buttonColors(backgroundColor = white, contentColor = void),
        contentPadding = ButtonContentPadding,
    ) {
        // The theme's text styles carry their own color, which would override the button's content color.
        Text(text = text, style = MaterialTheme.typography.button, color = void)
        if (trailingIcon != null) {
            Spacer(Modifier.width(Dimens.small))
            Icon(
                imageVector = trailingIcon,
                contentDescription = trailingIconDescription,
                tint = void,
                modifier = Modifier.size(TrailingIconSize),
            )
        }
    }
}

/**
 * A secondary action: a quiet pill on the raised neutral, for actions that sit beside content or repeat in a list,
 * such as store links and Retry. [content] is the button's row, usually a label and sometimes an icon.
 */
@Composable
fun TonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight).pointerHoverIcon(PointerIcon.Hand),
        elevation = null,
        colors = ButtonDefaults.buttonColors(backgroundColor = secondaryVariant, contentColor = white),
        contentPadding = ButtonContentPadding,
        content = content,
    )
}

/**
 * A link to another part of the app: an outlined pill with a trailing arrow, so it reads as navigation and neither as
 * the main action nor as a [Tag]. It is compact, for use inside cards.
 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = CornerShapes.pill,
        border = BorderStroke(Dimens.one, white.copy(alpha = OUTLINE_ALPHA)),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, contentColor = white),
        contentPadding = PaddingValues(horizontal = Dimens.compact, vertical = Dimens.small),
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
    ) {
        Text(text = text, color = white)
        Spacer(Modifier.width(Dimens.small))
        Icon(
            imageVector = ChevronLeft,
            contentDescription = null,
            modifier = Modifier.size(OutlineArrowSize).rotate(OUTLINE_ARROW_ROTATION),
        )
    }
}

@Preview
@Composable
internal fun ButtonsPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(void).padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.medium),
        ) {
            PrimaryButton(text = "Explore games", onClick = {})
            PrimaryButton(text = "Get giveaway", onClick = {}, trailingIcon = OpenInNew)
            TonalButton(onClick = {}) { Text(text = "Retry", style = MaterialTheme.typography.button) }
            OutlineButton(text = "Open Releases", onClick = {})
        }
    }
}
