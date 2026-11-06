// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import CalendarDays
import Compass
import House
import Library
import Newspaper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.navigation_discover
import dev.vladleesi.braindanceapp.resources.navigation_home
import dev.vladleesi.braindanceapp.resources.navigation_library
import dev.vladleesi.braindanceapp.resources.navigation_news
import dev.vladleesi.braindanceapp.resources.navigation_releases
import dev.vladleesi.braindanceapp.ui.screens.DiscoverScreen
import dev.vladleesi.braindanceapp.ui.screens.HomeScreen
import dev.vladleesi.braindanceapp.ui.screens.LibraryScreen
import dev.vladleesi.braindanceapp.ui.screens.NewsScreen
import dev.vladleesi.braindanceapp.ui.screens.ReleasesScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Serializable
sealed interface BottomBarRoute : Route {
    @Transient
    val imageVector: ImageVector

    @Transient
    val label: StringResource

    @Serializable
    object HomeRoute : BottomBarRoute {
        override val label: StringResource
            get() = Res.string.navigation_home

        override val imageVector: ImageVector
            get() = House

        @Composable
        override fun Content(
            modifier: Modifier,
            paddingValues: PaddingValues,
        ) {
            HomeScreen(modifier = Modifier.padding(paddingValues))
        }
    }

    @Serializable
    object DiscoverRoute : BottomBarRoute {
        const val RESULT_KEY = "search_result_key"

        override val label: StringResource
            get() = Res.string.navigation_discover

        override val imageVector: ImageVector
            get() = Compass

        @Composable
        override fun Content(
            modifier: Modifier,
            paddingValues: PaddingValues,
        ) {
            DiscoverScreen(modifier.padding(paddingValues))
        }
    }

    @Serializable
    object ReleasesRoute : BottomBarRoute {
        override val label: StringResource
            get() = Res.string.navigation_releases

        override val imageVector: ImageVector
            get() = CalendarDays

        @Composable
        override fun Content(
            modifier: Modifier,
            paddingValues: PaddingValues,
        ) {
            ReleasesScreen(modifier = modifier.padding(paddingValues))
        }
    }

    @Serializable
    object NewsRoute : BottomBarRoute {
        override val label: StringResource
            get() = Res.string.navigation_news

        override val imageVector: ImageVector
            get() = Newspaper

        @Composable
        override fun Content(
            modifier: Modifier,
            paddingValues: PaddingValues,
        ) {
            NewsScreen(modifier = modifier.padding(paddingValues))
        }
    }

    @Serializable
    object LibraryRoute : BottomBarRoute {
        override val label: StringResource
            get() = Res.string.navigation_library

        override val imageVector: ImageVector
            get() = Library

        @Composable
        override fun Content(
            modifier: Modifier,
            paddingValues: PaddingValues,
        ) {
            LibraryScreen(modifier = modifier.padding(paddingValues))
        }
    }

    companion object {
        val routes = listOf<BottomBarRoute>(HomeRoute, DiscoverRoute, ReleasesRoute, NewsRoute, LibraryRoute)
    }
}
