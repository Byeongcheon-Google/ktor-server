package com.bcgg.util

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*

fun PipelineContext<Unit, ApplicationCall>.getUserIdFromToken(): String {
    val principal = call.principal<JWTPrincipal>()
    return principal!!.payload.getClaim("id").asString()
}

fun DefaultWebSocketServerSession.getUserIdFromToken(): String {
    val principal = call.principal<JWTPrincipal>()
    return principal!!.payload.getClaim("id").asString()
}