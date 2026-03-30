package com.verdura.app.api.models

import com.google.gson.annotations.SerializedName

data class TrefleSearchResponse(
    @SerializedName("data") val data: List<TrefleSpeciesSummary>,
    @SerializedName("meta") val meta: TrefleMeta?
)

data class TrefleSpeciesResponse(
    @SerializedName("data") val data: TrefleSpeciesDetail
)

data class TrefleMeta(
    @SerializedName("total") val total: Int?
)

data class TrefleSpeciesSummary(
    @SerializedName("id") val id: Int,
    @SerializedName("common_name") val commonName: String?,
    @SerializedName("scientific_name") val scientificName: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("family") val family: String?,
    @SerializedName("genus") val genus: String?
)

data class TrefleSpeciesDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("common_name") val commonName: String?,
    @SerializedName("scientific_name") val scientificName: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("family") val family: String?,
    @SerializedName("family_common_name") val familyCommonName: String?,
    @SerializedName("genus") val genus: String?,
    @SerializedName("observations") val observations: String?,
    @SerializedName("duration") val duration: List<String>?,
    @SerializedName("edible") val edible: Boolean?,
    @SerializedName("edible_part") val ediblePart: List<String>?,
    @SerializedName("flower") val flower: TrefleFlower?,
    @SerializedName("foliage") val foliage: TrefleFoliage?,
    @SerializedName("specifications") val specifications: TrefleSpecifications?,
    @SerializedName("growth") val growth: TrefleGrowth?,
    @SerializedName("images") val images: TrefleImages?
)

data class TrefleFlower(
    @SerializedName("color") val color: List<String>?,
    @SerializedName("conspicuous") val conspicuous: Boolean?
)

data class TrefleFoliage(
    @SerializedName("texture") val texture: String?,
    @SerializedName("color") val color: List<String>?,
    @SerializedName("leaf_retention") val leafRetention: Boolean?
)

data class TrefleSpecifications(
    @SerializedName("ligneous_type") val ligneousType: String?,
    @SerializedName("growth_form") val growthForm: String?,
    @SerializedName("growth_habit") val growthHabit: String?,
    @SerializedName("growth_rate") val growthRate: String?,
    @SerializedName("average_height") val averageHeight: TrefleMeasure?,
    @SerializedName("maximum_height") val maximumHeight: TrefleMeasure?,
    @SerializedName("toxicity") val toxicity: String?,
    @SerializedName("shape_and_orientation") val shapeAndOrientation: String?
)

data class TrefleGrowth(
    @SerializedName("description") val description: String?,
    @SerializedName("sowing") val sowing: String?,
    @SerializedName("light") val light: Int?,
    @SerializedName("atmospheric_humidity") val atmosphericHumidity: Int?,
    @SerializedName("soil_humidity") val soilHumidity: Int?,
    @SerializedName("minimum_temperature") val minimumTemperature: TrefleMeasure?,
    @SerializedName("maximum_temperature") val maximumTemperature: TrefleMeasure?,
    @SerializedName("ph_minimum") val phMinimum: Double?,
    @SerializedName("ph_maximum") val phMaximum: Double?,
    @SerializedName("bloom_months") val bloomMonths: List<String>?,
    @SerializedName("growth_months") val growthMonths: List<String>?,
    @SerializedName("soil_nutriments") val soilNutriments: Int?,
    @SerializedName("soil_texture") val soilTexture: Int?
)

data class TrefleMeasure(
    @SerializedName("cm") val cm: Double?,
    @SerializedName("deg_c") val degC: Double?,
    @SerializedName("deg_f") val degF: Double?
)

data class TrefleImages(
    @SerializedName("flower") val flower: List<TrefleImage>?,
    @SerializedName("leaf") val leaf: List<TrefleImage>?,
    @SerializedName("habit") val habit: List<TrefleImage>?,
    @SerializedName("fruit") val fruit: List<TrefleImage>?,
    @SerializedName("bark") val bark: List<TrefleImage>?,
    @SerializedName("other") val other: List<TrefleImage>?
)

data class TrefleImage(
    @SerializedName("id") val id: Int?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("copyright") val copyright: String?
)
