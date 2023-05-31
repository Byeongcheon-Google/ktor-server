package com.bcgg.response

import com.bcgg.schema.schedule.Place
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class SchedulePerDateResponse(
    val id: Int,
    val scheduleId: Int,
    val date: LocalDate?,
    val startTime: LocalTime?,
    val endHopeTime: LocalTime?,
    val mealTimes: List<LocalTime>?,
    val startPlaceId: Int?,
    val endPlaceId: Int?,
    val places: List<Place>
)
