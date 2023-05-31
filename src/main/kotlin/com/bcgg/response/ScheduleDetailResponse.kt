package com.bcgg.response

import com.bcgg.schema.schedule.Schedule
import com.bcgg.schema.schedule.SchedulePerDate
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleDetailResponse(
    val schedule: Schedule,
    val schedulePerDates: List<SchedulePerDate>
)