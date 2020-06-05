package schemati

import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
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
        if (!filename.isValidName()) return null
        return File(playerDir, filename)
    }

    fun list(): List<String> {
        // list() returns null if the file is invalid.
        // In this case it should always be valid because playerDir creates it should it not exist.
        val files = playerDir.listFiles { file ->
            file.isValidSchematic() && file.name.isValidName()
        } ?: return emptyList()
        return files.map { file -> file.name }
    }

    fun rename(filename: String, newName: String): Boolean {
        val file = file(filename) ?: return false
        val new = file(newName) ?: return false
        if (file.extension != new.extension) return false
        return file.renameTo(new)
    }

    fun delete(filename: String): Boolean {
        if (!filename.isValidName()) return false
        return file(filename)?.delete() ?: false
    }

    private fun File.isValidSchematic(): Boolean {
        ClipboardFormats.findByFile(this) ?: return false
        return true
    }

    private fun String.isValidName(): Boolean {
        return this.matches(Regex("""[a-zA-Z0-9_.]+?\.(schem(atic)?)"""))
    }
}
