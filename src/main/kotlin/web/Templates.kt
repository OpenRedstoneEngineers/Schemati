package schemati.web

import io.ktor.html.*
import kotlinx.html.*
import java.io.File

class LoggedInBaseTemplate : Template<HTML> {
    val content = Placeholder<FlowContent>()
    val base = BaseTemplate()
    override fun HTML.apply() {
        insert(base) {
            footer {
                loggedIn = true
            }
            mainContent {
                insert(content)
            }
        }
    }
}

class ErrorTemplate : Template<HTML> {
    val content = Placeholder<FlowContent>()
    val base = BaseTemplate()
    override fun HTML.apply() {
        insert(base) {
            mainContent {
                insert(content)
            }
        }
    }
}

class BaseTemplate : Template<HTML> {
    val mainContent = Placeholder<FlowContent>()
    val footer = TemplatePlaceholder<FooterTemplate>()
    override fun HTML.apply() {
        head {
            title {
                +"Schemati"
            }
            link(href = "https://openredstone.org/wp-content/uploads/2018/07/favicon.png", rel = "icon")
            link(href = "https://fonts.googleapis.com/css?family=Montserrat&display=swap", rel = "stylesheet")
            link(href = "https://use.fontawesome.com/releases/v5.1.1/css/all.css", rel = "stylesheet")
            style {
                unsafe {
                    raw(getStyle())
                }
            }
            script(type = "text/javascript") {
                unsafe {
                    raw(getScript())
                }
            }
        }
        body {
            div {
                id = "main"
                classes = setOf("center")
                h2 {
                    +"Schemati"
                }
                hr { }
                div {
                    id = "content"
                    insert(mainContent)
                }
                hr {}
                insert(FooterTemplate(), footer)
            }
        }
    }
}

class LoggedInHomeTemplate(var username : String) : Template<HTML> {
    val loggedInBaseTemplate : LoggedInBaseTemplate = LoggedInBaseTemplate()
    override fun HTML.apply() {
        insert(loggedInBaseTemplate) {
            content {
                p { +"Hello, $username" }
                p { +"View your "
                    a("/schems") {
                        span {
                            +"schematics"
                        }
                    }
                    +"?"
                }
            }
        }
    }
}

class SchemsRenameTemplate(var filename : String) : Template<HTML> {
    val loggedInBaseTemplate : LoggedInBaseTemplate = LoggedInBaseTemplate()
    override fun HTML.apply() {
        insert(loggedInBaseTemplate) {
            content {
                p {
                    +"Rename "
                    span {
                        +filename
                    }
                    +"?"
                }
                form(action = "/schems/rename/") {
                    input(type = InputType.text, name = "file") {
                        hidden = true
                        value = filename
                    }
                    input(type = InputType.text, name = "newname") {
                        classes = setOf("textInput")
                        style = "width:80%;margin-left:10%;margin-right:10%;"
                        value = filename
                        placeholder = "New Name"
                    }
                    div {
                        style = "padding-top:20px;min-height:50px"
                        input(type = InputType.submit) {
                            classes = setOf("generalButton leftFloat")
                            value = "Rename"
                        }
                        a(href = "/schems") {
                            button {
                                classes = setOf("generalButton rightFloat")
                                +"Cancel"
                            }
                        }
                    }
                }
            }
        }
    }
}

class SchemsDeleteTemplate(var filename : String) : Template<HTML> {
    val loggedInBaseTemplate : LoggedInBaseTemplate = LoggedInBaseTemplate()
    override fun HTML.apply() {
        insert(loggedInBaseTemplate) {
            content {
                p {
                    +"Delete "
                    span {
                        +filename
                    }
                    +"?"
                }
                form(action = "/schems/delete/") {
                    classes = setOf("leftFloat")
                    input(type = InputType.text, name = "file") {
                        hidden = true
                        value = filename
                    }
                    input(type = InputType.submit, name = "confirm") {
                        style = "min-width:100px;"
                        classes = setOf("generalButton")
                        value = "Confirm"
                    }
                }
                form(action = "/schems") {
                    classes = setOf("rightFloat")
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

class SchemsListTemplate(var files : Set<File>) : Template<HTML> {
    val loggedInBaseTemplate : LoggedInBaseTemplate = LoggedInBaseTemplate()
    val username = Placeholder<FlowContent>()
    override fun HTML.apply() {
        insert(loggedInBaseTemplate) {
            content {
                p {
                    span {
                        insert(username)
                    }
                    +"'s schematics:"
                }
                // I hate doing this instead of using the API, but its the only way to get it to work
                if (files.isNotEmpty()) {
                    ul {
                        id = "fileListing"
                        var fileCount = 0
                        files.forEach {
                            fileCount++
                            li {
                                classes = setOf("fileEntry")
                                div {
                                    classes = setOf("fileEntryName")
                                    id = "fileName${fileCount}"
                                    +it.name
                                }
                                div {
                                    classes = setOf("fileEntryActions")
                                    id = "actionOptions${fileCount}"
                                    style = "display:none;"
                                    span {
                                        style = "font-size:0.8em;"
                                        a("/schems/rename/?file=${it.name}") {
                                            style = "color:#444444;"
                                            +"Rename "
                                            i {
                                                classes = setOf("fas fa-edit")
                                            }
                                        }
                                    }
                                    +"  |  "
                                    span {
                                        style = "font-size:0.8em;"
                                        a("/schems/delete/?file=${it.name}") {
                                            style = "color:#444444;"
                                            +"Delete "
                                            i {
                                                classes = setOf("fas fa-trash")
                                            }
                                        }
                                    }
                                    +"  |  "
                                    span {
                                        style = "font-size:0.8em;"
                                        a("/schems/download/?file=${it.name}") {
                                            style = "color:#444444;"
                                            +"Download "
                                            i {
                                                classes = setOf("fas fa-download")
                                            }
                                        }
                                    }
                                }
                                div {
                                    classes = setOf("actionMenu")
                                    button {
                                        classes = setOf("actionButton")
                                        onClick = "swapDisplay(${fileCount})"
                                        i {
                                            id = "actionIcon${fileCount}"
                                            classes = setOf("fas fa-caret-left")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                div {
                    form(action = "/schems/upload", method = FormMethod.post, encType = FormEncType.multipartFormData) {
                        style = "padding-bottom:14px;padding-top:14px;"
                        input(type = InputType.file, name = "toUpload") {
                            style = "display:none;"
                            onChange = "this.form.submit()"
                            id = "fileUpload"
                        }
                        input(type = InputType.button) {
                            classes = setOf("generalButton")
                            style = "width:60%;margin-left:20%;margin-right:20%;"
                            value = "Upload Schematic"
                            onClick = "document.getElementById('fileUpload').click();"
                        }
                    }
                }
            }
        }
    }
}

class LandingTemplate : Template<HTML> {
    val base : BaseTemplate = BaseTemplate()
    override fun HTML.apply() {
        insert(base) {
            mainContent {
                p { +"Welcome : ) !" }
                div {
                    classes = setOf("center")
                    style = "padding-top:10px;"
                    a ("/login") {
                        id = "loginWithDiscord"
                        +"Login with "
                        img(src = "https://discord.com/assets/f72fbed55baa5642d5a0348bab7d7226.png") {
                            id = "discordLoginIcon"
                        }
                    }
                }
            }
        }
    }
}

class FooterTemplate(var loggedIn : Boolean = false) : Template<FlowContent> {
    override fun FlowContent.apply() {
        p {
            style = "font-size:0.8em;"
            if (loggedIn) {
                a ("/logout") { +"logout"}
                +" | "
            }
            a ("https://openredstone.org") { +"openredstone"}
            +" | "
            a ("https://forum.openredstone.org") { +"forum"}
            +" | "
            a ("https://openredstone.org/discord") { +"discord"}
        }
    }
}