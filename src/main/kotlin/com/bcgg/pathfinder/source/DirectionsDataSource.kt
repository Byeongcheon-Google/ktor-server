package com.bcgg.pathfinder.source

import com.bcgg.pathfinder.model.Point

interface DirectionsDataSource {
    fun getTimeAndCost(point1: Point, point2: Point): Pair<Double, Double>
    fun getPath(point1: Point, point2: Point)
}