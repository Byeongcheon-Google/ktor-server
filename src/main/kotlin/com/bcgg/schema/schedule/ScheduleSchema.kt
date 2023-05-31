package com.bcgg.schema.schedule

import com.bcgg.schema.Service
import com.bcgg.schema.user.UserService
import com.bcgg.schema.user.userIdInTable
import com.bcgg.util.dateTimeNow
import com.bcgg.util.dbQuery
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.format.DateTimeFormatter

@Serializable
data class Schedule(
    val id: Int = -1,
    val ownerId: String = "",
    val name: String,
    val createdAt: LocalDateTime = dateTimeNow,
    val updatedAt: LocalDateTime = dateTimeNow
)

class ScheduleService(
    private val database: Database
) : Service {
    object Schedules : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 1024)
        val ownerId = userIdInTable.references(UserService.Users.id)
        val createdAt = varchar("createdAt", 128)
        val updatedAt = varchar("updatedAt", 128)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Schedules)
        }
    }

    suspend fun create(ownerId: String, schedule: Schedule): Int = dbQuery {
        Schedules.insert {
            it[Schedules.ownerId] = ownerId
            it[Schedules.name] = schedule.name
            it[Schedules.createdAt] = DateTimeFormatter.ISO_DATE_TIME.format(dateTimeNow.toJavaLocalDateTime())
            it[Schedules.updatedAt] = DateTimeFormatter.ISO_DATE_TIME.format(dateTimeNow.toJavaLocalDateTime())
        }[Schedules.id]
    }

    suspend fun updateScheduleName(userId: String, scheduleId: Int, scheduleName: String) = dbQuery {
        Schedules
            .join(CollaboratorService.Collaborators, JoinType.LEFT)
            .update({
                ((Schedules.ownerId eq userId) or (CollaboratorService.Collaborators.userId eq userId)) and (Schedules.id eq scheduleId)
            }) {
                it[Schedules.name] = scheduleName
                it[Schedules.updatedAt] = DateTimeFormatter.ISO_DATE_TIME.format(dateTimeNow.toJavaLocalDateTime())
            }
    }

    suspend fun updateTime(ownerId: String, scheduleId: Int) = dbQuery {
        Schedules
            .join(CollaboratorService.Collaborators, JoinType.LEFT)
            .update({
                (Schedules.ownerId eq ownerId) and (Schedules.id eq scheduleId)
            }) {
                it[Schedules.updatedAt] = DateTimeFormatter.ISO_DATE_TIME.format(dateTimeNow.toJavaLocalDateTime())
            }
    }

    suspend fun read(userId: String): List<Schedule> = dbQuery {
        Schedules
            .join(CollaboratorService.Collaborators, JoinType.LEFT)
            .select { ((Schedules.ownerId eq userId) or (CollaboratorService.Collaborators.userId eq userId)) }
            .map {
                Schedule(
                    it[Schedules.id],
                    it[Schedules.ownerId],
                    it[Schedules.name],
                    it[Schedules.createdAt].toLocalDateTime(),
                    it[Schedules.updatedAt].toLocalDateTime()
                )
            }
            .distinctBy { it.id }
    }

    suspend fun read(userId: String, scheduleId: Int): Schedule? = dbQuery {
        Schedules
            .join(CollaboratorService.Collaborators, JoinType.LEFT)
            .select { ((Schedules.ownerId eq userId) or (CollaboratorService.Collaborators.userId eq userId)) and (Schedules.id eq scheduleId) }
            .map {
                Schedule(
                    it[Schedules.id],
                    it[Schedules.ownerId],
                    it[Schedules.name],
                    it[Schedules.createdAt].toLocalDateTime(),
                    it[Schedules.updatedAt].toLocalDateTime()
                )
            }
            .distinctBy { it.id }
            .firstOrNull()
    }

    suspend fun delete(ownerId: String, scheduleId: Int) {
        dbQuery {
            Schedules.deleteWhere { (Schedules.ownerId eq ownerId) and (id eq scheduleId) }
        }
    }
}