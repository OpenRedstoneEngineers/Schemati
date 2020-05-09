package schemati

import co.aikar.commands.PaperCommandManager
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import io.ktor.server.engine.ApplicationEngine
import org.bukkit.plugin.java.JavaPlugin
import schemati.connector.JDBCDatabase
import schemati.web.AuthConfig
import java.io.File

class Schemati : JavaPlugin() {
    private var web: ApplicationEngine? = null
    private var database: JDBCDatabase? = null

    override fun onEnable() {
        loadConfig()

        val wePlugin = server.pluginManager.getPlugin("WorldEdit") as? WorldEditPlugin ?: throw Exception("no u")
        val schems = Schematics(File(config.getString("schematicsDirectory")!!))
        PaperCommandManager(this).apply {
            commandContexts.registerIssuerOnlyContext(PlayerSchematics::class.java) { context ->
                schems.forPlayer(context.player.uniqueId)
            }
            commandCompletions.registerCompletion("schematics", SchematicCompletionHandler(schems))
            registerCommand(Commands(wePlugin.worldEdit))
        }

        database = config.getConfigurationSection("database")!!.run {
            JDBCDatabase(
                database = getString("database")!!,
                username = getString("username")!!,
                password = getString("password")!!
            )
        }

        val oauthSection = config
            .getConfigurationSection("web")!!
            .getConfigurationSection("oauth")!!

        val authConfig = oauthSection.run {
            AuthConfig(
                clientId = getString("clientId")!!,
                clientSecret = getString("clientSecret")!!,
                scopes = getStringList("scopes")
            )
        }


        if (config.contains("web.port")) {
            web = startWeb(
                config.getConfigurationSection("web")!!.getInt("port"),
                database!!,
                authConfig,
                schems
            )
        }
    }

    override fun onDisable() {
        database?.unload()
        web?.stop(1000, 1000)
    }

    private fun loadConfig() {
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }

        val file = File(dataFolder, "config.yml")

        if (!file.exists()) {
            file.createNewFile()
        }

        config.options().copyDefaults(true)
        saveConfig()
    }
}
