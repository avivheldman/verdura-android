package com.verdura.app.util

import android.content.Context
import java.util.Properties

object ApiConfig {
    private var plantApiKey: String? = null
    private var trefleToken: String? = null

    fun init(context: Context) {
        if (plantApiKey != null) return
        try {
            val properties = Properties()
            context.assets.open("api_config.properties").use { stream ->
                properties.load(stream)
            }
            plantApiKey = properties.getProperty("PLANT_API_KEY")
            trefleToken = properties.getProperty("TREFLE_API_TOKEN")
        } catch (_: Exception) {
            plantApiKey = null
            trefleToken = null
        }
    }

    fun getPlantApiKey(): String {
        return plantApiKey ?: throw IllegalStateException(
            "Plant API key not configured."
        )
    }

    fun getTrefleToken(): String {
        return trefleToken ?: throw IllegalStateException(
            "Trefle API token not configured."
        )
    }
}
