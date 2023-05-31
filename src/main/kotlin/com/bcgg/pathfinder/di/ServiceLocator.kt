package com.bcgg.pathfinder.di

import com.bcgg.pathfinder.model.NaverMapKey
import com.bcgg.pathfinder.api.NaverDirectionsApi
import com.bcgg.pathfinder.model.Point
import com.bcgg.pathfinder.repository.DirectionsRepository
import com.bcgg.pathfinder.source.DirectionsLocalDataSource
import com.bcgg.pathfinder.util.LocalDateSerializer
import com.bcgg.pathfinder.util.LocalTimeSerializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

object ServiceLocator {
    private val file = File("src/main/resources/naver_map_key.json")
    private val key = Gson().fromJson(file.reader(), NaverMapKey::class.java)

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://naveropenapi.apigw.ntruss.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor {
                    val newRequest = it.request().newBuilder()
                        .addHeader("X-NCP-APIGW-API-KEY-ID", "zqeinhejgc")
                        .addHeader("X-NCP-APIGW-API-KEY", "4Gxsa1CrVQY4GsmimWU6QL65hFIeQbmwhASoheWb")
                        .build()
                    it.proceed(newRequest)
                }
                .build()
        )
        .build()

    private val naverMapDirectionsApi = retrofit.create(NaverDirectionsApi::class.java)
    private val directionsLocalDataSource = DirectionsLocalDataSource()

    val directionsRepository = DirectionsRepository(
        directionsLocalDataSource, naverMapDirectionsApi
    )

    private val gsonBuilder = GsonBuilder().apply {
        registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
        registerTypeAdapter(LocalTime::class.java, LocalTimeSerializer())
    }
    private var _gson: Gson? = null
    val gson: Gson
        get() {
            if (_gson == null) _gson = gsonBuilder.setPrettyPrinting().create()
            return _gson!!
        }
}

object Samples {
    val points = setOf(
        //강릉
        Point(
            name = "국립 대관령자연휴양림", lat = 37.7092769061, lon = 128.7815478522,
            classification = Point.Classification.Travel,
            stayTimeMinute = 80
        ),
        Point(
            name = "등명락가사(강릉)", lat = 37.7116837926, lon = 129.0068052775,
            classification = Point.Classification.Travel,
            stayTimeMinute = 60
        ),
        Point(
            name = "등명해변(등명해수욕장)", lat = 37.7042126254, lon = 129.016615738,
            classification = Point.Classification.Travel,
            stayTimeMinute = 20
        ),
        /*Point(name="르꼬따쥬",lat=37.7875861221,lon=128.8643296445,
            classification = Point.Classification.Travel,
            stayTimeMinute = 30
        ),
        Point(name="리고엠",lat=37.7641862993,lon=128.8770014492,
            classification = Point.Classification.Travel,
            stayTimeMinute = 70
        ),*/
        /*Point(name="동산항물회",lat=37.7672032219,lon=128.9080763334,
            classification = Point.Classification.Food,
            stayTimeMinute = 60
        ),*/
        Point(
            name = "동성호", lat = 37.892356638, lon = 128.8292212313,
            classification = Point.Classification.Food,
            stayTimeMinute = 20
        ),
        Point(
            name = "동일장칼국수", lat = 37.7659703348, lon = 128.9236166386,
            classification = Point.Classification.Food,
            stayTimeMinute = 60
        ),
        //속초
        Point(
            name = "다이나믹 메이즈 (속초점)", lat = 38.2055364158, lon = 128.517345299,
            classification = Point.Classification.Travel,
            stayTimeMinute = 120
        ),
        Point(
            name = "대포마을", lat = 38.1758166221, lon = 128.6072936101,
            classification = Point.Classification.Travel,
            stayTimeMinute = 30
        ),
        Point(
            name = "김삿갓회막국수", lat = 38.2065474555, lon = 128.5197422719,
            classification = Point.Classification.Food,
            stayTimeMinute = 50
        ),
        Point(
            name = "김영애할머니순두부 본점", lat = 38.2060282334, lon = 128.5280831963,
            classification = Point.Classification.Food,
            stayTimeMinute = 60
        )
    )

    val point1 =
        Point(
            lat = 37.7092769061,
            lon = 128.7815478522,
            classification = Point.Classification.Travel,
            stayTimeMinute = 60
        )
    val point2 =
        Point(
            lat = 37.9149286939, lon = 128.8123942726,
            classification = Point.Classification.House,
            stayTimeMinute = 60
        )
    val sampleHouse1 =
        Point(
            name = "강릉 지중해펜션", lat = 37.8330588607, lon = 128.8758521976,
            classification = Point.Classification.House,
            stayTimeMinute = 0
        )
    val sampleHouse2 = Point(
        name = "굿모닝가족호텔", lat = 38.1906453170, lon = 128.6003326682,
        classification = Point.Classification.House,
        stayTimeMinute = 0
    )
    val koreatech = Point(
        name = "한국기술교육대학교 제1캠퍼스",
        lat = 36.763718,
        lon = 127.2819405,
        classification = Point.Classification.House,
        stayTimeMinute = 0
    )
}