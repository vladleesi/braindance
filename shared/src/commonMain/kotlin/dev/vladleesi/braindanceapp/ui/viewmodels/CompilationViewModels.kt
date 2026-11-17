// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.map
import dev.vladleesi.braindanceapp.data.repository.FeedPage
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayItemModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel

// The full lists behind Home's See All actions. Each maps its source's pages to the cards Home shows for it.

/** Popular Right Now, with the reason each game is trending. */
class PopularGamesViewModel(
    private val homeRepo: HomeRepo,
) : PagedListViewModel<GameCardModel>(itemId = GameCardModel::id) {
    override suspend fun loadPage(page: Int): Outcome<FeedPage<GameCardModel>> =
        homeRepo.popularPage(page).map { feed -> FeedPage(feed.items.map { it.toGameCardModel() }, feed.isLast) }
}

/** Most Anticipated. */
class AnticipatedGamesViewModel(
    private val homeRepo: HomeRepo,
) : PagedListViewModel<GameCardModel>(itemId = GameCardModel::id) {
    override suspend fun loadPage(page: Int): Outcome<FeedPage<GameCardModel>> =
        homeRepo.anticipatedPage(page).map { feed ->
            FeedPage(feed.items.mapNotNull { it.toGameCardModel() }, feed.isLast)
        }
}

/** Every current giveaway; the source has a single page. */
class GiveawaysListViewModel(
    private val gamerPowerRepo: GamerPowerRepo,
) : PagedListViewModel<GiveawayItemModel>(itemId = GiveawayItemModel::id) {
    override suspend fun loadPage(page: Int): Outcome<FeedPage<GiveawayItemModel>> =
        gamerPowerRepo.allGiveaways().map { giveaways ->
            FeedPage(giveaways.mapNotNull { it.toGiveawayItemModel() }, isLast = true)
        }
}
