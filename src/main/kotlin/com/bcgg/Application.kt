package com.bcgg

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.bcgg.plugins.*
import com.bcgg.routing.configureRouting
import com.bcgg.routing.configureUserRouting

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureSecurity()
    configureMonitoring()
    configureSerialization()
    val database = configureDatabases()
    configureSockets()
    configureRouting(database)
}
