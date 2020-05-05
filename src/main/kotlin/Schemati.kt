package schemati

import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import schemati.connector.Database
import schemati.web.main
import java.io.File

class Schemati : JavaPlugin() {

    override fun onEnable() {
        loadConfig()
        schematiConfig = config

        val databaseSection = config.getConfigurationSection("database")!!
        databaseManager = Database(
            database = databaseSection.getString("database")!!,
            username = databaseSection.getString("username")!!,
            password = databaseSection.getString("password")!!
        )

        if (config.contains("web.port")) {
            startWeb(config.getConfigurationSection("web")!!.getInt("port"))
        }

    }

    override fun onDisable() {
        databaseManager?.unload()
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

    private fun startWeb(port: Int) {
        embeddedServer(Netty, port = port, module = Application::main).start()
    }

    companion object Configuration {
        var schematiConfig: FileConfiguration? = null
        var databaseManager: Database? = null
    }
}