package com.example.mycamerafinal.data

import android.app.Application
import android.content.Context

/**
 * APP CLASS — created once by Android before any screen opens
 * (declared with android:name=".data.App" in the AndroidManifest).
 *
 * Its only job is to keep the application context available everywhere,
 * so the data layer ([Api], [LocalDb]) can create the Volley request queue
 * and open the SQLite database without needing an Activity.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: App
            private set
        val context: Context get() = instance.applicationContext
    }
}
