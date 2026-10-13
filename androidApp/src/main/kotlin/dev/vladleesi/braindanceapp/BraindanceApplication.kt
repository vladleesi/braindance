// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import android.app.Application
import android.content.pm.ApplicationInfo
import dev.vladleesi.braindanceapp.koin.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class BraindanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            androidConfig = {
                androidLogger(Level.DEBUG)
                androidContext(this@BraindanceApplication)
            },
            debugHttpLogging = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
        )
    }
}
