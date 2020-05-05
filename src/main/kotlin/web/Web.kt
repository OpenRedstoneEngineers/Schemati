package schemati.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.HttpMethod
import io.ktor.request.*
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import io.ktor.sessions.*
import kotlinx.html.*
import schemati.Schemati

data class LoggedSession(val userId: String, val userName: String)

var discordApiBase = "https://discordapp.com/api/"

val oauthSection = Schemati.schematiConfig!!
    .getConfigurationSection("web")!!
    .getConfigurationSection("oauth")

var loginProvider = OAuthServerSettings.OAuth2ServerSettings(
    name = "Discord",
    authorizeUrl = "${discordApiBase}oauth2/authorize",
    accessTokenUrl = "${discordApiBase}oauth2/token",
    requestMethod = HttpMethod.Post,
    clientId = oauthSection!!.getString("clientId")!!,
    clientSecret = oauthSection.getString("clientSecret")!!,
    defaultScopes = oauthSection.getStringList("scopes")
)

fun Application.main() {

    install(StatusPages)
    install(DefaultHeaders)

    // Registers session management
    install(Sessions) {
        cookie<LoggedSession>("sessionId", storage = SessionStorageMemory())
    }

    // Registers authentication
    install(Authentication) {
        oauth("discordOauth") {
            client = HttpClient(Apache)
            providerLookup = {
                loginProvider
            }
            urlProvider = { redirectUrl("/login") }
        }
    }

    // Registers routes
    install(Routing) {
        get("/") {
            val session = call.sessions.get("sessionId")
            if (session != null && session is LoggedSession) {
                call.respondHtmlTemplate(MainTemplate()) {
                    content {
                        p { +"HI ${session.userName}:${session.userId}" }
                        p { +"Wanna "
                            a("/logout") { +"logout" }
                            +"?"
                        }
                    }
                }
            } else {
                call.respondHtmlTemplate(MainTemplate()) {
                    content {
                        p { +"Please "
                            a ("/login") { +"login with DiscOREd" }
                        }
                    }
                }
            }
        }
        get("/logout") {
            call.sessions.clear("sessionId")
            call.respondRedirect("/")
        }
        authenticate("discordOauth") {
            route("/login") {
                param("error") {
                    handle {
                        call.respondHtmlTemplate(MainTemplate()) {
                            content {
                                p { +"Login error(s):" }
                                for (e in call.parameters.getAll("error").orEmpty()) {
                                    p { +e }
                                }
                            }
                        }
                    }
                }
                handle {
                    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()

                    if (principal == null) {
                        call.respondHtmlTemplate(LandingTemplate()) { }
                        call.respondRedirect("/")
                        call.respondHtml {  }
                    }

                    val id = fetchId(principal!!.accessToken)

                    if (id == null) {
                        call.respondHtmlTemplate(MainTemplate()) {
                            content {
                                p { +"Login error: Invalid principal!" }
                            }
                        }
                    }

                    val user = Schemati.databaseManager?.getUuidFromDiscordId(id!!)

                    if (user == null) {
                        call.respondHtmlTemplate(MainTemplate()) {
                            content {
                                p { +"Login error: User is not linked!" }
                            }
                        }
                    }

                    call.sessions.set("sessionId", LoggedSession(user!!.mojangId, user.ign))
                    call.respondHtmlTemplate(MainTemplate()) {
                        content {
                            p {
                                +"You are now logged in as ${user.ign}. Now go back "
                                a("/") { +"home" }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun fetchId(accessToken: String) : String? {
    val json = HttpClient(Apache).get<String>("${discordApiBase}users/@me") {
        header("Authorization", "Bearer $accessToken")
    }
    val data = ObjectMapper().readValue(json, Map::class.java)
    return data["id"] as String?
}

private fun ApplicationCall.redirectUrl(path: String): String {
    val defaultPort = if (request.origin.scheme == "http") 80 else 443
    val hostPort = request.host() + request.port().let { port -> if (port == defaultPort) "" else ":$port" }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort$path"
}