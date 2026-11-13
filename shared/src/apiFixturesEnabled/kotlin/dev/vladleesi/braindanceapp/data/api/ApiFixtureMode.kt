// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import dev.vladleesi.braindanceapp.data.api.fixtures.createApiFixtureEngine
import io.ktor.client.engine.HttpClientEngine

/** API_FIXTURES=true: API requests and images are answered from built-in sample data, without network access. */
internal fun apiFixtureEngine(): HttpClientEngine? = createApiFixtureEngine()
