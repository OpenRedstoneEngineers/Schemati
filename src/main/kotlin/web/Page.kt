package schemati.web

import io.ktor.application.ApplicationCall
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authentication
import io.ktor.html.respondHtml
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.*
import io.ktor.sessions.sessions
import kotlinx.html.*
import schemati.Schematics
import schemati.connector.Database
import java.io.File

suspend fun pageLanding(call: ApplicationCall) {
    val session = call.sessions.get("sessionId")
    if (session is LoggedSession) {
        call.respondHtmlTemplate(LoggedInHomeTemplate(username = session.userName)) { }
    } else {
        call.respondHtmlTemplate(LandingTemplate()) { }
    }
}

suspend fun pageError(call: ApplicationCall) {
    call.respondHtmlTemplate(ErrorTemplate()) {
        content {
            p { +"The following error(s) occured:" }
            for (e in call.parameters.getAll("error").orEmpty()) {
                p { +e }
            }
        }
    }
}

suspend fun pageLogout(call: ApplicationCall) {
    call.sessions.clear("sessionId")
    call.respondRedirect("/")
}

suspend fun pageLogin(call: ApplicationCall, database: Database) {
    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()

    if (principal == null) {
        call.respondHtmlTemplate(LandingTemplate()) { }
        call.respondRedirect("/")
        call.respondHtml {  }
        return
    }

    val id = fetchId(principal.accessToken)

    if (id == null) {
        call.respondHtmlTemplate(ErrorTemplate()) {
            content {
                p { +"Login error: Invalid principal!" }
            }
        }
        return
    }

    val user = database.getUuidFromDiscordId(id)

    if (user == null) {
        call.respondHtmlTemplate(ErrorTemplate()) {
            content {
                p { +"Login error: User is not linked!" }
            }
        }
        return
    }

    call.sessions.set("sessionId", LoggedSession(user.mojangId, user.ign))
    call.respondRedirect("/schems")
}

// Common functionality with ingame commands

suspend fun pageSchems(call: ApplicationCall, schems: Schematics) {
    val user = getSessionUser(call.sessions) ?: run {
        call.respondText("riop")
        return
    }
    val files = schems.list(user.userId)
    call.respondHtmlTemplate(SchemsListTemplate(files)) {
        username {
            +user.userName
        }
    }
}

suspend fun pageSchemsRename(call: ApplicationCall, schems: Schematics) {
    val user = getSessionUser(call.sessions) ?: run {
        call.respondText("riop")
        return
    }
    val filename = call.parameters["file"] ?: run {
        call.respondText("riop")
        return
    }
    val newName = call.parameters["newname"]
    if (newName != null) {
        when (schems.rename(user.userId, filename, newName)) {
            true -> call.respondRedirect("/schems")
            false -> call.respondText("riop")
        }
    } else {
        call.respondHtmlTemplate(SchemsRenameTemplate(filename = filename)) { }
    }
}

suspend fun pageSchemsDelete(call: ApplicationCall, schems: Schematics) {
    val user = getSessionUser(call.sessions) ?: run {
        call.respondText("riop")
        return
    }
    val filename = call.parameters["file"] ?: run {
        call.respondText("riop")
        return
    }
    if ("confirm" in call.parameters) {
        when (schems.delete(user.userId, filename)) {
            true -> call.respondRedirect("/schems")
            false -> call.respondText("riop")
        }
    } else {
        call.respondHtmlTemplate(SchemsDeleteTemplate(filename = filename)) { }
    }
}

// Web specific functionality

suspend fun pageSchemsUpload(call: ApplicationCall, schems: Schematics) {
    val user = getSessionUser(call.sessions) ?: run {
        call.respondText("riop")
        return
    }
    // TODO: do something
    try {
        call.receiveMultipart().forEachPart { part ->
            if (part is PartData.FileItem) {
                val name = part.originalFileName!!
                val file = schems.playerFile(user.userId, name) ?: run {
                    call.respondText("riop")
                    // TODO: get outta here
                    throw Exception("riop")
                }
                part.streamProvider().use { inputStream ->
                    file.outputStream().buffered().use {
                        inputStream.copyTo(it)
                    }
                }
            }
        }
    } catch (e: Exception) {
        if (e.message == "riop") {
            return
        }
        throw e
    }
    call.respondRedirect("/schems")
}

suspend fun pageSchemsDownload(call: ApplicationCall, schems: Schematics) {
    val filename = call.parameters["file"] ?: run {
        call.respondText("riop")
        return
    }
    val user = getSessionUser(call.sessions) ?: run {
        call.respondText("riop")
        return
    }
    call.response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, filename).toString()
    )
    val file = schems.playerFile(user.userId, filename) ?: run {
        call.respondText("riop")
        return
    }
    call.respondFile(file)
}
