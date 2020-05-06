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
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.*
import io.ktor.response.header
import io.ktor.response.respondFile
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.sessions.*
import kotlinx.html.*
import schemati.Schemati
import java.io.File

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
                        p { +"Hello, ${session.userName}" }
                        p { +"View your "
                            a("/schems") {
                                span {
                                    style = "color:red;"
                                    +"schematics"
                                }
                            }
                            +"?"
                        }
                    }
                }
            } else {
                call.respondHtmlTemplate(LandingTemplate()) { }
            }
        }
        get("/logout") {
            call.sessions.clear("sessionId")
            call.respondRedirect("/")
        }
        route("/schems") {
            handle {
                val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
                val user = getSessionUser(call.sessions) ?: call.respondText("riop")

                call.respondHtmlTemplate(ListingTemplate()) {
                    username {
                        +(user as LoggedSession).userName
                    }
                    File(folderPath.toString()).listFiles().forEach {file ->
                        file {
                            +file.name
                            div {
                                classes = setOf("fileActions")
                                a("/schems/rename/?file=${file.name}") {
                                    i {
                                        classes = setOf("fas fa-edit")
                                    }
                                }
                                +" "
                                a("/schems/delete/?file=${file.name}") {
                                    i {
                                        classes = setOf("fas fa-trash")
                                    }
                                }
                                +" "
                                a("/schems/download/?file=${file.name}") {
                                    i {
                                        classes = setOf("fas fa-download")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            post("/upload") {
                // TODO: Implement path+file validation
                val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val name = part.originalFileName!!
                        val file = File("${folderPath}${name}")
                        part.streamProvider().use {inputStream ->
                            file.outputStream().buffered().use {
                                inputStream.copyTo(it)
                            }
                        }
                    }
                }
                call.respondRedirect("/schems")
            }
            get("/download") {
                val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
                val filename = call.parameters["file"]
                call.response.header(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName, filename!!).toString())
                call.respondFile(File("${folderPath}${filename}"))
            }
            get("/rename") {
                val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
                val filename = call.parameters["file"]
                if (call.parameters.contains("newname")) {
                    val newname = call.parameters["newname"]
                    // TODO: Implement path+file validation
                    File("${folderPath}${filename}").renameTo(File("${folderPath}${newname}"))
                    call.respondRedirect("/schems")
                } else {
                    call.respondHtmlTemplate(MainTemplate()) {
                        content {
                            // TODO: Style this
                            p {
                                +"Rename "
                                span {
                                    style = "color:red;"
                                    +filename!!
                                }
                                +"?"
                            }
                            form(action = "/schems/rename/") {
                                input(type = InputType.text, name = "file") {
                                    hidden=true
                                    value=filename!!
                                }
                                input(type = InputType.text, name = "newname") {
                                    value=filename!!
                                    placeholder="New Name"
                                }
                                input(type = InputType.submit) { value = "Rename" }
                            }
                            form(action ="/schems") {
                                input(type = InputType.submit) { value = "Cancel" }
                            }
                        }
                    }
                }
            }
            get("/delete") {
                val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
                val filename = call.parameters["file"]
                if (call.parameters.contains("confirm")) {
                    // TODO: Implement path+file validation
                    File("${folderPath}${filename}").delete()
                    call.respondRedirect("/schems")
                } else {
                    call.respondHtmlTemplate(MainTemplate()) {
                        content {
                            p {
                                +"Delete "
                                span {
                                    style = "color:red;"
                                    +filename!!
                                }
                                +"?"
                            }
                            form(action = "/schems/delete/") {
                                style = "display:inline-block;float:left;padding-left:60px;"
                                input(type = InputType.text, name = "file") {
                                    hidden=true
                                    value = filename!!
                                }
                                input(type = InputType.submit, name = "confirm") {
                                    style = "min-width:100px;"
                                    classes = setOf("generalButton")
                                    value = "Confirm" }
                            }
                            form(action ="/schems") {
                                style = "display:inline-block;float:right;padding-right:60px;"
                                input(type = InputType.submit) {
                                    style = "min-width:100px;"
                                    classes = setOf("generalButton")
                                    value = "Cancel"
                                }
                            }
                        }
                    }
                }
            }
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
                    call.respondRedirect("/")
                }
            }
        }
    }
}

private fun getSessionUser(session: CurrentSession): LoggedSession? {
    val sessionId = session.get("sessionId")
    if (sessionId != null && sessionId is LoggedSession) {
        return sessionId
    }
    return null
}

private fun getFolderPath(session: CurrentSession): String? {
    val sessionId = session.get("sessionId")
    if (sessionId != null && sessionId is LoggedSession) {
        return Schemati.schematiConfig?.getString("schematicsDirectory") + sessionId.userId + "/"
    }
    return null
}

private fun isValidSession(session: CurrentSession): Boolean {
    val sessionId = session.get("sessionId")
    return sessionId != null && sessionId is LoggedSession
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