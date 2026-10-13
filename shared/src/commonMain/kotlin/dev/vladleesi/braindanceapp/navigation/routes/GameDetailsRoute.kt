// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.ui.screens.GameDetailsScreen
import kotlinx.serialization.Serializable

@Serializable
data class GameDetailsRoute(
    val id: Int,
) : Route {
    @Composable
    override fun Content(
        modifier: Modifier,
        paddingValues: PaddingValues,
    ) {
        GameDetailsScreen(id = id, modifier = modifier.padding(paddingValues))
    }
}
