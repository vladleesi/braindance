// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import ChevronLeft
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.navigation.routeLink
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.back_button_content_description
import dev.vladleesi.braindanceapp.resources.profile_screen_title
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.utils.withCircleRippleEffect
import org.jetbrains.compose.resources.stringResource

// Without actions, the title keeps the same end margin as its start margin beside the back button.
private val titleEndPaddingWithoutActions = 64.dp

// An icon button's 48 dp target insets its 24 dp icon by 12 dp; the end padding leaves that out of the page gutter, so
// the last icon lines up with the content's edge.
private val iconButtonInset = Dimens.compact

@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    title: String = "",
    subtitle: String = "",
    titleColor: Color = foreground,
    titleStyle: TextStyle = MaterialTheme.typography.body2,
    // The canvas every screen shows. A screen whose bar covers scrolled content passes its own color.
    tabBarColor: Color = background,
    backButtonIcon: ImageVector = ChevronLeft,
    showBackButton: Boolean = false,
    backButtonBackgroundColor: Color = navBar,
    backButtonColor: Color = foreground,
    onBackButtonPressed: () -> Unit = {},
    actions: (@Composable RowScope.() -> Unit)? = null,
    maxContentWidth: Dp = Dimens.pageMaxWidth,
    height: Dp = if (showBackButton) Dimens.backTopBarHeight else Dimens.topBarHeight,
) {
    val gutter = pageGutter()
    // The color spans the whole width; the back button, title, and actions line up with the page column below, which
    // has the same [maxContentWidth].
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .background(tabBarColor)
                .pageWidth(gutter, maxContentWidth)
                .padding(start = gutter, end = if (actions == null) gutter else gutter - iconButtonInset),
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
 * [onProfileClick]. The button is left out in the desktop layout, whose header has it. [showProfileButton] overrides
 * that. Nested screens use [TopAppBar] with a back button instead.
 */
@Composable
fun TopLevelTopAppBar(
    title: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProfileButton: Boolean = desktopLayout() == null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = title,
        titleStyle = MaterialTheme.typography.h2,
        tabBarColor = Color.Transparent,
        actions = {
            actions()
            if (showProfileButton) ProfileButton(onClick = onProfileClick)
        },
    )
}

/**
 * The account button: the user's avatar, named "Profile", which opens Profile with [onClick]. While [selected], as
 * when Profile is open, a ring in the accent marks it. The ring's space is kept either way, so the avatar never moves.
 */
@Composable
fun ProfileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .routeLink(ProfileRoute)
                .semantics { this.selected = selected },
    ) {
        UserAvatar(
            contentDescription = stringResource(Res.string.profile_screen_title),
            modifier =
                Modifier
                    .border(Dimens.micro, if (selected) accent else Color.Transparent, CircleShape)
                    .padding(Dimens.micro),
        )
    }
}

/**
 * A secondary screen's loading or error state under a top bar with the back button alone, so a slow or failed load
 * never leaves the user without a way back. The state fills the rest of the screen through the modifier it gets.
 */
@Composable
fun BackOnlyFrame(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SpacerStatusBarInsets()
        // As tall as the loaded page's bar, so Back does not move when the page arrives.
        TopAppBar(showBackButton = true, onBackButtonPressed = onBack, height = Dimens.heroTopBarHeight)
        content(Modifier.weight(1f))
    }
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

@Preview
@Composable
internal fun ProfileButtonPreview() {
    BraindanceTheme {
        Row {
            ProfileButton(onClick = {})
            ProfileButton(onClick = {}, selected = true)
        }
    }
}

@Preview
@Composable
internal fun BackOnlyFramePreview() {
    BraindanceTheme {
        BackOnlyFrame(onBack = {}) { modifier ->
            Text(text = "Something went wrong", modifier = modifier.padding(Dimens.medium))
        }
    }
}
