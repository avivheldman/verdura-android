package com.verdura.app.api

import com.verdura.app.api.models.PlantDetailResponse
import com.verdura.app.api.models.PlantListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PlantApiService {

    @GET("species-list")
    suspend fun getPlantList(
        @Query("key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("q") query: String? = null,
        @Query("per_page") perPage: Int = 60,
        @Query("indoor") indoor: Int = 1
    ): PlantListResponse

    @GET("species/details/{id}")
    suspend fun getPlantDetails(
        @Path("id") plantId: Int,
        @Query("key") apiKey: String
    ): PlantDetailResponse
}
