package schemati.web

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.apache.Apache
import io.ktor.features.*
import io.ktor.http.HttpMethod
import io.ktor.routing.*
import io.ktor.sessions.*
import schemati.Schemati

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
            pageLanding(call)
        }
        get("/logout") {
            pageLogout(call)
        }
        route("/schems") {
            handle {
                pageSchems(call)
            }
            post("/upload") {
                pageSchemsUpload(call)
            }
            get("/download") {
                pageSchemsDownload(call)
            }
            get("/rename") {
                pageSchemsRename(call)
            }
            get("/delete") {
                pageSchemsDelete(call)
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
                    pageLogin(call)
                }
            }
        }
    }
}