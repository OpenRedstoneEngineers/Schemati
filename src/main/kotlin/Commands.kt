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
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.entity.Player
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

@CommandAlias("/schematics")
@Description("Manage your schematics")
@CommandPermission("schemati.schematics")
class Commands(private val worldEdit: WorldEdit, private val schems: Schematics) : BaseCommand() {
    private val ioErrorMessage =
        "An unexpected error happened against our expectations. Please consult your dictionary."

    @Default
    @Subcommand("list")
    fun list(player: Player) {
        val files = schems.list(player.uniqueId)
        player.sendMessage("Your schematics:")
        player.sendMessage(files.toTypedArray())
    }

    @Subcommand("rename")
    @CommandAlias("/rename")
    @CommandCompletion("@schematics")
    fun rename(player: Player, filename: String, newName: String) {
        val response = when (schems.rename(player.uniqueId, filename, newName)) {
            true -> "Renamed schematic $filename to $newName."
            false -> "Something went wrong!"
        }
        player.sendMessage(response)
    }

    @Subcommand("delete")
    @CommandAlias("/delete")
    @CommandCompletion("@schematics")
    fun delete(player: Player, filename: String) {
        val response = when (schems.delete(player.uniqueId, filename)) {
            true -> "Deleted schematic $filename."
            false -> "Something went wrong!"
        }
        player.sendMessage(response)
    }

    @Subcommand("save")
    @CommandAlias("/save")
    @CommandCompletion("@schematics")
    fun save(player: Player, filename: String) {
        val file = schems.playerFile(player.uniqueId, filename) ?: run {
            player.sendMessage("Invalid file!")
            return
        }
        val clipboard = try {
            player.weSession.clipboard.clipboard
        } catch (e: EmptyClipboardException) {
            player.sendMessage("Your clipboard is empty! Please copy before saving.")
            return
        }
        try {
            BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getWriter(FileOutputStream(file)).use { it.write(clipboard) }
        } catch (e: IOException) {
            player.sendMessage(ioErrorMessage)
            return
        }
        player.sendMessage("Schematic $filename has been saved.")
    }

    @Subcommand("load")
    @CommandAlias("/load")
    @CommandCompletion("@schematics")
    fun load(player: Player, filename: String) {
        val file = schems.playerFile(player.uniqueId, filename) ?: run {
            player.sendMessage("Invalid filename.")
            return
        }
        val format = ClipboardFormats.findByFile(file) ?: run {
            player.sendMessage("Invalid or unrecognized schematic format.")
            return
        }
        val clipboard = try {
            format.getReader(FileInputStream(file)).use { reader ->
                reader.read()
            }
        } catch (e: IOException) {
            player.sendMessage(ioErrorMessage)
            return
        }
        player.weSession.clipboard = ClipboardHolder(clipboard)
    }

    private val Player.weSession: LocalSession
        get() = worldEdit.sessionManager.get(BukkitAdapter.adapt(this))
}

//        return


class SchematicCompletionHandler(private val schems: Schematics) :
    CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext>
{
    override fun getCompletions(context: BukkitCommandCompletionContext): MutableCollection<String> =
        schems.list(context.player.uniqueId).toMutableList()
}

