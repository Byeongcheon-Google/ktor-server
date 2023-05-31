package com.bcgg.response

import com.bcgg.pathfinder.pathfinder.PathFinderResult
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class PlanResultResponse(
    val name: String,
    val pathFinderResults: List<PathFinderResult>
)
