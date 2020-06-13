package schemati

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandCompletionContext
import co.aikar.commands.CommandCompletions
import co.aikar.commands.annotation.*
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.entity.Player
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

@CommandAlias("/schematics")
@Description("Manage your schematics")
@CommandPermission("schemati.schematics")
class Commands(private val worldEdit: WorldEdit) : BaseCommand() {
    private val ioErrorMessage =
        "An unexpected error happened against our expectations. Please consult your dictionary."

    @Default
    @Subcommand("list")
    fun list(player: Player, schems: PlayerSchematics) {
        val files = schems.list()
        player.sendMessage("Your schematics:")
        player.sendMessage(files.toTypedArray())
    }

    @Subcommand("rename")
    @CommandAlias("/rename")
    @CommandCompletion("@schematics")
    fun rename(player: Player, schems: PlayerSchematics, filename: String, newName: String) {
        schems.rename(filename, newName)
        player.sendMessage("Renamed schematic $filename to $newName.")
    }

    @Subcommand("delete")
    @CommandAlias("/delete")
    @CommandCompletion("@schematics")
    fun delete(player: Player, schems: PlayerSchematics, filename: String) {
        schems.delete(filename)
        player.sendMessage("Deleted schematic $filename")
    }

    @Subcommand("save")
    @CommandAlias("/save")
    @CommandCompletion("@schematics")
    fun save(player: Player, schems: PlayerSchematics, filename: String) {
        val file = schems.file(filename)
        val clipboard = try {
            player.weSession.clipboard.clipboard
        } catch (e: EmptyClipboardException) {
            throw SchematicsException("Your clipboard is empty! Please copy before saving.", e)
        }
        try {
            BuiltInClipboardFormat
                .SPONGE_SCHEMATIC
                .getWriter(FileOutputStream(file))
                .use { it.write(clipboard) }
        } catch (e: IOException) {
            throw SchematicsException(ioErrorMessage, e)
        }
        player.sendMessage("Schematic $filename has been saved.")
    }

    @Subcommand("load")
    @CommandAlias("/load")
    @CommandCompletion("@schematics")
    fun load(player: Player, schems: PlayerSchematics, filename: String) {
        val file = schems.file(filename)
        val format = ClipboardFormats.findByFile(file)
            ?: throw SchematicsException("Invalid or unrecognized schematic format.")
        val clipboard = try {
            format
                .getReader(FileInputStream(file))
                .use(ClipboardReader::read)
        } catch (e: IOException) {
            throw SchematicsException(ioErrorMessage, e)
        }
        player.weSession.clipboard = ClipboardHolder(clipboard)
        player.sendMessage("Loaded schematic $filename to clipboard.")
    }

    private val Player.weSession: LocalSession
        get() = worldEdit.sessionManager.get(BukkitAdapter.adapt(this))
}

class SchematicCompletionHandler(private val schems: Schematics) :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext>
{
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> =
        schems.forPlayer(context.player.uniqueId).list().toList()
}
