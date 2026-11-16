// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import Check
import Heart
import HeartFilled
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_details_add_to_collection
import dev.vladleesi.braindanceapp.resources.game_details_add_to_collection_coming_soon
import dev.vladleesi.braindanceapp.resources.game_details_in_collection
import dev.vladleesi.braindanceapp.resources.game_details_on_wishlist
import dev.vladleesi.braindanceapp.resources.game_details_wishlist
import dev.vladleesi.braindanceapp.resources.game_details_wishlist_coming_soon
import dev.vladleesi.braindanceapp.ui.components.ButtonProgress
import dev.vladleesi.braindanceapp.ui.components.ComingSoonLabel
import dev.vladleesi.braindanceapp.ui.components.PrimaryButton
import dev.vladleesi.braindanceapp.ui.components.TonalButton
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.stringResource

// The same height as the text buttons, so the row aligns; a circle when the toggle shows only its icon.
private val ToggleSize = 48.dp
private val ToggleIconSize = 20.dp
private val CheckIconSize = 18.dp
private val ToggleLabelPadding = 20.dp

// Material's disabled content emphasis (ContentAlpha.disabled, which can only be read in composition).
private const val DISABLED_CONTENT_ALPHA = 0.38f
private val DisabledContentColor = white.copy(alpha = DISABLED_CONTENT_ALPHA)

/**
 * Where a game sits in the user's library. A game is in the collection or on the wishlist, never both, so the two
 * actions cannot contradict each other.
 */
enum class LibraryStatus {
    None,
    Collection,
    Wishlist,
}

/** The two library actions of the game details hero. */
enum class LibraryAction {
    Collection,
    Wishlist,
}

/**
 * The status a press of [action] leads to. Pressing the active action removes the game again. Adding a wishlisted
 * game to the collection takes it off the wishlist; a game in the collection cannot be wishlisted, so the wishlist
 * press leaves it there.
 */
fun LibraryStatus.after(action: LibraryAction): LibraryStatus =
    when (action) {
        LibraryAction.Collection ->
            if (this ==
                LibraryStatus.Collection
            ) {
                LibraryStatus.None
            } else {
                LibraryStatus.Collection
            }
        LibraryAction.Wishlist ->
            when (this) {
                LibraryStatus.None -> LibraryStatus.Wishlist
                LibraryStatus.Wishlist -> LibraryStatus.None
                LibraryStatus.Collection -> LibraryStatus.Collection
            }
    }

/** What the library actions show; the screen that owns the user's library provides it. */
sealed interface LibraryActionsState {
    /** Accounts and collections do not exist yet: both actions are shown disabled and marked "Coming soon". */
    data object Unavailable : LibraryActionsState

    /**
     * The game's [status]. While [saving] is set, that action's change is underway: [status] already shows the
     * result, so the press has immediate feedback, and both actions wait until the change completes.
     */
    data class Ready(
        val status: LibraryStatus,
        val saving: LibraryAction? = null,
    ) : LibraryActionsState
}

/**
 * Add to collection, the hero's main action, and the wishlist toggle beside it. With [fillWidth], for compact
 * layouts, the collection button takes the row's free width and the toggle shows only its heart; otherwise both keep
 * their natural width and the toggle is labelled. [onStatusChange] receives the status the user chose.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryActions(
    state: LibraryActionsState,
    fillWidth: Boolean,
    onStatusChange: (LibraryStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state as? LibraryActionsState.Ready
    val status = ready?.status ?: LibraryStatus.None
    val comingSoon = ready == null
    val collection: @Composable (Modifier) -> Unit = { buttonModifier ->
        val comingSoonLabel = stringResource(Res.string.game_details_add_to_collection_coming_soon)
        CollectionButton(
            inCollection = status == LibraryStatus.Collection,
            actionState = ready.actionState(LibraryAction.Collection),
            onClick = { onStatusChange(status.after(LibraryAction.Collection)) },
            modifier =
                if (comingSoon) buttonModifier.semantics { contentDescription = comingSoonLabel } else buttonModifier,
        )
    }
    val wishlist: @Composable () -> Unit = {
        // The switch semantics announce on and off, so screen readers hear only the action's name.
        WishlistToggle(
            onWishlist = status == LibraryStatus.Wishlist,
            actionState = ready.actionState(LibraryAction.Wishlist),
            description =
                stringResource(
                    if (comingSoon) Res.string.game_details_wishlist_coming_soon else Res.string.game_details_wishlist,
                ),
            showLabel = !fillWidth,
            onToggle = { onStatusChange(status.after(LibraryAction.Wishlist)) },
        )
    }
    if (fillWidth) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.small)) {
                collection(Modifier.weight(1f))
                wishlist()
            }
            if (comingSoon) ComingSoonLabel()
        }
    } else {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
            verticalArrangement = Arrangement.spacedBy(Dimens.small),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            collection(Modifier)
            wishlist()
            if (comingSoon) ComingSoonLabel()
        }
    }
}

/** Whether one action can be pressed now, is saving, or waits. */
private enum class ActionState {
    Enabled,
    Saving,
    Disabled,
}

/**
 * [action]'s state: disabled while accounts are unavailable (a null state) and while the other action saves; the
 * wishlist is also disabled for a game in the collection, which cannot be wishlisted.
 */
private fun LibraryActionsState.Ready?.actionState(action: LibraryAction): ActionState =
    when {
        this == null -> ActionState.Disabled
        saving == action -> ActionState.Saving
        saving != null -> ActionState.Disabled
        action == LibraryAction.Wishlist && status == LibraryStatus.Collection -> ActionState.Disabled
        else -> ActionState.Enabled
    }

/**
 * The white primary pill while the game is outside the collection; once it is in, a quiet tonal pill with a cyan
 * check that removes it again, so the selected state never competes with the next action.
 */
@Composable
private fun CollectionButton(
    inCollection: Boolean,
    actionState: ActionState,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val saving = actionState == ActionState.Saving
    if (!inCollection) {
        PrimaryButton(
            text = stringResource(Res.string.game_details_add_to_collection),
            onClick = onClick,
            enabled = actionState != ActionState.Disabled,
            loading = saving,
            modifier = modifier,
        )
        return
    }
    val enabled = actionState == ActionState.Enabled
    TonalButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        if (saving) {
            ButtonProgress(color = white)
        } else {
            Icon(imageVector = Check, contentDescription = null, tint = accent, modifier = Modifier.size(CheckIconSize))
        }
        Spacer(Modifier.width(Dimens.small))
        Text(
            text = stringResource(Res.string.game_details_in_collection),
            style = MaterialTheme.typography.button,
            color = if (enabled || saving) white else DisabledContentColor,
        )
    }
}

/**
 * Adds the game to the wishlist or takes it off: an outlined heart that fills cyan when the game is on the wishlist.
 * With [showLabel] it is a pill that names the state; otherwise a circle with only the heart, named for screen readers.
 */
@Composable
private fun WishlistToggle(
    onWishlist: Boolean,
    actionState: ActionState,
    description: String,
    showLabel: Boolean,
    onToggle: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape: Shape = if (showLabel) CornerShapes.pill else CircleShape
    val enabled = actionState == ActionState.Enabled
    val saving = actionState == ActionState.Saving
    // A saving toggle keeps full emphasis: its change is underway, not unavailable.
    val contentColor = if (actionState == ActionState.Disabled) DisabledContentColor else white
    Row(
        modifier =
            Modifier
                .heightIn(min = ToggleSize)
                .then(if (showLabel) Modifier else Modifier.size(ToggleSize))
                .clip(shape)
                .background(secondaryVariant)
                .focusRing(interactionSource.isFocusVisible(), shape, Dimens.micro)
                .toggleable(
                    value = onWishlist,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onValueChange = { onToggle() },
                ).pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .semantics { contentDescription = description }
                .padding(horizontal = if (showLabel) ToggleLabelPadding else 0.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(ToggleIconSize), contentAlignment = Alignment.Center) {
            if (saving) {
                ButtonProgress(color = white)
            } else {
                Icon(
                    imageVector = if (onWishlist) HeartFilled else Heart,
                    contentDescription = null,
                    tint = if (onWishlist) accent else contentColor,
                    modifier = Modifier.size(ToggleIconSize),
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.width(Dimens.small))
            Text(
                text =
                    stringResource(
                        if (onWishlist) Res.string.game_details_on_wishlist else Res.string.game_details_wishlist,
                    ),
                style = MaterialTheme.typography.button,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
internal fun LibraryActionsPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.large),
        ) {
            val states =
                listOf(
                    LibraryActionsState.Unavailable,
                    LibraryActionsState.Ready(LibraryStatus.None),
                    LibraryActionsState.Ready(LibraryStatus.Wishlist),
                    LibraryActionsState.Ready(LibraryStatus.Collection),
                    LibraryActionsState.Ready(LibraryStatus.Collection, saving = LibraryAction.Collection),
                    LibraryActionsState.Ready(LibraryStatus.Wishlist, saving = LibraryAction.Wishlist),
                )
            states.forEach { state ->
                LibraryActions(state = state, fillWidth = true, onStatusChange = {})
                LibraryActions(state = state, fillWidth = false, onStatusChange = {})
            }
        }
    }
}
