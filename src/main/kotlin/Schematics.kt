package schemati

import java.io.File
import java.util.*

class Schematics(private val schematicsDir: File) {
    fun forPlayer(playerId: UUID): PlayerSchematics = PlayerSchematics(schematicsDir, playerId)
}

class PlayerSchematics(schematicsDir: File, uuid: UUID) {
    init {
        if (!schematicsDir.exists())
            schematicsDir.mkdir()
    }

    private val playerDir = File(schematicsDir, uuid.toString()).apply {
        if (!exists()) mkdir()
    }

    fun file(filename: String): File? {
        val valid = filename.all { it.isLetterOrDigit() || it == '_' }
        if (!valid) return null
        return File(playerDir, filename)
    }

    fun list(): List<String> {
        // list() returns null if the file is invalid.
        // In this case it should always be valid because playerDir creates it should it not exist.
        return playerDir.list()!!.sorted()
    }

    fun rename(filename: String, newName: String): Boolean {
        val file = file(filename) ?: return false
        val new = file(newName) ?: return false
        return file.renameTo(new)
    }

    fun delete(filename: String): Boolean {
        return file(filename)?.delete() ?: false
    }
}
