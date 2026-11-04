// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.DataError

/** One independently loaded screen section, such as a Home carousel, holding items of type [T]. */
sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>

    data class Success<T>(
        val items: List<T>,
    ) : SectionState<T>

    data class Error(
        val error: DataError,
    ) : SectionState<Nothing>
}
