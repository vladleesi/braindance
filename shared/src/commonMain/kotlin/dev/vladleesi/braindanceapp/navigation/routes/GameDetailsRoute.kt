// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.ui.screens.GameDetailsScreen
import kotlinx.serialization.Serializable

/**
 * A game's page showing [id], the game or one of its versions. [page] is the ID the page was opened with; switching
 * versions keeps it, so the screen stays one screen: no transition, the same scroll position and ViewModel. Only [id]
 * is part of the address.
 */
@Serializable
data class GameDetailsRoute(
    val id: Int,
    val page: Int = id,
) : Route {
    override val contentKey: String get() = "GameDetailsRoute(page=$page)"

    /** The same page showing version [versionId] instead. */
    fun withVersion(versionId: Int): GameDetailsRoute = GameDetailsRoute(id = versionId, page = page)

    @Composable
    override fun Content(
        modifier: Modifier,
        paddingValues: PaddingValues,
    ) {
        GameDetailsScreen(route = this, modifier = modifier.padding(paddingValues))
    }
}
