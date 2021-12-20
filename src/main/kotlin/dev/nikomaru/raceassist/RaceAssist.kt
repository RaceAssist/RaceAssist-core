/*
 * Copyright © 2021 Nikomaru
 * This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.nikomaru.raceassist

import co.aikar.commands.PaperCommandManager
import dev.nikomaru.raceassist.database.Database
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.race.commands.AudienceCommand
import dev.nikomaru.raceassist.race.commands.PlaceCommands
import dev.nikomaru.raceassist.race.commands.PlayerCommand
import dev.nikomaru.raceassist.race.commands.RaceCommand
import dev.nikomaru.raceassist.race.event.SetCentralPointEvent
import dev.nikomaru.raceassist.race.event.SetInsideCircuitEvent
import dev.nikomaru.raceassist.race.event.SetOutsideCircuitEvent
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.sql.SQLException

class RaceAssist : JavaPlugin() {
    private var sql: Database? = null

    override fun onEnable() {
        // Plugin startup logic
        plugin = this
        val config = Config()
        config.load()
        sqlConnection()
        registerCommands()
        registerEvents()
        Database.initializeDatabase()
    }

    override fun onDisable() {
        // Plugin shutdown logic
        sql?.disconnect()
    }

    private fun registerCommands() {
        val manager = PaperCommandManager(this)
        manager.registerCommand(PlaceCommands())
        manager.registerCommand(RaceCommand())
        manager.registerCommand(AudienceCommand())
        manager.registerCommand(PlayerCommand())
    }

    private fun registerEvents() {
        Bukkit
            .getPluginManager()
            .registerEvents(SetInsideCircuitEvent(), this)
        Bukkit
            .getPluginManager()
            .registerEvents(SetOutsideCircuitEvent(), this)
        Bukkit
            .getPluginManager()
            .registerEvents(SetCentralPointEvent(), this)
    }

    private fun sqlConnection() {
        sql = Database()
        try {
            sql!!.connect()
        } catch (e: ClassNotFoundException) {
            plugin!!.logger.warning("Database not connected")
        } catch (e: SQLException) {
            plugin!!.logger.warning("Database not connected")
        }
        if (sql!!.isConnected()) {
            plugin!!.logger.info("Database is connected!")
        }
    }

    companion object {
        var plugin: RaceAssist? = null
    }
}


