package com.provex.app

import android.app.Application
import com.provex.app.di.AppContainer
import com.provex.app.di.DefaultAppContainer

class ProvexApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
