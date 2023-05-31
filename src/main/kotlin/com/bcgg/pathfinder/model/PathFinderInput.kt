package com.bcgg.pathfinder.model

import java.time.LocalDate
import java.time.LocalTime


data class PathFinderInput(
    val date: LocalDate,
    val startTime: LocalTime, //시작 시간
    val endHopeTime: LocalTime, //종료 희망 시간
    val mealTimes: List<LocalTime>, //식사 희망 시간(정렬되어 있어야 함)
    val startPoint: Point, // 시작 지점
    val endPoint: Point, // 끝 지점
    val points: Collection<Point>
)