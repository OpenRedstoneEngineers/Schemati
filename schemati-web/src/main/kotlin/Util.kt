package org.openredstone.schemati.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

@Serializable
data class LoggedSession(
    @Serializable(with = UUIDSerializer::class) val userId: UUID
    , val userName: String)

suspend fun fetchId(accessToken: String) : String? {
    val json = HttpClient(Apache5).use { client ->
        client.get("${discordApiBase}users/@me") {
            bearerAuth(accessToken)
        }.bodyAsText()
    }
    val data = ObjectMapper().readValue(json, Map::class.java)
    return data["id"] as? String
}

fun ApplicationCall.redirectUrl(relativePath: String, absoluteBaseUri: String? = null): String {
    return if (absoluteBaseUri!=null) {
        "$absoluteBaseUri$relativePath"
    } else {
        val defaultPort = if (request.origin.scheme == "http") 80 else 443
        val hostPort = request.host() + request.port().let { port -> if (port == defaultPort) "" else ":$port" }
        val protocol = request.origin.scheme
        "$protocol://$hostPort$relativePath"
    }
}
