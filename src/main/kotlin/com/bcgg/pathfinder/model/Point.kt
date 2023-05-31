package com.bcgg.pathfinder.model

import com.bcgg.pathfinder.util.DistanceCalculator

data class Point(
    val name: String? = null,
    val lat: Double,
    val lon: Double,
    val classification: Classification,
    val stayTimeMinute: Long
) {
    fun isStartPoint(input: PathFinderInput) = this == input.startPoint
    fun isEndPoint(input: PathFinderInput) = this == input.endPoint

    enum class Classification {
        Travel, House, Food;

        override fun toString(): String {
            return when(this) {
                Travel -> "✈️"
                House -> "🏠"
                Food -> "🍴"
            }
        }
    }
    override fun toString(): String {
        return "$classification ${String.format("%s\t\t", name ?: "")} ${String.format("(%.7f, %.7f)", lat, lon)}, "
    }

    override fun equals(other: Any?): Boolean {
        if(other is Point) {
            return this.lat.equals(other.lat) && this.lon.equals(other.lon)
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return this.lat.hashCode() * this.lon.hashCode() * 31
    }
    infix fun distanceTo(other: Point) : Double = DistanceCalculator.getDistance(this.lat, this.lon, other.lat, other.lon)
}
