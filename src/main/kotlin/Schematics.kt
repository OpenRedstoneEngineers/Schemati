package schemati

import java.io.File
import java.util.*

class Schematics(private val schematicsDir: File) {
    init {
        if (!schematicsDir.exists())
            schematicsDir.mkdir()
    }

    private fun playerDir(uuid: UUID) = File(schematicsDir, uuid.toString()).apply {
        if (!exists()) mkdir()
    }

    fun playerFile(uuid: UUID, filename: String): File? {
        val valid = filename.all { it.isLetterOrDigit() || it == '_' }
        if (!valid) return null
        return File(playerDir(uuid), filename)
    }

    fun list(uuid: UUID): List<String> {
        // list() returns null if the file is invalid.
        // In this case it should always be valid because playerDir creates it should it not exist.
        return playerDir(uuid).list()!!.sorted()
    }

    fun rename(uuid: UUID, filename: String, newName: String): Boolean {
        val file = playerFile(uuid, filename) ?: return false
        val new = playerFile(uuid, newName) ?: return false
        return file.renameTo(new)
    }

    fun delete(uuid: UUID, filename: String): Boolean {
        return playerFile(uuid, filename)?.delete() ?: false
    }
}
