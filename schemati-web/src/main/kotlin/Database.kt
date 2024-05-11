package org.openredstone.schemati.web

import com.vladsch.kotlin.jdbc.Row
import com.vladsch.kotlin.jdbc.Session
import com.vladsch.kotlin.jdbc.session
import com.vladsch.kotlin.jdbc.sqlQuery
import java.util.*

data class User(val mojangId: UUID, val discordId: String, val ign: String)

interface Database {
    fun findUserByDiscordId(discordId: String): User?
    fun unload()
}

class NetworkDatabase(
    private val port: Int,
    private val host: String,
    private val database: String,
    private val username: String,
    private val password: String
): Database {
    private val toUser: (Row) -> User = { row ->
        User(
            UUID.fromString(row.string("user_uuid")),
            row.string("user_discord_id"),
            row.string("user_ign")
        )
    }

    private var connection: Session = load()

    private fun load() =
        session("jdbc:mysql://${host}:${port}/${database}", username, password)

    override fun findUserByDiscordId(discordId: String): User? {
        val selectUserFromDiscordId = sqlQuery("SELECT * FROM linkore_user WHERE user_discord_id LIKE ?", discordId)
        if (!connection.connection.isValid(5)) {
            connection = load()
        }
        return connection.first(selectUserFromDiscordId, toUser)
    }

    override fun unload() {
        connection.close()
    }
}
