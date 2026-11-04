// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.testing

import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class TestHarnessTest : ViewModelTest() {
    @Test
    fun homeViewModelLoadsThroughTheMockBackend() =
        runTest(dispatcher) {
            val client = mockBackendClient(dispatcher) { respondJson("""[{"id":1,"name":"A"}]""") }
            val viewModel =
                HomeViewModel(
                    homeRepo = HomeRepo(GamesRemote(client)),
                    gamerPowerRepo = GamerPowerRepo(GamerPowerRemote(client)),
                )

            viewModel.load()
            advanceUntilIdle()

            assertIs<SectionState.Success<*>>(viewModel.trendingNow.value)
        }
}
