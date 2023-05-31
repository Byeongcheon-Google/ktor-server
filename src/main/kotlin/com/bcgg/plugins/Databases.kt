package com.bcgg.plugins

import org.jetbrains.exposed.sql.*
import io.ktor.server.application.*

fun Application.configureDatabases(): Database {
    return Database.connect(
        url = "jdbc:mysql://localhost:3306/bcggktorserver",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "bcggktorserver",
        password = "bcgg"
    )
}
