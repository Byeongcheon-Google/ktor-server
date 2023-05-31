package com.bcgg.pathfinder.repository

import com.bcgg.pathfinder.api.NaverDirectionsApi
import com.bcgg.pathfinder.di.Samples.point1
import com.bcgg.pathfinder.di.Samples.point2
import com.bcgg.pathfinder.di.Samples.points
import com.bcgg.pathfinder.di.ServiceLocator
import com.bcgg.pathfinder.model.Point
import com.bcgg.pathfinder.pathfinder.PathFinderResult
import com.bcgg.pathfinder.source.DirectionsLocalDataSource
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.ceil


class DirectionsRepository(
    private val directionsLocalDataSource: DirectionsLocalDataSource,
    private val naverDirectionsApi: NaverDirectionsApi
) {
    suspend fun getMoveTime(allPoints: Set<Point>, point1: Point, point2: Point): Double {
        if (point1 !in allPoints) throw IllegalArgumentException("point 1 is not contains in given all paths")
        if (point2 !in allPoints) throw IllegalArgumentException("point 2 is not contains in given all paths")

        if (point1 == point2) return 0.0

        val duration = directionsLocalDataSource.getDuration(point1, point2)

        if (duration != null) return duration * TIME_PERCENTAGE

        val searchPath = generateNaverDirectionsApiWaypoints(allPoints, point1, point2)

        val start = searchPath.first().toNaverMapApiRequest()
        val waypoints =
            searchPath.slice(1 until searchPath.size).joinToString(separator = "|") { it.toNaverMapApiRequest() }
        val end = searchPath.last().toNaverMapApiRequest()

        val directionsResponse = naverDirectionsApi.directions5(
            start = start,
            goal = end,
            waypoints = waypoints
        )
        //네이버 오류
        if (directionsResponse.code != 0) throw RuntimeException(directionsResponse.message)

        val durations = mutableListOf<Double>()
        //던져진 점 갯수 != 경유지 점 갯수 error
        with(directionsResponse.route!!.traoptimal[0].summary) {
            if (this.waypoints != null) {
                durations.addAll(this.waypoints.map { it.duration / 1000.0 / 60 / 60 })
            }

            val duration = goal.duration / 1000.0 / 60 / 60

            durations.add(ceil(duration * TIME_PERCENTAGE * 6) / 6.0)
        }

        directionsLocalDataSource.put(searchPath, durations)

        return (durations[0])
    }

    suspend fun getResultPath(points: List<Point>, startTime: LocalTime, date: LocalDate, foundTime: LocalTime): PathFinderResult {
        val start = points.first().toNaverMapApiRequest()
        val waypoints =
            points.slice(1 until points.size).joinToString(separator = "|") { it.toNaverMapApiRequest() }
        val end = points.last().toNaverMapApiRequest()

        val directionsResponse = naverDirectionsApi.directions15(
            start = start,
            goal = end,
            waypoints = waypoints
        )

        var pointIndex = 0
        val resultItems = mutableListOf<PathFinderResult.PathFinderItem>()
        var time = startTime

        if (directionsResponse.route == null || directionsResponse.route.traoptimal.isEmpty()) throw RuntimeException(
            directionsResponse.message
        )

        with(directionsResponse.route.traoptimal[0]) {
            resultItems.add(
                PathFinderResult.PathFinderItem.Place(
                    name = points[0].name ?: "",
                    position = PathFinderResult.LatLng(
                        lat = summary.start.location[1],
                        lng = summary.start.location[0]
                    ),
                    stayTimeMinute = points[0].stayTimeMinute,
                    startTime = time.toKotlinLocalTime(),
                    classification = points[0].classification
                )
            )

            summary.waypoints?.forEachIndexed { index, waypoint ->
                val (distance, distanceUnit) = calcDistanceWithUnit(waypoint.distance)

                var minLat = Double.MAX_VALUE
                var minLng = Double.MAX_VALUE
                var maxLat = Double.MIN_VALUE
                var maxLng = Double.MIN_VALUE

                val slicedPoints = path.slice(pointIndex..waypoint.pointIndex.toInt())
                    .map {latlngList ->
                        val lat = latlngList[1]
                        val lng = latlngList[0]

                        if(lat < minLat) minLat = lat
                        if(lng < minLng) minLng = lng
                        if(lat > maxLat) maxLat = lat
                        if(lng > maxLng) maxLng = lng

                        PathFinderResult.LatLng(lat, lng)
                    }
                val durationMinute = ceil(waypoint.duration / 1000.0 / 60).toLong()

                pointIndex = waypoint.pointIndex.toInt()
                resultItems.add(
                    PathFinderResult.PathFinderItem.Move(
                        distance = distance,
                        distanceUnit = distanceUnit,
                        points = slicedPoints,
                        boundSouthWest = PathFinderResult.LatLng(minLat, minLng),
                        boundNorthEast = PathFinderResult.LatLng(maxLat, maxLng),
                        startTime = time.toKotlinLocalTime(),
                        durationMinute = durationMinute
                    )
                )

                time = time.plusMinutes(durationMinute)

                // ----------

                resultItems.add(
                    PathFinderResult.PathFinderItem.Place(
                        name = points[index + 1].name ?: "",
                        position = PathFinderResult.LatLng(
                            lat = waypoint.location[1],
                            lng = waypoint.location[0]
                        ),
                        stayTimeMinute = points[index + 1].stayTimeMinute,
                        startTime = time.toKotlinLocalTime(),
                        classification = points[index + 1].classification
                    )
                )

                time = time.plusMinutes(points[index + 1].stayTimeMinute)
            }

            val (distance, distanceUnit) = calcDistanceWithUnit(summary.goal.distance)

            var minLat = Double.MAX_VALUE
            var minLng = Double.MAX_VALUE
            var maxLat = Double.MIN_VALUE
            var maxLng = Double.MIN_VALUE

            val slicedPoints = path.slice(pointIndex..summary.goal.pointIndex.toInt())
                .map {latlngList ->
                    val lat = latlngList[1]
                    val lng = latlngList[0]

                    if(lat < minLat) minLat = lat
                    if(lng < minLng) minLng = lng
                    if(lat > maxLat) maxLat = lat
                    if(lng > maxLng) maxLng = lng

                    PathFinderResult.LatLng(lat, lng)
                }
            val durationMinute = ceil(summary.goal.duration / 1000.0 / 60).toLong()

            pointIndex = summary.goal.pointIndex.toInt()
            resultItems.add(
                PathFinderResult.PathFinderItem.Move(
                    distance = distance,
                    distanceUnit = distanceUnit,
                    points = slicedPoints,
                    boundSouthWest = PathFinderResult.LatLng(minLat, minLng),
                    boundNorthEast = PathFinderResult.LatLng(maxLat, maxLng),
                    startTime = time.toKotlinLocalTime(),
                    durationMinute = durationMinute
                )
            )

            time = time.plusMinutes(durationMinute)

            // ----------

            resultItems.add(
                PathFinderResult.PathFinderItem.Place(
                    name = points.last().name ?: "",
                    position = PathFinderResult.LatLng(
                        lat = summary.goal.location[1],
                        lng = summary.goal.location[0]
                    ),
                    stayTimeMinute = points.last().stayTimeMinute,
                    startTime = time.toKotlinLocalTime(),
                    classification = points.last().classification
                )
            )
        }

        return PathFinderResult(foundTime.toKotlinLocalTime(), date.toKotlinLocalDate(), resultItems)
    }

    private fun calcDistanceWithUnit(meters: Long): Pair<Double, PathFinderResult.DistanceUnit> {
        if (meters < 1000) return meters.toDouble() to PathFinderResult.DistanceUnit.m

        return meters / 1000.0 to PathFinderResult.DistanceUnit.km
    }

    private fun generateNaverDirectionsApiWaypoints(
        allPoints: Set<Point>,
        point1: Point,
        point2: Point
    ): List<Point> {
        val temp = mutableListOf(point1, point2)
        var linearDistance = point1 distanceTo point2

        while (linearDistance < 1500 && temp.size < 7) {
            var added = false
            for (point in allPoints - temp.toSet()) {
                if (!directionsLocalDataSource.containsPoint(temp.last(), point)) {
                    linearDistance += temp.last() distanceTo point
                    if (linearDistance < 1500 && temp.size < 7) {
                        added = true
                        temp.add(point)
                    }
                    break
                }
            }

            if (!added) break
        }

        return temp
    }

    private fun Point.toNaverMapApiRequest() = "$lon,$lat"

    companion object {
        private const val TIME_PERCENTAGE = 1.1

        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                println(ServiceLocator.directionsRepository.getMoveTime(points, point1, point2))
                println(ServiceLocator.directionsRepository.getMoveTime(points, point1, point2))
            }
        }
    }
}