// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.ui.screens.ProfileScreen
import kotlinx.serialization.Serializable

/** Opened from the profile button on top-level screens, on top of the current tab. */
@Serializable
object ProfileRoute : SingleInstanceRoute {
    @Composable
    override fun Content(
        modifier: Modifier,
        paddingValues: PaddingValues,
    ) {
        val navigator = LocalNavigator.current
        ProfileScreen(
            onBackButtonPressed = { navigator.goBack() },
            modifier = modifier.padding(paddingValues),
        )
    }
}
