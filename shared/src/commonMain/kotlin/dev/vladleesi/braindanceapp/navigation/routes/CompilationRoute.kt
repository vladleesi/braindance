// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.ui.screens.CompilationScreen
import kotlinx.serialization.Serializable

/** A Home section that has a full list behind its See All action. */
@Serializable
enum class Compilation {
    /** Popular Right Now. */
    Popular,

    /** Most Anticipated. */
    Anticipated,

    /** Latest Giveaways. */
    Giveaways,
}

/** The whole list of a [compilation], page by page. */
@Serializable
data class CompilationRoute(
    val compilation: Compilation,
) : Route {
    @Composable
    override fun Content(
        modifier: Modifier,
        paddingValues: PaddingValues,
    ) {
        CompilationScreen(compilation = compilation, modifier = modifier.padding(paddingValues))
    }
}
