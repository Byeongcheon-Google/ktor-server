package com.bcgg.pathfinder.api.response

data class DirectionsResponse(
    val code: Int,
    val message: String,
    val currentDateTime: String? = null,
    val route: Route? = null
) {

    data class Route(
        val traoptimal: List<TraOptimal>
    ) {

        data class TraOptimal(
            val summary: Summary,
            val path: List<List<Double>>,
            val section: List<Section>,
            val guide: List<Guide>,
        ) {

            data class Summary(
                val start: Start,
                val goal: Goal,
                val waypoints: List<Waypoint>?,
                val distance: Long, // meter
                val duration: Long, // milliseconds
                val bbox: List<List<Double>>,
                val tollFare: Long,
                val taxiFare: Long,
                val fuelPrice: Long
            )


            data class Section(
                val pointIndex: Long,
                val pointCount: Long,
                val distance: Long,
                val name: String,
                val congestion: Long,
                val speed: Long
            )


            data class Guide(
                val pointIndex: Long,
                val type: Int,
                val instructions: String,
                val distance: Long,
                val duration: Long
            )


            data class Waypoint(
                val location: List<Double>,
                val dir: Int,
                val distance: Long,
                val duration: Long,
                val pointIndex: Long
            )


            data class Start(
                val location: List<Double>
            )


            data class Goal(
                val location: List<Double>,
                val dir: Int,
                val distance: Long,
                val duration: Long,
                val pointIndex: Long
            )
        }

    }
}