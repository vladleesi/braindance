// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.expandable_text_read_more
import dev.vladleesi.braindanceapp.resources.expandable_text_show_less
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.textPlaceholder
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

const val DEFAULT_MINIMUM_TEXT_LINE = 4
private const val COLLAPSE_DURATION_MS = 180

private enum class ExpandableTextSlot {
    Paragraph,
    Disclosure,
}

/**
 * [text] cut to [collapsedMaxLines] with a control that shows the rest. While [placeholder], each line, and the
 * control, is a placeholder. [onCollapse] returns whether the scroll container could offset the removed text height.
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = DEFAULT_MINIMUM_TEXT_LINE,
    expandText: String = stringResource(Res.string.expandable_text_read_more),
    collapseText: String = stringResource(Res.string.expandable_text_show_less),
    textStyle: TextStyle = MaterialTheme.typography.body2,
    textColor: Color = white,
    expandTextStyle: TextStyle = MaterialTheme.typography.caption,
    expandTextColor: Color = white,
    placeholder: Boolean = false,
    onCollapse: (suspend (Int) -> Boolean)? = null,
) {
    var isExpanded by rememberSaveable(text, collapsedMaxLines) { mutableStateOf(false) }
    var animateCollapse by remember(text, collapsedMaxLines) { mutableStateOf(false) }
    var textLayout by remember(text, collapsedMaxLines) { mutableStateOf<TextLayoutResult?>(null) }
    val measurer = if (onCollapse != null) rememberTextMeasurer() else null
    val scope = rememberCoroutineScope()
    val sizeAnimation =
        if (onCollapse != null && (isExpanded || animateCollapse)) {
            Modifier.animateContentSize(
                animationSpec = if (animateCollapse) tween(COLLAPSE_DURATION_MS) else snap(),
                finishedListener = { _, _ -> animateCollapse = false },
            )
        } else {
            Modifier
        }

    SubcomposeLayout(modifier) { constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val paragraph =
            subcompose(ExpandableTextSlot.Paragraph) {
                Text(
                    text = text,
                    style = textStyle,
                    color = textColor,
                    maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    onTextLayout = { textLayout = it },
                    modifier = sizeAnimation.textPlaceholder(visible = placeholder) { textLayout },
                )
            }.single().measure(childConstraints)
        // Measure the button in the same pass as the text so content below it doesn't shift a frame later.
        val disclosure =
            if (isExpanded || textLayout?.hasVisualOverflow == true) {
                subcompose(ExpandableTextSlot.Disclosure) {
                    DisclosureButton(
                        label = if (isExpanded) collapseText else expandText,
                        expanded = isExpanded,
                        modifier = Modifier.padding(top = Dimens.small),
                        placeholder = placeholder,
                        style = expandTextStyle,
                        color = expandTextColor,
                        onClick = {
                            val removedHeight =
                                if (isExpanded && measurer != null) {
                                    textLayout?.heightRemovedByCollapse(measurer, collapsedMaxLines)
                                } else {
                                    null
                                }
                            if (removedHeight != null && onCollapse != null) {
                                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    animateCollapse = !onCollapse(removedHeight)
                                    isExpanded = false
                                }
                            } else {
                                animateCollapse = false
                                isExpanded = !isExpanded
                            }
                        },
                    )
                }.single().measure(childConstraints.offset(vertical = -paragraph.height))
            } else {
                null
            }
        val width = constraints.constrainWidth(maxOf(paragraph.width, disclosure?.width ?: 0))
        val height = constraints.constrainHeight(paragraph.height + (disclosure?.height ?: 0))
        layout(width, height) {
            paragraph.placeRelative(0, 0)
            disclosure?.placeRelative(0, paragraph.height)
        }
    }
}

private fun TextLayoutResult.heightRemovedByCollapse(
    measurer: TextMeasurer,
    maxLines: Int,
): Int {
    val input = layoutInput
    val collapsed =
        measurer.measure(
            text = input.text,
            style = input.style,
            overflow = TextOverflow.Ellipsis,
            softWrap = input.softWrap,
            maxLines = maxLines,
            constraints = input.constraints,
            layoutDirection = input.layoutDirection,
            density = input.density,
            fontFamilyResolver = input.fontFamilyResolver,
        )
    return (size.height - collapsed.size.height).coerceAtLeast(0)
}

@Preview
@Composable
internal fun ExpandableTextPreview() {
    BraindanceTheme {
        ExpandableText(
            text =
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Integer non lorem nec sapien cursus faucibus. Suspendisse potenti. " +
                    "Praesent euismod, massa non dignissim viverra, turpis justo tincidunt nisi, " +
                    "sed fermentum purus lorem at mauris. " +
                    "Curabitur id arcu vel sapien tristique interdum. Nullam in tortor sed urna placerat blandit. " +
                    "Maecenas vel lorem at odio consequat convallis. " +
                    "Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas.",
        )
    }
}
