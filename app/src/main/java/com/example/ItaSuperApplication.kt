package com.example

import android.app.Application
import com.example.platform.DriverNotificationHelper

class ItaSuperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        runCatching { DriverNotificationHelper.createChannels(this) }
    }

    companion object {
        lateinit var appContext: android.content.Context
            private set
    }
}
