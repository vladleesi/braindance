// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import Rss
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.navigation_news
import dev.vladleesi.braindanceapp.resources.news_category_for_you
import dev.vladleesi.braindanceapp.resources.news_category_latest
import dev.vladleesi.braindanceapp.resources.news_category_saved
import dev.vladleesi.braindanceapp.resources.news_feed_description
import dev.vladleesi.braindanceapp.resources.news_feed_title
import dev.vladleesi.braindanceapp.resources.news_sources_coming_soon
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.PlaceholderChips
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.style.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun NewsScreen(modifier: Modifier) {
    val navigator = LocalNavigator.current
    Column(modifier = modifier) {
        SpacerStatusBarInsets()
        TopLevelTopAppBar(
            title = stringResource(Res.string.navigation_news),
            onProfileClick = { navigator.navigate(ProfileRoute) },
            actions = {
                ComingSoonAction(imageVector = Rss, label = stringResource(Res.string.news_sources_coming_soon))
            },
        )
        Column(
            modifier = Modifier.padding(horizontal = Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.medium),
        ) {
            PlaceholderChips(
                labels =
                    listOf(
                        stringResource(Res.string.news_category_for_you),
                        stringResource(Res.string.news_category_latest),
                        stringResource(Res.string.news_category_saved),
                    ),
            )
            ComingSoonCard(
                title = stringResource(Res.string.news_feed_title),
                description = stringResource(Res.string.news_feed_description),
            )
        }
    }
}
