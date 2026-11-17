// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.repository.FeedPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Whether further results follow the loaded ones. */
sealed interface MoreResults {
    /** More exist; they load as the list nears its end. */
    data object Available : MoreResults

    data object Loading : MoreResults

    /** The next page failed; it loads again only when the user retries, so a failure is never repeated. */
    data class Failed(
        val error: DataError,
    ) : MoreResults

    /** Every result is loaded. */
    data object None : MoreResults
}

/** What a list that loads page by page shows. */
sealed interface PagedListState<out T> {
    /** The first page is loading, with nothing to show yet. */
    data object Loading : PagedListState<Nothing>

    /** Every item loaded so far, in order; [more] says whether and how further items arrive. */
    data class Loaded<T>(
        val items: List<T>,
        val more: MoreResults,
    ) : PagedListState<T>

    /** The list has no items. */
    data object Empty : PagedListState<Nothing>

    /** The first page failed to load. */
    data class Error(
        val error: DataError,
    ) : PagedListState<Nothing>
}

/**
 * Holds a list that loads page by page, such as a Home section's full list. The screen calls [load] once; [loadMore]
 * adds the next page while keeping the loaded items, and [retry] loads again whatever failed. At most one request runs
 * at a time, and [refresh] cancels it, so an older response never replaces newer state. Items repeated across pages,
 * such as after the source's ranking changed, appear once, at their first position, by [itemId].
 */
abstract class PagedListViewModel<T>(
    private val itemId: (T) -> Any,
) : ScreenViewModel() {
    private val _state = MutableStateFlow<PagedListState<T>>(PagedListState.Loading)
    val state: StateFlow<PagedListState<T>> = _state.asStateFlow()

    /** The page to load next, or null after the last one. */
    private var nextPage: Int? = FIRST_PAGE
    private var job: Job? = null

    /** Page [page] (1-based) as items ready to show. */
    protected abstract suspend fun loadPage(page: Int): Outcome<FeedPage<T>>

    override fun onLoad() {
        loadFirstPage()
    }

    override fun onRefresh() {
        loadFirstPage()
    }

    /** Loads the next page when more items are available and none is loading. */
    fun loadMore() {
        val current = _state.value as? PagedListState.Loaded ?: return
        if (current.more != MoreResults.Available) return
        val page = nextPage ?: return
        _state.value = current.copy(more = MoreResults.Loading)
        job =
            viewModelScope.launch {
                _state.value =
                    when (val outcome = loadPage(page)) {
                        is Outcome.Success -> {
                            nextPage = if (outcome.value.isLast) null else page + 1
                            loaded((current.items + outcome.value.items).distinctBy(itemId))
                        }
                        is Outcome.Failure -> current.copy(more = MoreResults.Failed(outcome.error))
                    }
            }
    }

    /** Loads again what failed: the first page, or the next page below the loaded items. */
    fun retry() {
        when (val current = _state.value) {
            is PagedListState.Error -> loadFirstPage()
            is PagedListState.Loaded ->
                if (current.more is MoreResults.Failed) {
                    _state.value = current.copy(more = MoreResults.Available)
                    loadMore()
                }
            else -> Unit
        }
    }

    private fun loadFirstPage() {
        job?.cancel()
        _state.value = PagedListState.Loading
        job =
            viewModelScope.launch {
                _state.value =
                    when (val outcome = loadPage(FIRST_PAGE)) {
                        is Outcome.Success -> {
                            nextPage = if (outcome.value.isLast) null else FIRST_PAGE + 1
                            loaded(outcome.value.items.distinctBy(itemId))
                        }
                        is Outcome.Failure -> PagedListState.Error(outcome.error)
                    }
            }
    }

    private fun loaded(items: List<T>): PagedListState<T> =
        when {
            items.isEmpty() && nextPage == null -> PagedListState.Empty
            nextPage == null -> PagedListState.Loaded(items, MoreResults.None)
            else -> PagedListState.Loaded(items, MoreResults.Available)
        }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
