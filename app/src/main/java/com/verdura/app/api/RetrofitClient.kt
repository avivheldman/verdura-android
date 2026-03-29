package com.verdura.app.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val PERENUAL_BASE_URL = "https://perenual.com/api/"
    private const val TREFLE_BASE_URL = "https://trefle.io/api/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val perenualRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(PERENUAL_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val trefleRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(TREFLE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val plantApiService: PlantApiService = perenualRetrofit.create(PlantApiService::class.java)
    val trefleApiService: TrefleApiService = trefleRetrofit.create(TrefleApiService::class.java)
}
