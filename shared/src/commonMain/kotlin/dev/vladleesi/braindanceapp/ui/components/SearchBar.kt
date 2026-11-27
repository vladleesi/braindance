// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import Search
import X
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.navigation.routeLink
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.discover_search_placeholder
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.clickable as foundationClickable

// Compact next to Material's 56 dp text field, and still the 44 dp touch height of iOS controls. The icon slots span
// the field's height, so their touch targets are as tall as the field.
private val SearchFieldHeight = 44.dp
private val SearchIconSlotWidth = 44.dp
private val SearchIconSize = 20.dp
private val SearchProgressSize = 18.dp

// Wide enough to read as a field, not a pill-shaped button.

/**
 * A compact pill-shaped search field. The caller owns [query], so it can clear, restore, or act on it.
 *
 * [leadingIcon] defaults to a decorative search icon. Slots take a [SearchFieldIcon], which is a button when it has
 * a click action, or a [SearchFieldProgress]; an empty [trailingIcon] leaves room for the text instead.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable () -> Unit = { SearchFieldIcon(imageVector = Search) },
    trailingIcon: @Composable () -> Unit = {},
    fieldHeight: Dp = SearchFieldHeight,
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().height(fieldHeight),
        singleLine = true,
        textStyle = MaterialTheme.typography.body2.copy(color = foreground),
        cursorBrush = SolidColor(accent),
        // Game titles are names, so autocorrect would only rewrite them.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(secondary, CornerShapes.pill)
                        .focusRing(visible = interactionSource.isFocusVisible(), shape = CornerShapes.pill),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon()
                Box(
                    modifier = Modifier.weight(1f).padding(end = Dimens.compact),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.body2,
                            color = secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                trailingIcon()
            }
        },
    )
}

/**
 * An icon in a [SearchBar] slot. With [onClick] it is a button that [label] names; without it, it is decorative.
 */
@Composable
fun SearchFieldIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val action =
        if (onClick != null) {
            Modifier
                .clip(CircleShape)
                .pointerHoverIcon(PointerIcon.Hand)
                .foundationClickable(role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }
    Box(
        modifier = modifier.fillMaxHeight().width(SearchIconSlotWidth).then(action),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = label,
            modifier = Modifier.size(SearchIconSize),
            tint = if (onClick != null) foreground else secondaryText,
        )
    }
}

/**
 * The search button of a top bar. It always searches games, wherever it appears, so it means the same thing on every
 * screen. Pass [Navigator.openSearch][dev.vladleesi.braindanceapp.navigation.Navigator.openSearch] as [onClick]. The
 * desktop layout leaves it out: its header's search field is on every screen.
 */
@Composable
fun SearchAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (desktopLayout() != null) return
    // Search lives on Discover, so the action links to it.
    IconButton(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand).routeLink(BottomBarRoute.DiscoverRoute),
    ) {
        Icon(
            imageVector = Search,
            contentDescription = stringResource(Res.string.discover_search_placeholder),
            tint = foreground,
        )
    }
}

/** The [SearchBar] trailing indicator while a search runs. */
@Composable
fun SearchFieldProgress(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxHeight().width(SearchIconSlotWidth),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(SearchProgressSize),
            color = foreground,
            strokeWidth = Dimens.micro,
        )
    }
}

@Preview
@Composable
internal fun SearchBarPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium),
        ) {
            SearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                placeholder = "Search games",
            )
            SearchBar(
                query = "Hollow Knight",
                onQueryChange = {},
                onSearch = {},
                placeholder = "Search games",
                modifier = Modifier.padding(top = Dimens.medium),
                trailingIcon = { SearchFieldIcon(imageVector = X, label = "Clear search", onClick = {}) },
            )
            SearchAction(onClick = {})
        }
    }
}
