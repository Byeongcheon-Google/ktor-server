package com.bcgg.routing

import com.bcgg.pathfinder.model.PathFinderInput
import com.bcgg.pathfinder.model.Point
import com.bcgg.pathfinder.pathfinder.PathFinder
import com.bcgg.response.PlanResultResponse
import com.bcgg.schema.schedule.*
import com.bcgg.schema.user.UserService
import com.bcgg.util.getUserIdFromToken
import com.bcgg.util.toDbString
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import org.jetbrains.exposed.sql.Database

fun Application.configurePathfinderRouting(
    database: Database
) {
    val scheduleService = ScheduleService(database)
    val schedulePerDateService = SchedulePerDateService(database)
    val placeService = PlaceService(database)
    val collaboratorService = CollaboratorService(database)
    val userService = UserService(database)

    routing {
        authenticate {
            get("/find/{scheduleId}") {
                val userId = getUserIdFromToken()
                val id = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@get
                }

                val results = schedulePerDateService.readList(scheduleId = id).map { schedulePerDate ->
                    val places = placeService.read(id, schedulePerDate.date!!)
                    if (places.isEmpty()) return@map null

                    val startPlace = places.find { it.id == schedulePerDate.startPlaceId }
                    val endPlace = places.find { it.id == schedulePerDate.endPlaceId }

                    if (startPlace == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "${schedulePerDate.date.toDbString()}의 출발 여행지를 설정해 주세요."
                        )
                        return@get
                    }

                    if (endPlace == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "${schedulePerDate.date.toDbString()}의 도착 여행지를 설정해 주세요."
                        )
                        return@get
                    }

                    val points = places
                        .filter { !(it.id == startPlace.id || it.id == endPlace.id) }
                        .map { it.toPoint() }
                        .distinct()
                    val startPoint = startPlace.toPoint()
                    val endPoint = endPlace.toPoint()

                    val pathFinderInput = PathFinderInput(
                        date = schedulePerDate.date.toJavaLocalDate(),
                        startTime = schedulePerDate.startTime!!.toJavaLocalTime(),
                        endHopeTime = schedulePerDate.endHopeTime!!.toJavaLocalTime(),
                        mealTimes = schedulePerDate.mealTimes?.map { it.toJavaLocalTime() } ?: emptyList(),
                        startPoint = startPoint.copy(stayTimeMinute = 0),
                        endPoint = endPoint.copy(stayTimeMinute = 0),
                        points = points
                    )

                    val pathFinder = PathFinder(pathFinderInput)

                    pathFinder.getResult()
                }
                    .filterNotNull()

                call.respond(
                    PlanResultResponse(
                        name = scheduleService.read(userId, id)?.name ?: "",
                        pathFinderResults = results
                    )
                )
            }
        }
    }
}

internal fun Classification.toPathFinderInputClassification(): Point.Classification {
    return Point.Classification.valueOf(this.name)
}

internal fun Place.toPoint() = Point(
    name = name,
    lat = lat,
    lon = lng,
    classification = classification.toPathFinderInputClassification(),
    stayTimeMinute = stayTimeHour * 60L
)