package schemati.connector

import com.vladsch.kotlin.jdbc.Row
import com.vladsch.kotlin.jdbc.Session
import com.vladsch.kotlin.jdbc.session
import com.vladsch.kotlin.jdbc.sqlQuery

class Database(val port: Int = 3306, val host: String = "localhost", val database: String, val username: String, val password: String) {

    data class User(val mojangId: String, val discordId: String, val ign: String)

    private val toUser: (Row) -> User = { row ->
        User(
            row.string("m_uuid"),
            row.string("discord_id"),
            row.string("ign")
        )
    }

    private val connection: Session = session("jdbc:mysql://${host}:${port}/${database}", username, password)

    fun getUuidFromDiscordId(discordId: String) : User? {
        val selectUserFromDiscordId = sqlQuery("SELECT * FROM nu_users WHERE discord_id LIKE ?", discordId)
        return connection.first(selectUserFromDiscordId, toUser)
    }

    fun unload() {
        connection.close()
    }

}