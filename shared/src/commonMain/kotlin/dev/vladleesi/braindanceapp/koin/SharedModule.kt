// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.koin

import dev.vladleesi.braindanceapp.data.api.clients.BackendClient
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.api.warmUpBackend
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayDetailsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeViewModel
import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(
    androidConfig: KoinAppDeclaration? = null,
    isDebugBuild: Boolean = false,
) {
    // Logs and HTTP logging stay off in release builds.
    if (isDebugBuild) BLogger.enable()
    val application =
        startKoin {
            androidConfig?.invoke(this)
            koinModules(isDebugBuild)
        }
    warmUpBackend { application.koin.get<HttpClient>() }
}

private fun KoinApplication.koinModules(isDebugBuild: Boolean) =
    modules(remoteModule, repositoryModule, viewModelModule, httpClientModule(isDebugBuild))

// The backend client is the only HttpClient in the graph, so remotes receive it by constructor injection.
private fun httpClientModule(isDebugBuild: Boolean) =
    module {
        single<HttpClient> { BackendClient.build(debugHttpLogging = isDebugBuild) }
    }

private val remoteModule =
    module {
        singleOf(::GamesRemote)
        singleOf(::GamerPowerRemote)
    }

private val repositoryModule =
    module {
        singleOf(::HomeRepo)
        singleOf(::GamerPowerRepo)
        singleOf(::GameDetailsRepo)
    }

private val viewModelModule =
    module {
        viewModelOf(::HomeViewModel)
        viewModel { (id: Int) -> GameDetailsViewModel(gameId = id, gameDetailsRepo = get()) }
        viewModel { (id: Int) -> GiveawayDetailsViewModel(giveawayId = id, gamerPowerRepo = get()) }
    }
