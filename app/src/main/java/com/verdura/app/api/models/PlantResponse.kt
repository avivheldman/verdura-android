package com.verdura.app.api.models

import com.google.gson.annotations.SerializedName

data class PlantListResponse(
    @SerializedName("data") val data: List<PlantSummary>,
    @SerializedName("to") val to: Int?,
    @SerializedName("per_page") val perPage: Int?,
    @SerializedName("current_page") val currentPage: Int?,
    @SerializedName("last_page") val lastPage: Int?,
    @SerializedName("total") val total: Int?
)

data class PlantSummary(
    @SerializedName("id") val id: Int,
    @SerializedName("common_name") val commonName: String?,
    @SerializedName("scientific_name") val scientificName: List<String>?,
    @SerializedName("cycle") val cycle: String?,
    @SerializedName("watering") val watering: String?,
    @SerializedName("sunlight") val sunlight: List<String>?,
    @SerializedName("default_image") val defaultImage: PlantImage?
)

data class PlantImage(
    @SerializedName("original_url") val originalUrl: String?,
    @SerializedName("regular_url") val regularUrl: String?,
    @SerializedName("medium_url") val mediumUrl: String?,
    @SerializedName("small_url") val smallUrl: String?,
    @SerializedName("thumbnail") val thumbnail: String?
)

data class PlantDetailResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("common_name") val commonName: String?,
    @SerializedName("scientific_name") val scientificName: List<String>?,
    @SerializedName("family") val family: String?,
    @SerializedName("origin") val origin: List<String>?,
    @SerializedName("type") val type: String?,
    @SerializedName("cycle") val cycle: String?,
    @SerializedName("watering") val watering: String?,
    @SerializedName("sunlight") val sunlight: List<String>?,
    @SerializedName("propagation") val propagation: List<String>?,
    @SerializedName("care_level") val careLevel: String?,
    @SerializedName("growth_rate") val growthRate: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("default_image") val defaultImage: PlantImage?,
    @SerializedName("indoor") val indoor: Boolean?,
    @SerializedName("flowers") val flowers: Boolean?,
    @SerializedName("flowering_season") val floweringSeason: String?
)
