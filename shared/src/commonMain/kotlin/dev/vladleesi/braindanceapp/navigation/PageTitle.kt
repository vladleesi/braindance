// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavKey

/**
 * Names the screen of [route], such as a game's name once it has loaded, in the browser tab and its history entries.
 * Pass null while the title is unknown. Only the Web has a page title. Elsewhere this does nothing.
 */
@Composable
fun PageTitle(
    route: NavKey,
    title: String?,
) {
    val location = browserLocation ?: return
    LaunchedEffect(route, title) { location.setTitle(route.toWebPath(), title) }
}
