package com.krishiyan

import android.app.Application
import org.osmdroid.config.Configuration

class KrishiYanApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        container = AppContainer(this)
    }
}
