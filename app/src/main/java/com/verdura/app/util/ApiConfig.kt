package com.verdura.app.util

import android.content.Context
import java.util.Properties

object ApiConfig {
    private var trefleToken: String? = null

    fun init(context: Context) {
        if (trefleToken != null) return
        try {
            val properties = Properties()
            context.assets.open("api_config.properties").use { stream ->
                properties.load(stream)
            }
            trefleToken = properties.getProperty("TREFLE_API_TOKEN")
        } catch (_: Exception) {
            trefleToken = null
        }
    }

    fun getTrefleToken(): String {
        return trefleToken ?: throw IllegalStateException(
            "Trefle API token not configured."
        )
    }
}
