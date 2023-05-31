package com.bcgg.pathfinder.source

import com.bcgg.pathfinder.model.Point
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import java.util.concurrent.ConcurrentMap

class DirectionsLocalDataSource {
    val cache: ConcurrentMap<Pair<Point, Point>, Double> =
        ConcurrentLinkedHashMap.Builder<Pair<Point, Point>, Double>()
            .maximumWeightedCapacity(1024)
            .build()

    fun put(
        paths: List<Point>, // if path point size is n,
        durations: List<Double> // this list size must be n-1,
    ) {
        if(paths.size - 1 != durations.size) throw IllegalStateException("If path point size is n, durations list size must be n-1.")

        for(i in 1 until paths.size) {
            cache[paths[i-1] to paths[i]] = durations[i - 1]
        }
    }

    fun getDuration(point1: Point, point2: Point) = cache[point1 to point2]

    fun containsPoint(point1: Point, point2: Point) = cache.containsKey(point1 to point2)
}