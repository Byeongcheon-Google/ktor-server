package com.bcgg.request

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleUpdateStayTimeHourRequest(
    val stayTimeHour: Int
)
