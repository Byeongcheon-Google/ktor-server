package com.bcgg.response

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val id: Int,
    val title: String,
    val modifiedDateTime: LocalDateTime,
    val dateCount: Int,
    val destinations: List<String>,
    val collaboratorUserIds: List<String>,
)
