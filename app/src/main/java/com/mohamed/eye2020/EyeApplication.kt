package com.mohamed.eye2020

import android.app.Application
import com.mohamed.eye2020.util.AppLocale

class EyeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppLocale.applyDefault(this)
        container = AppContainer(this)
    }
}
