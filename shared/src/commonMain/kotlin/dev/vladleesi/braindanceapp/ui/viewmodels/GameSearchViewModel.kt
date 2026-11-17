// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.repository.GameSearchRepo
import dev.vladleesi.braindanceapp.data.repository.SearchQuery
import dev.vladleesi.braindanceapp.ui.components.home.GameCardLabel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.utils.utcYearOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** What Discover shows below the search field. Every state but [Idle] belongs to one normalized query. */
sealed interface SearchState {
    /** No search: Discover shows its own content. */
    data object Idle : SearchState

    /** The first results are loading, with nothing to show yet. */
    data class Loading(
        val query: String,
    ) : SearchState

    /** Results in ranked order; [more] says whether and how further results arrive. */
    data class Results(
        val query: String,
        val games: List<GameCardModel>,
        val more: MoreResults,
    ) : SearchState

    /** The search matched no games. */
    data class Empty(
        val query: String,
    ) : SearchState

    /** The first results failed to load. */
    data class Error(
        val query: String,
        val error: DataError,
    ) : SearchState
}

/** Whether further results follow the loaded ones. */
sealed interface MoreResults {
    /** More exist; they load as the list nears its end. */
    data object Available : MoreResults

    data object Loading : MoreResults

    /** The next page failed; it loads again only on [GameSearchViewModel.retry], so a failure is never repeated. */
    data class Failed(
        val error: DataError,
    ) : MoreResults

    /** Every result is loaded. */
    data object None : MoreResults
}

/**
 * Searches games as the user types. Typing waits [DEBOUNCE] before a request, so one is sent per pause rather than per
 * keystroke; a new query cancels the running request, so an older response can never replace a newer one. Text that
 * normalizes to the current query sends nothing, and text too short to search returns to [SearchState.Idle] at once.
 *
 * Recent searches keep their loaded results for [CACHE_LIFETIME], so returning to one, such as after deleting a
 * character, shows them at once and continues where its pages ended. All state is confined to the main thread.
 */
class GameSearchViewModel(
    private val searchRepo: GameSearchRepo,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {
    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    /** Loaded results of one search: its games and the page to load next, or null after the last page. */
    private class LoadedResults(
        val games: List<GameCardModel>,
        val nextPage: Int?,
        val loadedAt: TimeMark,
    )

    // Least recently used first.
    private val recentSearches = LinkedHashMap<String, LoadedResults>()

    /** The query being shown or loaded, or null while idle. */
    private var query: String? = null
    private var loaded: LoadedResults? = null

    /** The running request for [query], including the debounce before its first page. */
    private var job: Job? = null
    private var debouncing = false

    /** Follows the search field: waits for a pause in typing before searching. */
    fun onQueryChange(text: String) {
        search(SearchQuery.normalize(text), debounce = true)
    }

    /** Searches [text] at once, as when the keyboard's search action is pressed, and retries a failed search. */
    fun onSearch(text: String) {
        val normalized = SearchQuery.normalize(text)
        val current = _state.value
        when {
            normalized != query -> search(normalized, debounce = false)
            debouncing -> loadFirstPage(checkNotNull(normalized), debounce = false)
            current is SearchState.Error -> retry()
        }
    }

    /** Loads the next page when more results are available and none is loading. */
    fun loadMore() {
        val current = _state.value as? SearchState.Results ?: return
        if (current.more != MoreResults.Available) return
        val results = loaded ?: return
        val nextPage = results.nextPage ?: return
        _state.value = current.copy(more = MoreResults.Loading)
        job =
            viewModelScope.launch {
                val outcome = searchRepo.search(current.query, nextPage)
                if (current.query != query) return@launch
                _state.value =
                    when (outcome) {
                        is Outcome.Success -> {
                            // Pages come from one ranking, so a repeated game only follows a ranking refresh.
                            val games = (results.games + outcome.value.games.toCards()).distinctBy { it.id }
                            val nextResults =
                                LoadedResults(
                                    games = games,
                                    nextPage = if (outcome.value.isLast) null else nextPage + 1,
                                    loadedAt = results.loadedAt,
                                )
                            show(current.query, nextResults)
                        }
                        is Outcome.Failure -> current.copy(more = MoreResults.Failed(outcome.error))
                    }
            }
    }

    /** Loads again what failed: the first results, or the next page below the loaded ones. */
    fun retry() {
        when (val current = _state.value) {
            is SearchState.Error -> loadFirstPage(current.query, debounce = false)
            is SearchState.Results ->
                if (current.more is MoreResults.Failed) {
                    _state.value = current.copy(more = MoreResults.Available)
                    loadMore()
                }
            else -> Unit
        }
    }

    private fun search(
        normalized: String?,
        debounce: Boolean,
    ) {
        if (normalized == query) return
        query = normalized
        loaded = null
        job?.cancel()
        debouncing = false
        if (normalized == null) {
            _state.value = SearchState.Idle
            return
        }
        val recent = recentResults(normalized)
        if (recent != null) {
            _state.value = show(normalized, recent)
        } else {
            loadFirstPage(normalized, debounce)
        }
    }

    private fun loadFirstPage(
        query: String,
        debounce: Boolean,
    ) {
        job?.cancel()
        _state.value = SearchState.Loading(query)
        debouncing = debounce
        job =
            viewModelScope.launch {
                if (debounce) {
                    delay(DEBOUNCE)
                    debouncing = false
                }
                val outcome = searchRepo.search(query, page = 1)
                if (query != this@GameSearchViewModel.query) return@launch
                _state.value =
                    when (outcome) {
                        is Outcome.Success -> {
                            val results =
                                LoadedResults(
                                    games = outcome.value.games.toCards(),
                                    nextPage = if (outcome.value.isLast) null else 2,
                                    loadedAt = timeSource.markNow(),
                                )
                            show(query, results)
                        }
                        is Outcome.Failure -> SearchState.Error(query, outcome.error)
                    }
            }
    }

    /** Makes [results] the current ones, remembers them, and returns the state that shows them. */
    private fun show(
        query: String,
        results: LoadedResults,
    ): SearchState {
        loaded = results
        recentSearches.remove(query)
        recentSearches[query] = results
        while (recentSearches.size > MAX_RECENT_SEARCHES) recentSearches.remove(recentSearches.keys.first())
        return when {
            results.games.isEmpty() -> SearchState.Empty(query)
            results.nextPage == null -> SearchState.Results(query, results.games, MoreResults.None)
            else -> SearchState.Results(query, results.games, MoreResults.Available)
        }
    }

    private fun recentResults(query: String): LoadedResults? {
        val results = recentSearches.remove(query) ?: return null
        return results.takeIf { it.loadedAt.elapsedNow() < CACHE_LIFETIME }
    }

    // A game shows its release year, which tells apart games that share a title. Cards without an ID are dropped.
    private fun List<GameItem>.toCards(): List<GameCardModel> =
        mapNotNull { game ->
            game.toGameCardModel(label = game.firstReleaseDate?.utcYearOrNull()?.let(GameCardLabel::ReleaseYear))
        }.distinctBy { it.id }

    companion object {
        /** About the pause between words while typing; shorter waits send a request for every partial word. */
        val DEBOUNCE = 300.milliseconds

        /** The API refreshes rankings hourly and the browser keeps responses 20 minutes; this is well within both. */
        val CACHE_LIFETIME = 10.minutes

        /** Each search holds at most 200 small cards, so this bounds memory to a few thousand cards. */
        const val MAX_RECENT_SEARCHES = 20
    }
}
