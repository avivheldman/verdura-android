package com.verdura.app.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TREFLE_BASE_URL = "https://trefle.io/api/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val trefleClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val trefleRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(TREFLE_BASE_URL)
        .client(trefleClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val trefleApiService: TrefleApiService = trefleRetrofit.create(TrefleApiService::class.java)
}
