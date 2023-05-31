package com.bcgg.schema.schedule

import com.bcgg.schema.Service
import com.bcgg.schema.schedule.PlaceService.Places.references
import com.bcgg.schema.user.UserService
import com.bcgg.schema.user.userIdInTable
import com.bcgg.util.dbQuery
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class Collaborator(
    val scheduleId: Int,
    val userId: String
)

class CollaboratorService(
    private val database: Database
) : Service {
    object Collaborators : Table() {
        val scheduleId = integer("scheduleId").references(ScheduleService.Schedules.id)
        val userId = userIdInTable.references(UserService.Users.id)

        override val primaryKey = PrimaryKey(scheduleId, userId)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Collaborators)
        }
    }

    suspend fun addCollaborator(scheduleId: Int, userId: String) = dbQuery {
        Collaborators.insert {
            it[Collaborators.scheduleId] = scheduleId
            it[Collaborators.userId] = userId
        }
    }

    suspend fun removeCollaborator(scheduleId: Int, userId: String) = dbQuery {
        Collaborators.deleteWhere {
            (Collaborators.scheduleId eq scheduleId) and (Collaborators.userId eq userId)
        }
    }

    suspend fun collaborators(scheduleId: Int) = dbQuery {
        Collaborators.select { Collaborators.scheduleId eq scheduleId }
            .map { Collaborator(it[Collaborators.scheduleId], it[Collaborators.userId]) }
    }
}