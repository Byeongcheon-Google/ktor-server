package com.bcgg.schema.schedule

import com.bcgg.schema.Service
import com.bcgg.util.dateNow
import com.bcgg.util.dbQuery
import com.bcgg.util.toDbString
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class Place(
    val id: Int = -1,
    val scheduleId: Int = -1,
    val date: LocalDate,
    val kakaoPlaceId: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val classification: Classification,
    val stayTimeHour: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Place

        if (scheduleId != other.scheduleId) return false
        if (date != other.date) return false
        if (kakaoPlaceId != other.kakaoPlaceId) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (lat != other.lat) return false
        if (lng != other.lng) return false
        if (classification != other.classification) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scheduleId
        result = 31 * result + date.hashCode()
        result = 31 * result + kakaoPlaceId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + lat.hashCode()
        result = 31 * result + lng.hashCode()
        result = 31 * result + classification.hashCode()
        return result
    }
}

enum class Classification {
    Travel, House, Food;
}

class PlaceService(
    private val database: Database
) : Service {
    object Places : Table() {
        val id = integer("id").autoIncrement()
        val scheduleId = integer("schedule_id").references(ScheduleService.Schedules.id)
        val date = varchar("date", length = 100)
        val kakaoPlaceId = varchar("place_id", 32)
        val name = varchar("name", 1024)
        val address = varchar("address", 1024)
        val lat = double("lat")
        val lng = double("lng")
        val classification = char("classification")
        val stayTimeHour = integer("stay_time_hour")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Places)
        }
    }

    private fun Char.toClassification() = Classification.values().find { it.name.first() == this }
        ?: throw IllegalArgumentException("Wrong classification char")

    suspend fun insert(scheduleId: Int, date: LocalDate, place: Place): Int = dbQuery {
        Places.insert {
            it[Places.scheduleId] = scheduleId
            it[Places.date] = date.toDbString()
            it[Places.kakaoPlaceId] = place.kakaoPlaceId
            it[Places.name] = place.name
            it[Places.address] = place.address
            it[Places.lat] = place.lat
            it[Places.lng] = place.lng
            it[Places.classification] = place.classification.name.first()
            it[Places.stayTimeHour] = place.stayTimeHour
        }
    }[Places.id]

    suspend fun updateStayTimeHour(scheduleId: Int, date: LocalDate, placeId: Int, stayTimeHour: Int) {
        dbQuery {
            Places.update({
                (Places.scheduleId eq scheduleId) and (Places.date eq date.toDbString()) and (Places.id eq placeId)
            }) {
                it[Places.stayTimeHour] = stayTimeHour
            }
        }
    }

    suspend fun delete(scheduleId: Int, date: LocalDate, placeId: Int) {
        dbQuery {
            Places.deleteWhere { (Places.scheduleId eq scheduleId) and (Places.date eq date.toDbString()) and (Places.id eq placeId) }
        }
    }

    suspend fun read(scheduleId: Int, date: LocalDate) = dbQuery {
        Places.select {
            (Places.scheduleId eq scheduleId) and (Places.date eq date.toDbString())
        }
            .map {
                Place(
                    id = it[Places.id],
                    scheduleId = it[Places.scheduleId],
                    date = it[Places.date].toLocalDate(),
                    kakaoPlaceId = it[Places.kakaoPlaceId],
                    name = it[Places.name],
                    address = it[Places.address],
                    lat = it[Places.lat],
                    lng = it[Places.lng],
                    classification = it[Places.classification].toClassification(),
                    stayTimeHour = it[Places.stayTimeHour],
                )
            }
    }
}