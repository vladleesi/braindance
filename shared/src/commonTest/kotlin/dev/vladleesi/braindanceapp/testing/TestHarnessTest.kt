// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.testing

import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeState
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class TestHarnessTest : ViewModelTest() {
    @Test
    fun homeViewModelLoadsThroughTheMockBackend() =
        runTest(dispatcher) {
            val client = mockBackendClient(dispatcher) { respondJson("""[{"id":1,"name":"A"}]""") }
            val manager = startTestKoin(client)
            val viewModel =
                HomeViewModel(
                    homeRepo = HomeRepo(GamesRemote(manager)),
                    gamerPowerRepo = GamerPowerRepo(GamerPowerRemote(manager)),
                )

            viewModel.load()
            advanceUntilIdle()

            assertIs<HomeState.Success<*>>(viewModel.trendingNow.value)
        }
}
