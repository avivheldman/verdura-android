package com.verdura.app

import android.app.Application
import com.verdura.app.util.PicassoConfig

class VerduraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Picasso with custom caching configuration
        PicassoConfig.initialize(this)
    }
}
