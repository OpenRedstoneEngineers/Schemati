package org.openredstone.schemati.web

import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.p
import org.openredstone.schemati.core.Schematics
import org.openredstone.schemati.core.SchematicsException
import java.io.File
import java.util.*

const val discordApiBase = "https://discordapp.com/api/"

fun startWeb(port: Int, baseUri: String, database: Database, authConfig: AuthConfig, schems: Schematics, sessionDir: File): EmbeddedServer<*, *> =
    embeddedServer(CIO, port = port, module = makeSchemsApp(database, baseUri, authConfig, schems, sessionDir)).start(wait = false)

data class AuthConfig(val clientId: String, val clientSecret: String, val scopes: List<String>)

fun makeSchemsApp(networkDatabase: Database, baseUri: String, authConfig: AuthConfig, schems: Schematics, sessionDir: File): Application.() -> Unit = {
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
        exception<RedirectException> { call, cause ->
            call.respondRedirect(cause.destination, permanent = false)
        }
        exception<ErrorPageException> { call, cause ->
            call.respondHtmlTemplate(ErrorTemplate()) {
                errorContent {
                    p { +cause.content }
                }
            }
        }
        exception<SchematicUploadException> { call, cause ->
            call.respondHtmlTemplate(UploadErrorTemplate()) {
                errorContent {
                    p { +cause.content}
                }
            }
        }
        exception<LoggedInErrorException> { call, cause ->
            call.respondHtmlTemplate(LoggedInErrorTemplate()) {
                errorContent {
                    p { +cause.content }
                }
            }
        }
        // TODO: are these logged?
        exception<SchematicsException> { call, cause ->
            call.respondHtmlTemplate(LoggedInErrorTemplate()) {
                errorContent {
                    p { +(cause.message ?: "Something went wrong!") }
                }
            }
        }
    }

    install(DefaultHeaders)
    install(ConditionalHeaders)

    deleteExpiredSessions(sessionDir)
    install(Sessions) {
        cookie<LoggedSession>("sessionId", storage = directorySessionStorage(sessionDir)) {
            cookie.maxAgeInSeconds = SESSION_MAX_AGE
        }
    }

    install(Authentication) {
        oauth("discordOauth") {
            client = HttpClient(Apache5)
            providerLookup = {
                loginProvider
            }
            urlProvider = { redirectUrl("/login", absoluteBaseUri = baseUri) }
        }
    }

    routing {
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

private const val SESSION_MAX_AGE = 30L * 24 * 60 * 60

// Ktor never deletes session files, but rewrites one on each request, so a file older
// than the cookie's lifetime is an expired session. Directories go once they're empty.
private fun deleteExpiredSessions(sessionDir: File) {
    val cutoff = System.currentTimeMillis() - SESSION_MAX_AGE * 1000
    sessionDir.walkBottomUp().filter { it != sessionDir && (it.isDirectory || it.lastModified() < cutoff) }.forEach { it.delete() }
}

private fun RoutingContext.user() =
    call.sessions.get<LoggedSession>() ?: redirectTo("/")

class SchematicUploadException(val content: String) : Exception()
class RedirectException(val destination: String) : Exception()
class ErrorPageException(val content: String) : Exception()
class LoggedInErrorException(val content: String) : Exception()

fun showErrorPage(message: String): Nothing = throw ErrorPageException(message)
fun showUploadErrorPage(message: String): Nothing = throw SchematicUploadException(message)
fun showLoggedInErrorPage(message: String): Nothing = throw LoggedInErrorException(message)
fun redirectTo(where: String): Nothing = throw RedirectException(where)
