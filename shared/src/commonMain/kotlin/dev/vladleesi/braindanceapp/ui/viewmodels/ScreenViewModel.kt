// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.ViewModel

abstract class ScreenViewModel : ViewModel() {
    /** Tracks whether the screen has already loaded data */
    private var hasLoaded = false

    /**
     * Called once when `load()` is invoked for the first time.
     * Implement actual data loading here.
     */
    protected abstract fun onLoad()

    /**
     * Called whenever `refresh()` is invoked.
     * Implement refresh logic here, e.g., reloading data or clearing caches.
     */
    protected abstract fun onRefresh()

    /**
     * Loads data if it hasn’t been loaded yet.
     * Subsequent calls do nothing until `refresh()` is called.
     */
    fun load() {
        if (!hasLoaded) {
            hasLoaded = true
            onLoad()
        }
    }

    /**
     * Refreshes the screen by invoking `onRefresh()`.
     * Does not reset the loaded state by default; children can handle reloading if needed.
     */
    fun refresh() {
        onRefresh()
    }
}
