package org.openredstone.schemati.plugin

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
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException
import com.sk89q.worldedit.util.formatting.component.PaginationBox
import com.sk89q.worldedit.util.formatting.text.Component
import com.sk89q.worldedit.util.formatting.text.TextComponent
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent
import com.sk89q.worldedit.util.formatting.text.format.TextColor
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.openredstone.schemati.core.PlayerSchematics
import org.openredstone.schemati.core.Schematics
import org.openredstone.schemati.core.SchematicsException
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

@CommandAlias("/schematics")
@Description("Manage your schematics")
@CommandPermission("schemati.schematics")
class Commands(private val worldEdit: WorldEdit, private val url: String, private val serverSchems: PlayerSchematics) :
    BaseCommand() {
    private val ioErrorMessage =
        "An unexpected error happened against our expectations. Please consult your dictionary."

    @Default
    @Syntax("[number]")
    @Subcommand("list")
    @CommandPermission("schemati.schematics.list")
    fun list(player: Player, schems: PlayerSchematics, @Default("1") page: Int) {
        val files = schems.list()
        val paginationBox = SchematicsPaginationBox(files, "Schematics", "//schematics list %page%")
        val component = try {
            paginationBox.create(page)
        } catch (e: InvalidComponentException) {
            TextComponent.of("Invalid page number.").color(TextColor.RED)
        }
        player.sendFormatted(component, prefix = false)
    }

    @Subcommand("rename")
    @CommandAlias("/rename")
    @CommandPermission("schemati.schematics.rename")
    @CommandCompletion("@schematics")
    fun rename(player: Player, schems: PlayerSchematics, filename: String, newName: String) {
        schems.rename(filename, newName)
        player.sendBasic("Renamed schematic $filename to $newName.")
    }

    @Subcommand("delete")
    @CommandAlias("/delete")
    @CommandCompletion("@schematics")
    @CommandPermission("schemati.schematics.delete")
    fun delete(player: Player, schems: PlayerSchematics, filename: String) {
        schems.delete(filename)
        player.sendBasic("Deleted schematic $filename")
    }

    @Subcommand("download")
    @CommandAlias("/download")
    @CommandCompletion("@schematics")
    @CommandPermission("schemati.schematics.download")
    fun download(player: Player, schems: PlayerSchematics, filename: String) {
        val file = schems.file(filename)
        val key = generateKey()
        val copiedFile = serverSchems.file("$key.${filename}", mustExist = false)
        file.copyTo(copiedFile)
        player.sendFormatted(
            TextComponent.of("Click to download schematic $filename.")
                .clickEvent(ClickEvent.openUrl("${url}/download?file=$key"))
                .hoverEvent(HoverEvent.showText(TextComponent.of("Download")))
            , prefix = true
        )
    }

    @Subcommand("save")
    @CommandAlias("/save")
    @CommandCompletion("@schematics")
    @CommandPermission("schemati.schematics.save")
    fun save(player: Player, schems: PlayerSchematics, filename: String) {
        val file = schems.file(
            if ("." !in filename) {
                "${filename}.schem"
            } else {
                filename
            }
            , mustExist = false
        )
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
        player.sendBasic("Schematic $filename has been saved.")
    }

    @Subcommand("load")
    @CommandAlias("/load")
    @CommandCompletion("@schematics")
    @CommandPermission("schemati.schematics.load")
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
        player.sendBasic("Loaded schematic $filename to clipboard.")
    }

    private fun generateKey(): String {
        val characters = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..16)
            .map { Random.nextInt(0, characters.size) }
            .map(characters::get)
            .joinToString("")
    }

    private fun Player.sendBasic(message: String) {
        this.sendFormatted(TextComponent.of(message))
    }

    private fun Player.sendFormatted(component: Component, prefix: Boolean = true) {
        if (prefix) {
            BukkitAdapter.adapt(player).print(TextComponent.of("[Schemati] ").color(TextColor.AQUA).append(component))
        } else {
            BukkitAdapter.adapt(player).print(component)
        }
    }

    private val Player.weSession: LocalSession
        get() = worldEdit.sessionManager.get(BukkitAdapter.adapt(this))
}

class SchematicCompletionHandler(private val schems: Schematics) :
    CommandCompletions.AsyncCommandCompletionHandler<BukkitCommandCompletionContext> {
    override fun getCompletions(context: BukkitCommandCompletionContext): Collection<String> =
        schems.forPlayer(context.player.uniqueId).list().toList()
}

class SchematicsPaginationBox(private val schematics: List<String>, title: String, command: String) :
    PaginationBox("${ChatColor.LIGHT_PURPLE}$title", command) {

    init {
        setComponentsPerPage(7)
    }

    override fun getComponent(number: Int): Component {
        if (number > schematics.size) throw IllegalArgumentException("Invalid location index.")
        return TextComponent.of("").color(TextColor.GRAY)
            .append(
                TextComponent.of(" ✏ ")
                    .clickEvent(ClickEvent.suggestCommand("//schematics rename ${schematics[number]}"))
                    .hoverEvent(HoverEvent.showText(TextComponent.of("Rename")))
                    .color(TextColor.RED)
            )
            .append(TextComponent.of("|"))
            .append(
                TextComponent.of(" ⬇ ")
                    .clickEvent(ClickEvent.suggestCommand("//schematics download ${schematics[number]}"))
                    .hoverEvent(HoverEvent.showText(TextComponent.of("Download")))
                    .color(TextColor.RED)
            )
            .append(TextComponent.of("|"))
            .append(
                TextComponent.of(" ✖ ")
                    .clickEvent(ClickEvent.suggestCommand("//schematics delete ${schematics[number]}"))
                    .hoverEvent(HoverEvent.showText(TextComponent.of("Delete")))
                    .color(TextColor.RED)
            )
            .append(TextComponent.of("|"))
            .append(TextComponent.of(" ${schematics[number]}").color(TextColor.YELLOW))
    }

    override fun getComponentsSize(): Int = schematics.size

    override fun create(page: Int): Component {
        super.getContents().append(TextComponent.of("Total Schematics: ${schematics.size}").color(TextColor.GRAY))
            .append(TextComponent.newline())
        return super.create(page)
    }
}
