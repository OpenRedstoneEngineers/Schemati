package schemati.connector

import com.vladsch.kotlin.jdbc.Row
import com.vladsch.kotlin.jdbc.Session
import com.vladsch.kotlin.jdbc.session
import com.vladsch.kotlin.jdbc.sqlQuery
import java.util.*

data class User(val mojangId: UUID, val discordId: String, val ign: String)

interface Database {
    fun getUuidFromDiscordId(discordId: String): User?
}

class DatabaseImpl(port: Int = 3306, host: String = "localhost", database: String, username: String, password: String): Database {
    private val toUser: (Row) -> User = { row ->
        User(
            UUID.fromString(row.string("m_uuid")),
            row.string("discord_id"),
            row.string("ign")
        )
    }

    private val connection: Session = session("jdbc:mysql://${host}:${port}/${database}", username, password)

    override fun getUuidFromDiscordId(discordId: String): User? {
        val selectUserFromDiscordId = sqlQuery("SELECT * FROM nu_users WHERE discord_id LIKE ?", discordId)
        return connection.first(selectUserFromDiscordId, toUser)
    }

    fun unload() {
        connection.close()
    }
}
