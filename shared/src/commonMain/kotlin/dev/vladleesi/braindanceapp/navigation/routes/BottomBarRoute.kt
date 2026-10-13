// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import Album
import CircleUserRound
import House
import Newspaper
import Search
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vladleesi.braindanceapp.ui.screens.CollectionsScreen
import dev.vladleesi.braindanceapp.ui.screens.HomeScreen
import dev.vladleesi.braindanceapp.ui.screens.NewsScreen
import dev.vladleesi.braindanceapp.ui.screens.ProfileScreen
import dev.vladleesi.braindanceapp.ui.screens.SearchScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface BottomBarRoute : Route {
    @Transient
    val imageVector: ImageVector

    @Serializable
    object HomeRoute : BottomBarRoute {
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
    object NewsRoute : BottomBarRoute {
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
    object SearchRoute : BottomBarRoute {
        const val RESULT_KEY = "search_result_key"

        override val imageVector: ImageVector
            get() = Search

        @Composable
        override fun Content(
            modifier: Modifier,
            paddingValues: PaddingValues,
        ) {
            SearchScreen(modifier.padding(paddingValues))
        }
    }

    @Serializable
    object CollectionsRoute : BottomBarRoute {
        override val imageVector: ImageVector
            get() = Album

        @Composable
        override fun Content(
            modifier: Modifier,
            paddingValues: PaddingValues,
        ) {
            CollectionsScreen(modifier = modifier.padding(paddingValues))
        }
    }

    @Serializable
    object ProfileRoute : BottomBarRoute {
        override val imageVector: ImageVector
            get() = CircleUserRound

        @Composable
        override fun Content(
            modifier: Modifier,
            paddingValues: PaddingValues,
        ) {
            ProfileScreen(modifier = modifier.padding(paddingValues))
        }
    }

    companion object {
        val routes = listOf<BottomBarRoute>(HomeRoute, NewsRoute, SearchRoute, CollectionsRoute, ProfileRoute)
    }
}
