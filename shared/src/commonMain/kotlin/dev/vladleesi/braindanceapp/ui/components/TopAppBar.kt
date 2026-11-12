// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import ChevronLeft
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.back_button_content_description
import dev.vladleesi.braindanceapp.resources.profile_screen_title
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.withCircleRippleEffect
import org.jetbrains.compose.resources.stringResource

// Without actions, the title keeps the same end margin as its start margin beside the back button.
private val titleEndPaddingWithoutActions = 64.dp

// An icon button's 48 dp target insets its 24 dp icon by 12 dp; this lines the last icon up with the 16 dp margin.
private val actionsEndPadding = Dimens.tiny

@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    title: String = "",
    subtitle: String = "",
    titleColor: Color = white,
    titleStyle: TextStyle = MaterialTheme.typography.body2,
    tabBarColor: Color = navBar,
    backButtonIcon: ImageVector = ChevronLeft,
    showBackButton: Boolean = false,
    backButtonBackgroundColor: Color = navBar,
    backButtonColor: Color = white,
    onBackButtonPressed: () -> Unit = {},
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Dimens.topBarHeight)
                .background(tabBarColor)
                .padding(start = Dimens.medium, end = if (actions == null) Dimens.medium else actionsEndPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            IconButton(
                modifier = Modifier.withCircleRippleEffect(backButtonBackgroundColor),
                onClick = (onBackButtonPressed),
            ) {
                Icon(
                    modifier = Modifier.size(26.dp),
                    imageVector = backButtonIcon,
                    tint = backButtonColor,
                    contentDescription = stringResource(Res.string.back_button_content_description),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = if (showBackButton) Dimens.medium else 0.dp,
                        end = if (actions == null) titleEndPaddingWithoutActions else Dimens.small,
                    ),
        ) {
            if (title.isNotEmpty()) {
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = title,
                    style = titleStyle,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.caption,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        actions?.invoke(this)
    }
}

/**
 * The bar of a top-level screen: its [title], the screen's own [actions], and the profile button, which calls
 * [onProfileClick]. The button is left out while the desktop sidebar is shown, because the sidebar already links to
 * Profile; [showProfileButton] overrides that. Nested screens use [TopAppBar] with a back button instead.
 */
@Composable
fun TopLevelTopAppBar(
    title: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProfileButton: Boolean = desktopLayout()?.hasSidebar != true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = title,
        titleStyle = MaterialTheme.typography.h2,
        tabBarColor = Color.Transparent,
        actions = {
            actions()
            if (showProfileButton) {
                IconButton(onClick = onProfileClick) {
                    UserAvatar(contentDescription = stringResource(Res.string.profile_screen_title))
                }
            }
        },
    )
}

@Preview
@Composable
internal fun TopAppBarPreview() {
    BraindanceTheme {
        TopAppBar(
            title = "Title",
            subtitle = "Subtitle",
            showBackButton = true,
            onBackButtonPressed = {},
        )
    }
}

@Preview
@Composable
internal fun TopLevelTopAppBarPreview() {
    BraindanceTheme {
        TopLevelTopAppBar(title = "Library", onProfileClick = {})
    }
}
