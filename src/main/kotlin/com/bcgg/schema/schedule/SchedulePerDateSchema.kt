package com.bcgg.schema.schedule

import com.bcgg.schema.Service
import com.bcgg.util.*
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class SchedulePerDate(
    val id: Int = -1,
    val scheduleId: Int = -1,
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endHopeTime: LocalTime? = null,
    val mealTimes: List<LocalTime>? = null,
    val startPlaceId: Int? = null,
    val endPlaceId: Int? = null
)

class SchedulePerDateService(
    private val database: Database
) : Service {

    object SchedulePerDates : Table() {
        val id = integer("id").autoIncrement()
        val scheduleId = integer("schedule_id").references(ScheduleService.Schedules.id)
        val date = varchar("date", length = 100)
        val startTime = varchar("start_time", length = 100)
        val endHopeTime = varchar("end_hope_time", length = 100)
        val mealTimes = varchar("meal_times", length = 1024)
        val startPlaceId = integer("start_place_id").references(PlaceService.Places.id).nullable()
        val endPlaceId = integer("end_place_id").references(PlaceService.Places.id).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(SchedulePerDates)
        }
    }

    suspend fun create(scheduleId: Int, date: LocalDate, schedulePerDate: SchedulePerDate): Int = dbQuery {
        SchedulePerDates.insert {
            it[SchedulePerDates.scheduleId] = scheduleId
            it[SchedulePerDates.date] = date.toDbString()
            it[SchedulePerDates.startTime] = (schedulePerDate.startTime ?: LocalTime(9, 0)).toDbString()
            it[SchedulePerDates.endHopeTime] = (schedulePerDate.endHopeTime ?: LocalTime(18, 0)).toDbString()
            it[SchedulePerDates.mealTimes] =
                (schedulePerDate.mealTimes ?: emptyList()).joinToString(" ") { it.toDbString() }
            it[SchedulePerDates.startPlaceId] = schedulePerDate.startPlaceId
            it[SchedulePerDates.endPlaceId] = schedulePerDate.endPlaceId
        }[SchedulePerDates.id]
    }

    suspend fun update(scheduleId: Int, date: LocalDate, schedulePerDate: SchedulePerDate) = dbQuery {
        SchedulePerDates.update(
            {
                (SchedulePerDates.scheduleId eq scheduleId) and
                        (SchedulePerDates.date eq date.toDbString())
            }
        ) {
            schedulePerDate.startTime?.toDbString()?.let { value -> it[SchedulePerDates.startTime] = value }
            schedulePerDate.endHopeTime?.toDbString()?.let { value -> it[SchedulePerDates.endHopeTime] = value }
            schedulePerDate.mealTimes?.joinToString(" ") { it.toDbString() }?.let { value -> it[mealTimes] = value }
            it[SchedulePerDates.startPlaceId] = schedulePerDate.startPlaceId
            it[SchedulePerDates.endPlaceId] = schedulePerDate.endPlaceId
        }
    }

    suspend fun delete(scheduleId: Int, date: LocalDate) = dbQuery {
        SchedulePerDates.deleteWhere {
            (SchedulePerDates.scheduleId eq scheduleId) and
                    (SchedulePerDates.date eq date.toDbString())
        }
    }

    suspend fun readList(scheduleId: Int) = dbQuery {
        SchedulePerDates
            .select {
                (SchedulePerDates.scheduleId eq scheduleId)
            }
            .map {
                SchedulePerDate(
                    id = it[SchedulePerDates.id],
                    scheduleId = it[SchedulePerDates.scheduleId],
                    date = it[SchedulePerDates.date].toLocalDate(),
                    startTime = it[SchedulePerDates.startTime].appendSecondAndToLocalTime(),
                    endHopeTime = it[SchedulePerDates.endHopeTime].appendSecondAndToLocalTime(),
                    mealTimes = kotlin.run {
                        val mealtimes = it[SchedulePerDates.mealTimes].split(" ")
                        return@run if (mealtimes[0] == "") emptyList()
                        else mealtimes.map { it.appendSecondAndToLocalTime() }
                    },
                    startPlaceId = it[SchedulePerDates.startPlaceId],
                    endPlaceId = it[SchedulePerDates.endPlaceId]
                )
            }
    }

    suspend fun read(scheduleId: Int, date: LocalDate) = dbQuery {
        SchedulePerDates
            .select {
                (SchedulePerDates.scheduleId eq scheduleId) and
                        (SchedulePerDates.date eq date.toDbString())
            }
            .map {
                SchedulePerDate(
                    id = it[SchedulePerDates.id],
                    scheduleId = it[SchedulePerDates.scheduleId],
                    date = it[SchedulePerDates.date].toLocalDate(),
                    startTime = it[SchedulePerDates.startTime].appendSecondAndToLocalTime(),
                    endHopeTime = it[SchedulePerDates.endHopeTime].appendSecondAndToLocalTime(),
                    mealTimes = kotlin.run {
                        val mealtimes = it[SchedulePerDates.mealTimes].split(" ")
                        return@run if (mealtimes[0] == "") emptyList()
                        else mealtimes.map { it.appendSecondAndToLocalTime() }
                    },
                    startPlaceId = it[SchedulePerDates.startPlaceId],
                    endPlaceId = it[SchedulePerDates.endPlaceId]
                )
            }
            .singleOrNull()
    }

    suspend fun exist(scheduleId: Int, date: LocalDate) = dbQuery {
        SchedulePerDates
            .select {
                (SchedulePerDates.scheduleId eq scheduleId) and
                        (SchedulePerDates.date eq date.toDbString())
            }
            .count() > 0
    }
}