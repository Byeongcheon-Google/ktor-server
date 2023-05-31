package com.bcgg.pathfinder.api

import com.bcgg.pathfinder.api.response.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NaverDirectionsApi {
    @GET("map-direction/v1/driving")
    suspend fun directions5(
        @Query("start") start: String, //(경도),(위도)
        @Query("goal") goal: String, //(경도),(위도):(경도),(위도):...
        @Query("waypoints") waypoints: String? = null, //(경도),(위도):(경도),(위도):...|...
        @Query("option") optionCode: Int? = null,
        @Query("cartype") carTypeCode: Int? = null,
        @Query("fueltype") fuelTypeCode: Int? = null,
        @Query("mileage") mileage: Double? = null,
        @Query("lang") lang: String? = null
    ): DirectionsResponse

    @GET("map-direction-15/v1/driving")
    suspend fun directions15(
        @Query("start") start: String, //(경도),(위도)
        @Query("goal") goal: String, //(경도),(위도):(경도),(위도):...
        @Query("waypoints") waypoints: String? = null, //(경도),(위도):(경도),(위도):...|...
        @Query("option") optionCode: Int? = null,
        @Query("cartype") carTypeCode: Int? = null,
        @Query("fueltype") fuelTypeCode: Int? = null,
        @Query("mileage") mileage: Double? = null,
        @Query("lang") lang: String? = null
    ): DirectionsResponse
}