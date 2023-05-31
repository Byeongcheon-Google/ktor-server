package com.bcgg.pathfinder.pathfinder

import com.bcgg.pathfinder.model.Point
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PathFinderResult(
    val foundTime: LocalTime,
    val date: LocalDate,
    val result: List<PathFinderItem>
) {
    @Serializable
    sealed class PathFinderItem {
        @Serializable
        @SerialName("Place")
        data class Place(
            val name: String,
            val classification: Point.Classification,
            val position: LatLng,
            val stayTimeMinute: Long,
            val startTime: LocalTime
        ) : PathFinderItem()

        @Serializable
        @SerialName("Move")
        data class Move(
            val distance: Double,
            val distanceUnit: DistanceUnit,
            val points: List<LatLng>,
            val boundSouthWest: LatLng,
            val boundNorthEast: LatLng,
            val startTime: LocalTime,
            val durationMinute: Long
        ) : PathFinderItem()
    }

    enum class DistanceUnit {
        km, m
    }

    @Serializable
    data class LatLng(
        val lat: Double,
        val lng: Double
    )
}
