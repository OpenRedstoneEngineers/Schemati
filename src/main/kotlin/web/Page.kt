package schemati.web

import io.ktor.application.ApplicationCall
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authentication
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.header
import io.ktor.response.respondFile
import io.ktor.response.respondRedirect
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import kotlinx.html.p
import schemati.PlayerSchematics
import schemati.connector.Database
import java.time.Duration
import java.time.Instant

suspend fun pageLanding(call: ApplicationCall) {
    val session = call.sessions.get<LoggedSession>()
    if (session != null) {
        call.respondHtmlTemplate(LoggedInHomeTemplate(username = session.userName)) { }
    } else {
        call.respondHtmlTemplate(LandingTemplate()) { }
    }
}

suspend fun pageError(call: ApplicationCall) {
    call.respondHtmlTemplate(ErrorTemplate()) {
        errorContent {
            p { +"The following error(s) occurred:" }
            for (e in call.parameters.getAll("error").orEmpty()) {
                p { +e }
            }
        }
    }
}

suspend fun pageLogout(call: ApplicationCall) {
    call.sessions.clear<LoggedSession>()
    call.respondRedirect("/")
}

suspend fun pageDownload(call: ApplicationCall, schems: PlayerSchematics) {
    val key = call.parameters["file"] ?: showLoggedInErrorPage("Did not receive parameter file")
    schems.list().filter {
        schems.file(it).lastModified() < Instant.now().minus(Duration.ofDays(7)).toEpochMilli()
    }.forEach(schems::delete)
    val filename = schems.list().firstOrNull { it.substringBefore(".") == key }
        ?: showErrorPage("Schematic download no longer available")
    call.response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameter(
            ContentDisposition.Parameters.FileName,
            filename.substringAfter(".")
        ).toString()
    )
    val file = schems.file(filename)
    call.respondFile(file)
}

suspend fun pageLogin(call: ApplicationCall, networkDatabase: Database) {
    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
        ?: redirectTo("/")

    val id = fetchId(principal.accessToken) ?: showErrorPage("Login error: Invalid principal!")
    val user = networkDatabase.findUserByDiscordId(id) ?: showErrorPage("Login error: User is not linked!")

    call.sessions.set(LoggedSession(user.mojangId, user.ign))
    call.respondRedirect("/schems")
}

// Common functionality with ingame commands

suspend fun pageSchems(call: ApplicationCall, schems: PlayerSchematics, user: LoggedSession) {
    call.respondHtmlTemplate(SchemsListTemplate(schems.list())) {
        username {
            +user.userName
        }
    }
}

suspend fun pageSchemsRename(call: ApplicationCall, schems: PlayerSchematics) {
    val filename = call.parameters["file"] ?: showLoggedInErrorPage("Did not receive parameter file")
    val newName = call.parameters["newname"]
    if (newName == null) {
        call.respondHtmlTemplate(SchemsRenameTemplate(filename = filename)) { }
        return
    }
    schems.rename(filename, newName)
    call.respondRedirect("/schems")
}

suspend fun pageSchemsDelete(call: ApplicationCall, schems: PlayerSchematics) {
    val filename = call.parameters["file"] ?: showLoggedInErrorPage("Did not receive parameter file")
    if ("confirm" !in call.parameters) {
        call.respondHtmlTemplate(SchemsDeleteTemplate(filename = filename)) { }
        return
    }
    schems.delete(filename)
    call.respondRedirect("/schems")
}

// Web specific functionality

suspend fun pageSchemsUpload(call: ApplicationCall, schems: PlayerSchematics) {
    val parts = call
        .receiveMultipart()
        .readAllParts()
        .filterIsInstance<PartData.FileItem>()
    if (parts.isEmpty()) showErrorPage("Did not receive file")
    val filename = parts.first().originalFileName ?: showLoggedInErrorPage("File does not have a name")
    val file = schems.file(filename, mustExist = false)
    file.outputStream().buffered().use { destination ->
        parts.forEach { part ->
            part.streamProvider().use { it.copyTo(destination) }
        }
    }
    call.respondRedirect("/schems")
}

suspend fun pageSchemsDownload(call: ApplicationCall, schems: PlayerSchematics) {
    val filename = call.parameters["file"] ?: showLoggedInErrorPage("Did not receive parameter file")
    val file = schems.file(filename)
    call.response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, filename).toString()
    )
    call.respondFile(file)
}
