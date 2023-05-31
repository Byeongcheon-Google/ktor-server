package com.bcgg.chat

import com.bcgg.schema.schedule.Place
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.datetime.serializers.LocalTimeIso8601Serializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = ChatPolymophicSerializer::class)
sealed class Chat {

    @Serializable
    @SerialName("ChatStartPlace")
    data class ChatStartPlace(
        @SerialName("date") @Serializable(with = LocalDateIso8601Serializer::class) val date: LocalDate,
        @SerialName("startPlaceId") val startPlace: Place?
    ): Chat()

    @Serializable
    @SerialName("ChatEndPlace")
    data class ChatEndPlace(
        @SerialName("date") @Serializable(with = LocalDateIso8601Serializer::class) val date: LocalDate,
        @SerialName("endPlaceId") val endPlace: Place?
    ): Chat()

    @Serializable
    @SerialName("ChatStartTime")
    data class ChatStartTime(
        @SerialName("date") @Serializable(with = LocalDateIso8601Serializer::class) val date: LocalDate,
        @SerialName("startTime") @Serializable(with = LocalTimeIso8601Serializer::class) val startTime: LocalTime
    ): Chat()

    @Serializable
    @SerialName("ChatEndTime")
    data class ChatEndTime(
        @SerialName("date") @Serializable(with = LocalDateIso8601Serializer::class) val date: LocalDate,
        @SerialName("endTime") @Serializable(with = LocalTimeIso8601Serializer::class) val endTime : LocalTime
    ): Chat()

    @Serializable
    @SerialName("ChatMapPosition")
    data class ChatMapPosition(
        @SerialName("userId") val userId: String,
        @SerialName("lat") val lat: Double,
        @SerialName("lng") val lng: Double
    ): Chat()

    @Serializable
    @SerialName("ChatMealTimes")
    data class ChatMealTimes(
        @SerialName("date") @Serializable(with = LocalDateIso8601Serializer::class) val date: LocalDate,
        @SerialName("mealTimes") val mealTimes: List<LocalTime>
    ): Chat()

    @Serializable
    @SerialName("ChatPoints")
    data class ChatPoints(
        @SerialName("date") @Serializable(with = LocalDateIso8601Serializer::class) val date: LocalDate,
        @SerialName("points") val points: List<Place>
    ): Chat()

    @Serializable
    @SerialName("ChatAddPoint")
    data class ChatAddPoint(
        @SerialName("date") @Serializable(with = LocalDateIso8601Serializer::class) val date: LocalDate,
        @SerialName("pointToAdd") val point: Place
    ): Chat()

    @Serializable
    @SerialName("ChatRemovePoint")
    data class ChatRemovePoint(
        @SerialName("date") @Serializable(with = LocalDateIso8601Serializer::class) val date: LocalDate,
        @SerialName("pointToRemove") val point: Place
    ): Chat()

    @Serializable
    @SerialName("ChatTitle")
    data class ChatTitle(
        @SerialName("title") val title: String
    ): Chat()

    @Serializable
    @SerialName("ChatCount")
    data class ChatCount(
        @SerialName("count") val count: Int
    ): Chat()
}