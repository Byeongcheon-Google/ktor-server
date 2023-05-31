package com.bcgg.routing

import com.bcgg.request.ScheduleUpdateStayTimeHourRequest
import com.bcgg.response.ScheduleDetailResponse
import com.bcgg.response.SchedulePerDateResponse
import com.bcgg.response.ScheduleResponse
import com.bcgg.schema.schedule.*
import com.bcgg.schema.user.UserService
import com.bcgg.util.getUserIdFromToken
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import kotlinx.datetime.toLocalDate
import org.jetbrains.exposed.sql.*
import java.lang.NullPointerException

fun Application.configureScheduleRouting(
    database: Database
) {
    val scheduleService = ScheduleService(database)
    val schedulePerDateService = SchedulePerDateService(database)
    val placeService = PlaceService(database)
    val collaboratorService = CollaboratorService(database)
    val userService = UserService(database)

    routing {
        authenticate {
            get("/schedule") {
                val id = getUserIdFromToken()

                val schedules = scheduleService.read(id)

                val list = schedules.map { schedule ->
                    val schedulePerDates = schedulePerDateService.readList(schedule.id)

                    ScheduleResponse(
                        id = schedule.id,
                        title = schedule.name,
                        modifiedDateTime = schedule.updatedAt,
                        dateCount = schedulePerDates.count(),
                        destinations = schedulePerDates.map { schedulePerDate ->
                            placeService.read(scheduleId = schedule.id, schedulePerDate.date!!).map { it.name }
                        }.flatten(),
                        collaboratorUserIds = collaboratorService.collaborators(schedule.id).map { it.userId }
                    )
                }
                call.respond(list)
            }
            post("/schedule") {
                val id = getUserIdFromToken()

                val schedule = call.receive<Schedule>()
                call.respond(scheduleService.create(id, schedule))
            }
            put("/schedule/{id}") {
                val userId = getUserIdFromToken()
                val id = call.parameters["id"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@put
                }

                val schedule = call.receive<Schedule>()
                scheduleService.updateScheduleName(userId, id, schedule.name)
                call.respond(HttpStatusCode.OK)
            }
            delete("/schedule/{id}") {
                val userId = getUserIdFromToken()
                val id = call.parameters["id"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@delete
                }

                scheduleService.delete(userId, id)

                call.respond(HttpStatusCode.OK)
            }

            get("/schedule/{scheduleId}") {
                val userId = getUserIdFromToken()
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@get
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@get
                }

                call.respond(
                    ScheduleDetailResponse(
                        schedule, schedulePerDateService.readList(scheduleId)
                    )
                )
            }

            post("/schedule/{scheduleId}/collaborators/{collaboratorId}") {
                val userId = getUserIdFromToken()
                val collaboratorId = call.parameters["collaboratorId"] ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "공동 작업자 ID 필드가 존재하지 않습니다.")
                    return@post
                }
                if(userService.read(collaboratorId) == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID가 존재하지 않습니다.")
                    return@post
                }
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@post
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@post
                }
                if(collaboratorService.collaborators(schedule.id).map { it.userId }.contains(collaboratorId)) {
                    call.respond(HttpStatusCode.BadRequest, "해당 ID는 이미 공동작업자입니다.")
                    return@post
                }
                collaboratorService.addCollaborator(scheduleId, collaboratorId)
                call.respond(HttpStatusCode.OK)
            }

            delete("/schedule/{scheduleId}/collaborators/{collaboratorId}") {
                val userId = getUserIdFromToken()
                val collaboratorId = call.parameters["collaboratorId"] ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "공동 작업자 ID 필드가 존재하지 않습니다.")
                    return@delete
                }
                if(userService.read(collaboratorId) == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID가 존재하지 않습니다.")
                    return@delete
                }
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@delete
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@delete
                }
                if(!collaboratorService.collaborators(schedule.id).map { it.userId }.contains(collaboratorId)) {
                    call.respond(HttpStatusCode.BadRequest, "해당 ID는 공동작업자가 아닙니다.")
                    return@delete
                }
                collaboratorService.removeCollaborator(scheduleId, collaboratorId)
                call.respond(HttpStatusCode.OK)
            }

            get("/schedule/{scheduleId}/{date}") {
                val userId = getUserIdFromToken()
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@get
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@get
                }
                val date = try {
                    call.parameters["date"]?.toLocalDate() ?: throw NullPointerException()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "날짜 형식이 잘못되었습니다.")
                    return@get
                }

                val schedulePerDate = schedulePerDateService.read(scheduleId, date)
                if (schedulePerDate == null) {
                    call.respond(HttpStatusCode.BadRequest, "해당 날짜에 해당하는 여행 계획이 없습니다.")
                    return@get
                }

                val places = placeService.read(scheduleId, date)

                call.respond(
                    SchedulePerDateResponse(
                        id = schedulePerDate.id,
                        scheduleId = schedulePerDate.scheduleId,
                        date = schedulePerDate.date,
                        startTime = schedulePerDate.startTime,
                        endHopeTime = schedulePerDate.endHopeTime,
                        mealTimes = schedulePerDate.mealTimes,
                        startPlaceId = schedulePerDate.startPlaceId,
                        endPlaceId = schedulePerDate.endPlaceId,
                        places = places
                    )
                )
            }

            post("/schedule/{scheduleId}/{date}") {
                val userId = getUserIdFromToken()
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@post
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@post
                }
                val date = try {
                    call.parameters["date"]?.toLocalDate() ?: throw NullPointerException()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "날짜 형식이 잘못되었습니다.")
                    return@post
                }
                val schedulePerDate = call.receive<SchedulePerDate>()

                if (schedulePerDateService.read(scheduleId, date) != null) {
                    schedulePerDateService.update(scheduleId, date, schedulePerDate)
                } else {
                    schedulePerDateService.create(scheduleId, date, schedulePerDate)
                }

                scheduleService.updateTime(userId, scheduleId)
                call.respond(HttpStatusCode.OK)
            }

            delete("/schedule/{scheduleId}/{date}") {
                val userId = getUserIdFromToken()
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@delete
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@delete
                }
                val date = try {
                    call.parameters["date"]?.toLocalDate() ?: throw NullPointerException()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "날짜 형식이 잘못되었습니다.")
                    return@delete
                }

                schedulePerDateService.delete(scheduleId, date)
                call.respond(HttpStatusCode.OK)
            }

            post("/schedule/{scheduleId}/{date}/addPlace") {
                val userId = getUserIdFromToken()
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@post
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@post
                }
                val date = try {
                    call.parameters["date"]?.toLocalDate() ?: throw NullPointerException()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "날짜 형식이 잘못되었습니다.")
                    return@post
                }

                val schedulePerDate = schedulePerDateService.read(scheduleId, date)
                if (schedulePerDate == null) {
                    call.respond(HttpStatusCode.BadRequest, "해당 날짜에 해당하는 여행 계획이 없습니다.")
                    return@post
                }

                scheduleService.updateTime(userId, scheduleId)

                val place = call.receive<Place>()
                call.respond(placeService.insert(scheduleId, date, place))
            }

            post("/schedule/{scheduleId}/{date}/updatePlaceStayTimeHours/{placeId}") {
                val userId = getUserIdFromToken()
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@post
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@post
                }
                val date = try {
                    call.parameters["date"]?.toLocalDate() ?: throw NullPointerException()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "날짜 형식이 잘못되었습니다.")
                    return@post
                }
                val placeId = call.parameters["placeId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "여행지 ID 형식이 잘못되었습니다.")
                    return@post
                }

                val schedulePerDate = schedulePerDateService.read(scheduleId, date)
                if (schedulePerDate == null) {
                    call.respond(HttpStatusCode.BadRequest, "해당 날짜에 해당하는 여행 계획이 없습니다.")
                    return@post
                }

                val stayTimeHour = call.receive<ScheduleUpdateStayTimeHourRequest>().stayTimeHour
                placeService.updateStayTimeHour(scheduleId, date, placeId, stayTimeHour)

                scheduleService.updateTime(userId, scheduleId)
                call.respond(HttpStatusCode.OK)
            }

            delete("/schedule/{scheduleId}/{date}/removePlace/{placeId}") {
                val userId = getUserIdFromToken()
                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "스케줄 ID 형식이 잘못되었습니다.")
                    return@delete
                }
                val schedule = scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "존재하지 않는 스케줄입니다.")
                    return@delete
                }
                val date = try {
                    call.parameters["date"]?.toLocalDate() ?: throw NullPointerException()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "날짜 형식이 잘못되었습니다.")
                    return@delete
                }
                val placeId = call.parameters["placeId"]?.toIntOrNull() ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, "여행지 ID 형식이 잘못되었습니다.")
                    return@delete
                }

                val schedulePerDate = schedulePerDateService.read(scheduleId, date)
                if (schedulePerDate == null) {
                    call.respond(HttpStatusCode.BadRequest, "해당 날짜에 해당하는 여행 계획이 없습니다.")
                    return@delete
                }

                placeService.delete(scheduleId, date, placeId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
