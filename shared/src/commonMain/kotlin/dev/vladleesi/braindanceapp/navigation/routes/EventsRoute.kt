// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.ui.screens.EventsScreen
import kotlinx.serialization.Serializable

@Serializable
data class EventsRoute(
    val id: Int? = null,
) : Route {
    @Composable
    override fun Content(
        modifier: Modifier,
        paddingValues: PaddingValues,
    ) {
        EventsScreen(route = this, modifier = modifier.padding(paddingValues))
    }
}
