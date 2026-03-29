package com.verdura.app.util

import android.content.Context
import java.util.Properties

object ApiConfig {
    private var plantApiKey: String? = null

    fun init(context: Context) {
        if (plantApiKey != null) return
        try {
            val properties = Properties()
            context.assets.open("api_config.properties").use { stream ->
                properties.load(stream)
            }
            plantApiKey = properties.getProperty("PLANT_API_KEY")
        } catch (_: Exception) {
            plantApiKey = null
        }
    }

    fun getPlantApiKey(): String {
        return plantApiKey ?: throw IllegalStateException(
            "Plant API key not configured. Copy api_config.properties.example to api_config.properties and set your key."
        )
    }
}
