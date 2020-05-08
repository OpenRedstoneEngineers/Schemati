package schemati.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.features.origin
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.sessions.CurrentSession
import schemati.Schemati
import java.util.*

data class LoggedSession(val userId: UUID, val userName: String)

fun getSessionUser(session: CurrentSession): LoggedSession? =
    session.get("sessionId") as? LoggedSession

suspend fun fetchId(accessToken: String) : String? {
    val json = HttpClient(Apache).get<String>("${discordApiBase}users/@me") {
        header("Authorization", "Bearer $accessToken")
    }
    val data = ObjectMapper().readValue(json, Map::class.java)
    return data["id"] as? String
}

fun ApplicationCall.redirectUrl(path: String): String {
    val defaultPort = if (request.origin.scheme == "http") 80 else 443
    val hostPort = request.host() + request.port().let { port -> if (port == defaultPort) "" else ":$port" }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort$path"
}
