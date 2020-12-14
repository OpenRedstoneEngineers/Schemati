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
    private val port: Int = 3306,
    private val host: String = "localhost",
    val database: String,
    val username: String,
    val password: String
): Database {
    private val toUser: (Row) -> User = { row ->
        User(
            UUID.fromString(row.string("m_uuid")),
            row.string("discord_id"),
            row.string("ign")
        )
    }

    private var connection: Session = load()

    private fun load() =
        session("jdbc:mysql://${host}:${port}/${database}", username, password)

    override fun findUserByDiscordId(discordId: String): User? {
        val selectUserFromDiscordId = sqlQuery("SELECT * FROM nu_users WHERE discord_id LIKE ?", discordId)
        if (!connection.connection.isValid(5)) {
            connection = load()
        }
        return connection.first(selectUserFromDiscordId, toUser)
    }

    override fun unload() {
        connection.close()
    }
}