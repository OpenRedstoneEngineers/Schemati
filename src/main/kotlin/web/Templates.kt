package schemati.web

import io.ktor.html.*
import kotlinx.html.*

class MainTemplate : Template<HTML> {
    val content = Placeholder<FlowContent>()
    val footer = TemplatePlaceholder<FooterTemplate>()
    override fun HTML.apply() {
        head {
            title {
                +"Schemati"
            }
        }
        body {
            h1 {
                +"Schemati"
            }
            insert(content)
            insert(FooterTemplate(), footer)
        }
    }
}

class LandingTemplate(val main: MainTemplate = MainTemplate()) : Template<HTML> {
    override fun HTML.apply() {
        insert(main) {
            content {
                p { +"Welcome : ) !" }
            }
        }
    }
}

class FooterTemplate : Template<FlowContent> {
    override fun FlowContent.apply() {
        p {
            a ("https://openredstone.org") { +"openredstone.org"}
            +" !"
        }
    }
}