package com.bcgg.pathfinder.pathfinder

import com.bcgg.pathfinder.model.Point
import java.time.LocalTime

sealed class PathFinderState {
    data class Finding(
        val searchEdgesCount: Long,
        val allEdgesCount: Long
    ): PathFinderState()

    data class Found(
        val path: List<Pair<Point, ClosedRange<LocalTime>>>,
        val time: LocalTime
    ): PathFinderState()
}
