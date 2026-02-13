package com.verdura.app.util

import android.content.Context
import com.squareup.picasso.LruCache
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object PicassoConfig {

    private const val DISK_CACHE_SIZE = 50L * 1024 * 1024 // 50 MB
    private const val MEMORY_CACHE_SIZE = 10 * 1024 * 1024 // 10 MB

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        val cacheDir = File(context.cacheDir, "picasso_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val okHttpClient = OkHttpClient.Builder()
            .cache(Cache(cacheDir, DISK_CACHE_SIZE))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val picasso = Picasso.Builder(context)
            .downloader(OkHttp3Downloader(okHttpClient))
            .memoryCache(LruCache(MEMORY_CACHE_SIZE))
            .indicatorsEnabled(false)
            .loggingEnabled(false)
            .build()

        Picasso.setSingletonInstance(picasso)
        isInitialized = true
    }

    fun clearCache(context: Context) {
        val cacheDir = File(context.cacheDir, "picasso_cache")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
        Picasso.get().shutdown()
        isInitialized = false
        initialize(context)
    }
}
