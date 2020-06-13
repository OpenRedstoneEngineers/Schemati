package schemati

import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import java.io.File
import java.io.IOException
import java.util.*

// TODO: catch exception somewhere during instantiation
class Schematics(private val schematicsDir: File) {
    init {
        schematicsDir.ensureDirectoryExists()
    }

    fun forPlayer(playerId: UUID) = PlayerSchematics(schematicsDir, playerId)
}

// TODO: maybe catch exception from ensureDirectoryExists?
class PlayerSchematics(schematicsDir: File, uuid: UUID) {
    private val playerDir = File(schematicsDir, uuid.toString())
        .also(File::ensureDirectoryExists)

    fun file(filename: String): File {
        if (!filename.isValidName())
            throw SchematicsException("Filename is invalid")
        return File(playerDir, filename)
    }

    fun list(): List<String> = playerDir
        .listFiles(File::isValidSchematic)
        ?.map { it.name } ?: throw SchematicsException("Could not list files")

    fun rename(filename: String, newName: String) {
        val file = file(filename)
        val new = file(newName)
        if (file.extension != new.extension)
            throw SchematicsException("You cannot change the file extension")
        if (!file.renameTo(new))
            throw SchematicsException("Could not rename")
    }

    fun delete(filename: String) {
        if (!file(filename).delete())
            throw SchematicsException("Could not delete")
    }
}

private fun File.isValidSchematic() =
    name.isValidName() && ClipboardFormats.findByFile(this) != null

private fun String.isValidName() =
    this.matches(Regex("""[a-zA-Z0-9_.]+\.schem(atic)?"""))

private fun File.ensureDirectoryExists() {
    if (exists()) {
        if (!isDirectory)
            throw IOException("$absolutePath exists but is not a directory")
    } else if (!mkdir()) {
        throw IOException("Could not create directory $absolutePath")
    }
}

class SchematicsException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

