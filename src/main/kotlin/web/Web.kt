package schemati.web

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.authenticate
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.HttpMethod
import io.ktor.routing.*
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import schemati.Schematics
import schemati.connector.Database

const val discordApiBase = "https://discordapp.com/api/"

fun startWeb(port: Int, database: Database, authConfig: AuthConfig, schems: Schematics): ApplicationEngine =
    embeddedServer(Netty, port = port, module = makeSchemsApp(database, authConfig, schems)).start()

data class AuthConfig(val clientId: String, val clientSecret: String, val scopes: List<String>)

fun makeSchemsApp(database: Database, authConfig: AuthConfig, schems: Schematics): Application.() -> Unit = {
    val loginProvider = OAuthServerSettings.OAuth2ServerSettings(
        name = "Discord",
        authorizeUrl = "${discordApiBase}oauth2/authorize",
        accessTokenUrl = "${discordApiBase}oauth2/token",
        requestMethod = HttpMethod.Post,
        clientId = authConfig.clientId,
        clientSecret = authConfig.clientSecret,
        defaultScopes = authConfig.scopes
    )

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
            pageLanding(call)
        }
        get("/logout") {
            pageLogout(call)
        }
        route("/schems") {
            handle {
                pageSchems(call, schems)
            }
            post("/upload") {
                pageSchemsUpload(call, schems)
            }
            get("/download") {
                pageSchemsDownload(call, schems)
            }
            get("/rename") {
                pageSchemsRename(call, schems)
            }
            get("/delete") {
                pageSchemsDelete(call, schems)
            }
        }
        authenticate("discordOauth") {
            route("/login") {
                param("error") {
                    handle {
                        pageError(call)
                    }
                }
                handle {
                    pageLogin(call, database)
                }
            }
        }
    }
}
