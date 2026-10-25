package com.tubers.blacklight

import android.app.Application
import com.tubers.blacklight.di.AppContainer

class UvGlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
