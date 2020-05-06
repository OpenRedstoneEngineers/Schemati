package schemati.web

import io.ktor.html.*
import kotlinx.css.*
import kotlinx.css.Float
import kotlinx.css.properties.BoxShadow
import kotlinx.css.properties.BoxShadows
import kotlinx.css.properties.TextDecoration
import kotlinx.html.*

fun getStyle(): String {
    return CSSBuilder().apply {
        body {
            marginTop = 50.px
            backgroundColor = Color("#ffcdd2")
            fontFamily = "'Montserrat', sans-serif"
        }
        i {
            color = Color("#212121")
        }
        hr {
            width = LinearDimension("80%")
            border = "0px"
            borderTop = "1px solid #e0e0e0"
        }
        h2 {
            textAlign = TextAlign.center
        }
        p {
            width = LinearDimension("100%")
            textAlign = TextAlign.center
        }
        a {
            textDecoration = TextDecoration.none
        }
        ".center" {
            margin = "auto"
            width = LinearDimension.fitContent
        }
        ".fas" {
            color = Color("#ff5555")
        }
        "#discordLoginIcon" {
            width = 100.px
            verticalAlign = VerticalAlign.bottom
        }
        "#fileListing" {
            listStyleType = ListStyleType.none
            margin = "0px"
            padding = "0px"
        }
        ".fileEntry" {
            paddingTop = 5.px
            paddingRight = 10.px
            paddingBottom = 5.px
            paddingLeft = 10.px
            minHeight = 40.px
        }
        ".generalButton" {
            color = Color.initial
            backgroundColor = Color("#e57373")
            border = "none"
            borderRadius = 3.px
            padding = "10px"
            verticalAlign = VerticalAlign.middle
            fontWeight = FontWeight.bold
            fontFamily = "'Montserrat', sans-serif"
            val currentShadows = BoxShadows()
            currentShadows.plusAssign(BoxShadow(
                inset = false,
                offsetX = 0.px,
                offsetY = 0.px,
                color = Color("#888888"),
                spreadRadius = 0.px,
                blurRadius = 10.px
            ))
            boxShadow = currentShadows
        }
        ".fileActions" {
            float = Float.right
        }
        "#content" {
            minHeight = 100.px
        }
        "#loginWithDiscord" {
            color = Color.initial
            backgroundColor = Color("#7289DA")
            borderRadius = 3.px
            padding = "14px"
            verticalAlign = VerticalAlign.middle
            fontWeight = FontWeight.bold
            val currentShadows = BoxShadows()
            currentShadows.plusAssign(BoxShadow(
                inset = false,
                offsetX = 0.px,
                offsetY = 0.px,
                color = Color("#888888"),
                spreadRadius = 0.px,
                blurRadius = 10.px
            ))
            boxShadow = currentShadows
        }
        "#main" {
            backgroundColor = Color.white
            width = 400.px
            padding = "20px"
            borderRadius = 3.px
            val currentShadows = BoxShadows()
            currentShadows.plusAssign(BoxShadow(
                inset = false,
                offsetX = 0.px,
                offsetY = 0.px,
                color = Color("#888888"),
                spreadRadius = 0.px,
                blurRadius = 10.px
            ))
            boxShadow = currentShadows
        }
    }.toString()
}
class MainTemplate : Template<HTML> {
    val content = Placeholder<FlowContent>()
    val footer = TemplatePlaceholder<FooterTemplate>()
    override fun HTML.apply() {
        head {
            title {
                +"Schemati"
            }
            link(href = "https://fonts.googleapis.com/css?family=Montserrat&display=swap", rel = "stylesheet")
            link(href = "https://use.fontawesome.com/releases/v5.1.1/css/all.css", rel = "stylesheet")
            style {
                +getStyle()
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
                    insert(content)
                }
                hr {}
                insert(FooterTemplate(), footer)
            }
        }
    }
}

class ListingTemplate : Template<HTML> {
    val main: MainTemplate = MainTemplate()
    val username = Placeholder<FlowContent>()
    val file = PlaceholderList<UL, FlowContent>()
    override fun HTML.apply() {
        insert(main) {
            content {
                p {
                    span {
                        insert(username)
                    }
                    +"'s schematics:"
                }
                if (!file.isEmpty()) {
                    ul {
                        id = "fileListing"
                        each(file) {
                            li {
                                classes = setOf("fileEntry")
                                insert(it)
                            }
                        }
                    }
                }
                div {
                    // TODO: Style this
                    form(action = "/schems/upload", method = FormMethod.post, encType = FormEncType.multipartFormData) {
                        input(type = InputType.file, name = "toUpload") {
                            id = "fileUpload"
                        }
                        input(type = InputType.submit) { }
                    }
                }
            }
        }
    }
}

class LandingTemplate : Template<HTML> {
    val main: MainTemplate = MainTemplate()
    override fun HTML.apply() {
        insert(main) {
            content {
                p { +"Welcome : ) !" }
                div {
                    classes = setOf("center")
                    style = "padding-top:10px;"
                    a ("/login") {
                        id = "loginWithDiscord"
                        +"Login with "
                        img(src = "https://discord.com/assets/34b52b6af57f96d86dd0b48c9e7841f7.png") {
                            id = "discordLoginIcon"
                        }
                    }
                }
            }
        }
    }
}



class FooterTemplate : Template<FlowContent> {
    // TODO: Style this
    override fun FlowContent.apply() {
        p {
            a ("https://openredstone.org") { +"openredstone.org"}
        }
    }
}