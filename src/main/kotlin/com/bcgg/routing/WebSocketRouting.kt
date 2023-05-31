package com.bcgg.routing

import com.bcgg.chat.Chat
import com.bcgg.chat.ChatPolymophicSerializer
import com.bcgg.schema.schedule.*
import com.bcgg.util.getUserIdFromToken
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import java.util.*

fun Application.configureWebSocketRouting(database: Database) {
    val scheduleService = ScheduleService(database)
    val schedulePerDateService = SchedulePerDateService(database)
    val placeService = PlaceService(database)
    val collaboratorService = CollaboratorService(database)

    routing {
        authenticate {
            val scheduleSession = Collections.synchronizedMap<Int, MutableSet<WebSocketServerSession>>(mutableMapOf())


            webSocket("/ws/{scheduleId}") {
                val userId = getUserIdFromToken()

                val scheduleId = call.parameters["scheduleId"]?.toIntOrNull() ?: kotlin.run {
                    close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "스케줄 ID 형식이 잘못되었습니다."))
                    return@webSocket
                }
                scheduleService.read(userId, scheduleId) ?: kotlin.run {
                    close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "존재하지 않는 스케줄입니다."))
                    return@webSocket
                }

                scheduleSession[scheduleId] = scheduleSession[scheduleId] ?: mutableSetOf()
                with(scheduleSession[scheduleId]!!) {
                    add(this@webSocket)
                    forEach { it.outgoing.send(Frame.Text(Json.encodeToString(Chat.ChatCount(count())))) }
                }

                suspend fun sendToClient(chat: Chat) {
                    with(scheduleSession[scheduleId]!!) {
                        forEach {
                            it.outgoing.send(Frame.Text(Json.encodeToString(chat)))
                        }
                    }
                }

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val chatString = frame.readText()
                            val chat = Json.decodeFromString(ChatPolymophicSerializer, chatString)

                            when (chat) {
                                is Chat.ChatCount -> {
                                    with(scheduleSession[scheduleId]!!) {
                                        forEach { if (it != this@webSocket) it.outgoing.send(Frame.Text(chatString)) }
                                    }
                                }

                                is Chat.ChatEndPlace -> {
                                    val (date, endPlace) = chat
                                    val schedulePerDate = schedulePerDateService.read(scheduleId, date)
                                    if (schedulePerDate == null) {
                                        schedulePerDateService.create(
                                            scheduleId,
                                            date,
                                            SchedulePerDate(endPlaceId = endPlace?.id)
                                        )
                                    } else {
                                        schedulePerDateService.update(
                                            scheduleId,
                                            date,
                                            schedulePerDate.copy(endPlaceId = endPlace?.id)
                                        )
                                    }

                                    with(scheduleSession[scheduleId]!!) {
                                        forEach { if (it != this@webSocket) it.outgoing.send(Frame.Text(chatString)) }
                                    }
                                }

                                is Chat.ChatEndTime -> {
                                    val (date, endTime) = chat
                                    val schedulePerDate = schedulePerDateService.read(scheduleId, date)

                                    if (schedulePerDate == null) {
                                        schedulePerDateService.create(
                                            scheduleId,
                                            date,
                                            SchedulePerDate(endHopeTime = endTime)
                                        )
                                    } else {
                                        schedulePerDateService.update(
                                            scheduleId,
                                            date,
                                            schedulePerDate.copy(endHopeTime = endTime)
                                        )
                                    }

                                    with(scheduleSession[scheduleId]!!) {
                                        forEach { if (it != this@webSocket) it.outgoing.send(Frame.Text(chatString)) }
                                    }
                                }

                                is Chat.ChatMapPosition -> {
                                    with(scheduleSession[scheduleId]!!) {
                                        forEach { if (it != this@webSocket) it.outgoing.send(Frame.Text(chatString)) }
                                    }
                                }

                                is Chat.ChatMealTimes -> {
                                    val (date, mealTimes) = chat
                                    val schedulePerDate = schedulePerDateService.read(scheduleId, date)

                                    if (schedulePerDate == null) {
                                        schedulePerDateService.create(
                                            scheduleId,
                                            date,
                                            SchedulePerDate(mealTimes = mealTimes)
                                        )
                                    } else {
                                        schedulePerDateService.update(
                                            scheduleId,
                                            date,
                                            schedulePerDate.copy(mealTimes = mealTimes)
                                        )
                                    }

                                    with(scheduleSession[scheduleId]!!) {
                                        forEach { if (it != this@webSocket) it.outgoing.send(Frame.Text(chatString)) }
                                    }
                                }

                                is Chat.ChatPoints -> {
                                    val (date, places) = chat
                                    val schedulePerDate = schedulePerDateService.read(scheduleId, date)
                                    val originalPlaces = placeService.read(scheduleId, date)

                                    if (schedulePerDate == null) {
                                        schedulePerDateService.create(
                                            scheduleId,
                                            date,
                                            SchedulePerDate()
                                        )
                                    }

                                    places.forEach { place ->
                                        //db에 저장된 장소 업데이트
                                        val find = originalPlaces.find { originalPlace -> place == originalPlace }

                                        if (find != null) {
                                            placeService.updateStayTimeHour(
                                                scheduleId,
                                                date,
                                                find.id,
                                                place.stayTimeHour
                                            )
                                        }
                                    }

                                    val newPlaces = placeService.read(scheduleId, date)

                                    with(scheduleSession[scheduleId]!!) {
                                        forEach {
                                            it.outgoing.send(
                                                Frame.Text(Json.encodeToString(Chat.ChatPoints(date, newPlaces)))
                                            )
                                        }
                                    }
                                }

                                is Chat.ChatAddPoint -> {
                                    val (date, place) = chat
                                    val schedulePerDate = schedulePerDateService.read(scheduleId, date)

                                    if (schedulePerDate == null) {
                                        schedulePerDateService.create(
                                            scheduleId,
                                            date,
                                            SchedulePerDate()
                                        )
                                    }

                                    placeService.insert(scheduleId, date, place)

                                    val places = placeService.read(scheduleId, date)
                                    val startPlaceId = schedulePerDate?.startPlaceId
                                    val endPlaceId = schedulePerDate?.endPlaceId

                                    //처음 아이템 추가 시 시작/끝 지점 추가
                                    if (schedulePerDate != null && places.isNotEmpty() && (startPlaceId == null || endPlaceId == null)) {
                                        val resultPlaces = placeService.read(scheduleId, date)
                                        if (resultPlaces.isNotEmpty()) {
                                            schedulePerDateService.update(
                                                scheduleId, date, schedulePerDate.copy(
                                                    startPlaceId = resultPlaces.first().id,
                                                    endPlaceId = resultPlaces.first().id
                                                )
                                            )

                                            sendToClient(
                                                Chat.ChatStartPlace(
                                                    date = date,
                                                    startPlace = resultPlaces.first()
                                                )
                                            )

                                            sendToClient(
                                                Chat.ChatEndPlace(
                                                    date = date,
                                                    endPlace = resultPlaces.first()
                                                )
                                            )
                                        }
                                    }

                                    sendToClient(Chat.ChatPoints(date, places))
                                }

                                is Chat.ChatRemovePoint -> {
                                    val (date, place) = chat
                                    var schedulePerDate = schedulePerDateService.read(scheduleId, date)
                                    val originalPlaces = placeService.read(scheduleId, date)

                                    if (schedulePerDate == null) {
                                        schedulePerDate = SchedulePerDate()
                                        schedulePerDateService.create(
                                            scheduleId,
                                            date,
                                            schedulePerDate
                                        )
                                    }

                                    val startPlaceId = schedulePerDate.startPlaceId
                                    val endPlaceId = schedulePerDate.endPlaceId

                                    //db에 저장되어 있지만 사용자가 삭제한 장소 삭제
                                    //삭제할 아이템이 시작/끝 지점 선택되었을 경우 이동 및 제거
                                    val removePlace = originalPlaces.find { original -> original == place }
                                    if (removePlace != null) {
                                        val replacePlace = originalPlaces.filter { it != removePlace }.firstOrNull()

                                        if (removePlace.id == startPlaceId) {
                                            schedulePerDate = schedulePerDate.copy(
                                                startPlaceId = replacePlace?.id
                                            )

                                            sendToClient(
                                                Chat.ChatStartPlace(
                                                    date = date,
                                                    startPlace = replacePlace
                                                )
                                            )
                                        }

                                        if(removePlace.id == endPlaceId) {
                                            schedulePerDate = schedulePerDate.copy(
                                                endPlaceId = replacePlace?.id
                                            )

                                            sendToClient(
                                                Chat.ChatEndPlace(
                                                    date = date,
                                                    endPlace = replacePlace
                                                )
                                            )
                                        }

                                        schedulePerDateService.update(scheduleId, date, schedulePerDate)
                                        placeService.delete(scheduleId, date, removePlace.id)
                                    }

                                    val places = placeService.read(scheduleId, date)
                                    sendToClient(Chat.ChatPoints(date, places))
                                }

                                is Chat.ChatStartPlace -> {
                                    val (date, startPlace) = chat
                                    val schedulePerDate = schedulePerDateService.read(scheduleId, date)

                                    if (schedulePerDate == null) {
                                        schedulePerDateService.create(
                                            scheduleId,
                                            date,
                                            SchedulePerDate(startPlaceId = startPlace?.id)
                                        )
                                    } else {
                                        schedulePerDateService.update(
                                            scheduleId,
                                            date,
                                            schedulePerDate.copy(startPlaceId = startPlace?.id)
                                        )
                                    }

                                    with(scheduleSession[scheduleId]!!) {
                                        forEach { it.outgoing.send(Frame.Text(chatString)) }
                                    }
                                }

                                is Chat.ChatStartTime -> {
                                    val (date, startTime) = chat
                                    val schedulePerDate = schedulePerDateService.read(scheduleId, date)

                                    if (schedulePerDate == null) {
                                        schedulePerDateService.create(
                                            scheduleId,
                                            date,
                                            SchedulePerDate(startTime = startTime)
                                        )
                                    } else {
                                        schedulePerDateService.update(
                                            scheduleId,
                                            date,
                                            schedulePerDate.copy(startTime = startTime)
                                        )
                                    }

                                    with(scheduleSession[scheduleId]!!) {
                                        forEach { it.outgoing.send(Frame.Text(chatString)) }
                                    }
                                }

                                is Chat.ChatTitle -> {
                                    val (title) = chat
                                    scheduleService.updateScheduleName(userId, scheduleId, title)

                                    with(scheduleSession[scheduleId]!!) {
                                        forEach { it.send(Frame.Text(chatString)) }
                                    }
                                }
                            }
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    with(scheduleSession[scheduleId]!!) {
                        remove(this@webSocket)
                        forEach { it.sendSerialized(Chat.ChatCount(count())) }
                    }
                }
            }
        }
    }
}

//{"date":"2023-05-29","points":[{"id":14,"scheduleId":14,"kakaoPlaceId":"10808261","name":"제주국제공항","address":"제주특별자치도 제주시 용담2동 2002","lat":33.5070789578184,"lng":126.492769004244,"classification":"Travel","stayTimeHour":4}]}
//{"date":"2023-05-29","points":[{"id":14,"scheduleId":14,"date":"2023-05-29","kakaoPlaceId":"10808261","name":"제주국제공항","address":"제주특별자치도 제주시 용담2동 2002","lat":33.5070789578184,"lng":126.492769004244,"classification":"Travel","stayTimeHour":4}]}

/*

 */

/*
 */