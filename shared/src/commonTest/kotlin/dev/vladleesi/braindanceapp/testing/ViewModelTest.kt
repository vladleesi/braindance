// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Runs `viewModelScope` (Dispatchers.Main) and the mock backend engine on one test scheduler, so tests control
 * ordering with `advanceUntilIdle()` and `runCurrent()` instead of waiting on real threads.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    protected val dispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        stopTestKoin()
        Dispatchers.resetMain()
    }
}
