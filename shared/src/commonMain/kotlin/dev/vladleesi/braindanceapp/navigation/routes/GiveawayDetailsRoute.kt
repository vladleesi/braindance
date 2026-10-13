// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.ui.screens.GiveawayDetailsScreen
import kotlinx.serialization.Serializable

@Serializable
data class GiveawayDetailsRoute(
    val id: Int,
) : Route {
    @Composable
    override fun Content(
        modifier: Modifier,
        paddingValues: PaddingValues,
    ) {
        GiveawayDetailsScreen(id = id, modifier = modifier.padding(paddingValues))
    }
}
