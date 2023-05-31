package com.bcgg.routing

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*

fun Application.configureRouting(
    database: Database
) {
    configureUserRouting(database)
    configureScheduleRouting(database)
    configureWebSocketRouting(database)
    configurePathfinderRouting(database)
}