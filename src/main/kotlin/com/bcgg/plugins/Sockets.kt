package com.bcgg.plugins

import io.ktor.network.sockets.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.LinkedHashSet

fun Application.configureSockets() {
    var count = 0

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(120)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = Collections.synchronizedSet<WebSocketSession>(LinkedHashSet())

        webSocket("/ws") { // websocketSession
            connections += this

            connections.forEach { it.send(Frame.Text(connections.count().toString())) }

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        outgoing.send(Frame.Text("YOU SAID: $text"))
                        if (text.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                    }
                }
            } catch (e: Exception) {

            } finally {
                connections -= this
                connections.forEach { it.send(Frame.Text(connections.count().toString())) }
            }
        }
    }
}
