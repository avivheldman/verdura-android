package com.verdura.app.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Ensures a minimum gap between outgoing requests so we stay under the
 * 60 req/min Perenual rate limit. Uses a synchronized timestamp to space
 * requests at least [minIntervalMs] apart.
 */
private class ThrottleInterceptor(
    private val minIntervalMs: Long = 1200L
) : Interceptor {
    private val lock = Semaphore(1)

    @Volatile
    private var lastRequestTime = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        lock.acquire()
        try {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < minIntervalMs) {
                Thread.sleep(minIntervalMs - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
        } finally {
            lock.release()
        }
        return chain.proceed(chain.request())
    }
}

object RetrofitClient {

    private const val PERENUAL_BASE_URL = "https://perenual.com/api/"
    private const val TREFLE_BASE_URL = "https://trefle.io/api/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val perenualClient = OkHttpClient.Builder()
        .addInterceptor(ThrottleInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val trefleClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val perenualRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(PERENUAL_BASE_URL)
        .client(perenualClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val trefleRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(TREFLE_BASE_URL)
        .client(trefleClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val plantApiService: PlantApiService = perenualRetrofit.create(PlantApiService::class.java)
    val trefleApiService: TrefleApiService = trefleRetrofit.create(TrefleApiService::class.java)
}
