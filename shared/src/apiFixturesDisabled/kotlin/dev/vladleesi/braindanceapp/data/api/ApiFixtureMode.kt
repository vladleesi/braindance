// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import io.ktor.client.engine.HttpClientEngine

/** Null: this build talks to the configured API. Builds with API_FIXTURES=true compile the fixture variant instead. */
internal fun apiFixtureEngine(): HttpClientEngine? = null
