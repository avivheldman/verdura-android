package com.verdura.app.api

import com.verdura.app.api.models.TrefleSearchResponse
import com.verdura.app.api.models.TrefleSpeciesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TrefleApiService {

    @GET("species/search")
    suspend fun searchSpecies(
        @Query("token") token: String,
        @Query("q") query: String
    ): TrefleSearchResponse

    @GET("species/{id}")
    suspend fun getSpeciesDetails(
        @Path("id") speciesId: Int,
        @Query("token") token: String
    ): TrefleSpeciesResponse
}
