package schemati.web

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
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
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.HttpMethod
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.*
import io.ktor.util.pipeline.PipelineContext
import kotlinx.html.p
import schemati.Schematics
import schemati.SchematicsException
import schemati.connector.Database
import java.util.*

const val discordApiBase = "https://discordapp.com/api/"

fun startWeb(port: Int, baseUri: String, database: Database, authConfig: AuthConfig, schems: Schematics): ApplicationEngine =
    embeddedServer(Netty, port = port, module = makeSchemsApp(database, baseUri, authConfig, schems)).start()

data class AuthConfig(val clientId: String, val clientSecret: String, val scopes: List<String>)

fun makeSchemsApp(networkDatabase: Database, baseUri: String, authConfig: AuthConfig, schems: Schematics): Application.() -> Unit = {
    val loginProvider = OAuthServerSettings.OAuth2ServerSettings(
        name = "Discord",
        authorizeUrl = "${discordApiBase}oauth2/authorize",
        accessTokenUrl = "${discordApiBase}oauth2/token",
        requestMethod = HttpMethod.Post,
        clientId = authConfig.clientId,
        clientSecret = authConfig.clientSecret,
        defaultScopes = authConfig.scopes
    )

    install(StatusPages) {
        exception<RedirectException> { cause ->
            call.respondRedirect(cause.destination, permanent = false)
        }
        exception<ErrorPageException> { cause ->
            call.respondHtmlTemplate(ErrorTemplate()) {
                errorContent {
                    p { +cause.content }
                }
            }
        }
        exception<LoggedInErrorException> { cause ->
            call.respondHtmlTemplate(LoggedInErrorTemplate()) {
                errorContent {
                    p { +cause.content }
                }
            }
        }
        // TODO: are these logged?
        exception<SchematicsException> { cause ->
            call.respondHtmlTemplate(LoggedInErrorTemplate()) {
                errorContent {
                    p { +(cause.message ?: "Something went wrong!") }
                }
            }
        }
    }

    install(DefaultHeaders)

    install(Sessions) {
        cookie<LoggedSession>("sessionId", storage = SessionStorageMemory())
    }

    install(Authentication) {
        oauth("discordOauth") {
            client = HttpClient(Apache)
            providerLookup = {
                loginProvider
            }
            urlProvider = { redirectUrl("/login", absoluteBaseUri = baseUri) }
        }
    }

    install(Routing) {
        get("/") {
            pageLanding(call)
        }
        get("/download") {
            pageDownload(call, schems.forPlayer(UUID.fromString("00000000-0000-0000-0000-000000000000")))
        }
        get("/logout") {
            pageLogout(call)
        }
        route("/schems") {
            handle {
                pageSchems(call, schems.forPlayer(user().userId), user())
            }
            post("/upload") {
                pageSchemsUpload(call, schems.forPlayer(user().userId))
            }
            get("/download") {
                pageSchemsDownload(call, schems.forPlayer(user().userId))
            }
            get("/rename") {
                pageSchemsRename(call, schems.forPlayer(user().userId))
            }
            get("/delete") {
                pageSchemsDelete(call, schems.forPlayer(user().userId))
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
                    pageLogin(call, networkDatabase)
                }
            }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.user() =
    call.sessions.get<LoggedSession>() ?: redirectTo("/")

class RedirectException(val destination: String) : Exception()
class ErrorPageException(val content: String) : Exception()
class LoggedInErrorException(val content: String) : Exception()

fun showErrorPage(message: String): Nothing = throw ErrorPageException(message)
fun showLoggedInErrorPage(message: String): Nothing = throw LoggedInErrorException(message)
fun redirectTo(where: String): Nothing = throw RedirectException(where)
