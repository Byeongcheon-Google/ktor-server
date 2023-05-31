package com.bcgg.routing

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bcgg.schema.user.User
import com.bcgg.schema.user.UserService
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import java.util.*

fun Application.configureUserRouting(
    database: Database
) {
    val userService = UserService(database)

    val jwtAudience = this@configureUserRouting.environment.config.property("jwt.audience").getString()
    val jwtIssuer = this@configureUserRouting.environment.config.property("jwt.domain").getString()
    val jwtSecret = this@configureUserRouting.environment.config.property("jwt.secret").getString()

    routing {
        // Create user
        post("/user") {
            val user = call.receive<User>()
            if(userService.read(user.id) != null) {
                call.respond(HttpStatusCode.BadRequest, "중복된 ID 입니다.")
            }
            try {
                val id = userService.create(user)
                call.respond(HttpStatusCode.Created, id)
            } catch (e: ExposedSQLException) {
                call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
            }
        }
        post("/user/check/{userId}") {
            val userId = call.parameters["userId"] ?: kotlin.run {
                call.respond(HttpStatusCode.NotFound, "잘못된 사용자 ID 형식입니다.")
                return@post
            }

            call.respond(userService.read(userId) != null)
        }
        // Update user
        put("/user/{id}") {
            val id = call.parameters["id"] ?: kotlin.run {
                call.respond(HttpStatusCode.NotFound, "존재하지 않는 사용자입니다.")
                return@put
            }
            val user = call.receive<User>()
            userService.update(id, user)
            call.respond(HttpStatusCode.OK)
        }
        // Delete user
        delete("/user/{id}") {
            val id = call.parameters["id"] ?: kotlin.run {
                call.respond(HttpStatusCode.NotFound, "존재하지 않는 사용자입니다.")
                return@delete
            }
            userService.delete(id)
            call.respond(HttpStatusCode.OK)
        }

        post("/user/login") {
            val callUser = call.receive<User>()
            val user = userService.read(callUser.id) ?: kotlin.run {
                call.respond(HttpStatusCode.NotFound, "존재하지 않는 사용자입니다.")
                return@post
            }

            if (user.password != callUser.password) {
                call.respond(HttpStatusCode.Unauthorized, "비밀번호가 다릅니다.")
            }

            val token = JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withClaim("id", user.id)
                .withExpiresAt(Date(System.currentTimeMillis() + 259200000))
                .sign(Algorithm.HMAC256(jwtSecret))

            call.respond(token)
        }

        authenticate {
            get("/user") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("id").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText(username)
            }
        }
    }
}
