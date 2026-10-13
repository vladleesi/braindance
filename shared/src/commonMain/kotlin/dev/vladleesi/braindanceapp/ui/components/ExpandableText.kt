// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white

const val DEFAULT_MINIMUM_TEXT_LINE = 4

@Composable
fun ExpandableText(
    text: String,
    collapsedMaxLines: Int = DEFAULT_MINIMUM_TEXT_LINE,
    expandText: String = "Read more",
    collapseText: String = "Show less",
    textStyle: TextStyle = MaterialTheme.typography.body2,
    textColor: Color = white,
    expandTextStyle: TextStyle = MaterialTheme.typography.body2,
    expandTextColor: Color = secondaryText,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isTextOverflowing by rememberSaveable { mutableStateOf(false) }

    Column(modifier) {
        Text(
            text = text,
            style = textStyle,
            color = textColor,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                isTextOverflowing = textLayoutResult.hasVisualOverflow || isExpanded
            },
        )

        // Show "Show more" only if the text overflows
        if (isTextOverflowing) {
            Text(
                text = if (isExpanded) collapseText else expandText,
                style = expandTextStyle,
                color = expandTextColor,
                modifier =
                    Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = Dimens.tiny),
            )
        }
    }
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
