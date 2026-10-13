// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import io.ktor.client.HttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class KtorClientManager : KoinComponent {
    val backendHttpClient: HttpClient by inject(named(BACKEND_HTTP_CLIENT))

    companion object {
        const val BACKEND_HTTP_CLIENT = "BACKEND_HTTP_CLIENT"
    }
}
