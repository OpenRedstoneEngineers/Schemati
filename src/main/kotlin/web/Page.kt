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
import io.ktor.response.header
import io.ktor.response.respondFile
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.sessions.sessions
import kotlinx.html.*
import schemati.Schemati
import java.io.File

suspend fun pageLanding(call: ApplicationCall) {
    val session = call.sessions.get("sessionId")
    if (isValidSession(call.sessions)) {
        call.respondHtmlTemplate(LoggedInHomeTemplate(username = (session as LoggedSession).userName)) { }
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

suspend fun pageLogin(call: ApplicationCall) {
    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()

    if (principal == null) {
        call.respondHtmlTemplate(LandingTemplate()) { }
        call.respondRedirect("/")
        call.respondHtml {  }
    }

    val id = fetchId(principal!!.accessToken)

    if (id == null) {
        call.respondHtmlTemplate(ErrorTemplate()) {
            content {
                p { +"Login error: Invalid principal!" }
            }
        }
    }

    val user = Schemati.databaseManager?.getUuidFromDiscordId(id!!)

    if (user == null) {
        call.respondHtmlTemplate(ErrorTemplate()) {
            content {
                p { +"Login error: User is not linked!" }
            }
        }
    }

    call.sessions.set("sessionId", LoggedSession(user!!.mojangId, user.ign))
    call.respondRedirect("/schems")
}

suspend fun pageSchems(call: ApplicationCall) {
    // TODO: resolve multiple response errors
    val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
    val user = getSessionUser(call.sessions) ?: call.respondText("riop")
    call.respondHtmlTemplate(SchemsListTemplate(files = File(folderPath.toString()).listFiles().toSet())) {
        username {
            +(user as LoggedSession).userName
        }
    }
}

suspend fun pageSchemsUpload(call: ApplicationCall) {
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

suspend fun pageSchemsDownload(call: ApplicationCall) {
    val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
    val filename = call.parameters["file"]
    call.response.header(
        HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
            ContentDisposition.Parameters.FileName, filename!!).toString())
    call.respondFile(File("${folderPath}${filename}"))
}

suspend fun pageSchemsRename(call: ApplicationCall) {
    val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
    val filename = call.parameters["file"]
    if (call.parameters.contains("newname")) {
        val newname = call.parameters["newname"]
        // TODO: Implement path+file validation
        File("${folderPath}${filename}").renameTo(File("${folderPath}${newname}"))
        call.respondRedirect("/schems")
    } else {
        call.respondHtmlTemplate(SchemsRenameTemplate(filename = filename!!)) { }
    }
}

suspend fun pageSchemsDelete(call: ApplicationCall) {
    val folderPath = getFolderPath(call.sessions) ?: call.respondText("riop")
    val filename = call.parameters["file"]
    if (call.parameters.contains("confirm")) {
        // TODO: Implement path+file validation
        File("${folderPath}${filename}").delete()
        call.respondRedirect("/schems")
    } else {
        call.respondHtmlTemplate(SchemsDeleteTemplate(filename = filename!!)) { }
    }
}