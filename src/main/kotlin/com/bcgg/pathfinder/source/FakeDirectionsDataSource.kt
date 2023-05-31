package com.bcgg.pathfinder.source

import com.bcgg.pathfinder.model.Point

class FakeDirectionsDataSource : DirectionsDataSource {
    override fun getTimeAndCost(point1: Point, point2: Point): Pair<Double, Double> {
        val distance = point1.distanceTo(point2)

        return distance / 10 to 0.0
    }

    override fun getPath(point1: Point, point2: Point) {
        TODO("Not yet implemented")
    }
}