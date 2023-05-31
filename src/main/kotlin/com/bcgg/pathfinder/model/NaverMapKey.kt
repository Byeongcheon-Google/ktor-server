package com.bcgg.pathfinder.model

import com.google.gson.annotations.SerializedName

data class NaverMapKey(
    @SerializedName("X-NCP-APIGW-API-KEY-ID") val apiKeyId: String,
    @SerializedName("X-NCP-APIGW-API-KEY") val apiKey: String
)