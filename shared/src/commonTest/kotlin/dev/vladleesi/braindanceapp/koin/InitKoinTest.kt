// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.koin

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame

class InitKoinTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun aSecondHostReusesTheRunningKoin() {
        // Stands in for the first host's start, so this test makes no backend warm-up request.
        val running = startKoin {}.koin

        initKoin()

        assertSame(running, KoinPlatform.getKoin())
    }
}
