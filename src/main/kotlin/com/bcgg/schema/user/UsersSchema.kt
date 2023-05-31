package com.bcgg.schema.user

import com.bcgg.schema.Service
import com.bcgg.util.dbQuery
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

internal val Table.userIdInTable: Column<String>
    get() = varchar("userId", length = 50)

@Serializable
data class User(val id: String, val password: String = "")

class UserService(private val database: Database) : Service {
    object Users : Table() {
        val id = userIdInTable
        val password = varchar("password", 64)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: User): String = dbQuery {
        Users.insert {
            it[id] = user.id
            it[password] = user.password
        }[Users.id]
    }

    suspend fun read(id: String): User? {
        return dbQuery {
            Users.select { Users.id eq id }
                .map { User(it[Users.id], it[Users.password]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: String, user: User) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[Users.id] = user.id
                it[password] = user.password
            }
        }
    }

    suspend fun delete(id: String) {
        dbQuery {
            Users.deleteWhere { Users.id eq id }
        }
    }
}